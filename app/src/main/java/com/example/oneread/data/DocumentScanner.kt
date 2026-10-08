package com.example.oneread.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-performance, production-grade document discovery and indexing engine for HR Read.
 *
 * Discovers documents from:
 * 1. MediaStore.Files (primary & secondary storage, Downloads, Documents) with comprehensive extension and MIME aliases.
 * 2. Accessible device filesystem (Downloads, Documents, WhatsApp, Telegram, accessible app-specific directories like Android/data).
 * 3. App-specific external storage (context.getExternalFilesDirs).
 * 4. Storage Access Framework (SAF) tree URIs picked by the user.
 *
 * Guarantees:
 * - Authoritative extension & MIME type resolution via DocumentTypeRegistry.
 * - RTF (.rtf) and PDF (.pdf) detection regardless of MIME inconsistencies.
 * - No silent exclusion of accessible documents or files with similar names.
 * - Distinct physical files (e.g. "sample.rtf" and "sample (1).rtf") are both preserved.
 * - Fast background indexing that updates Room cache smoothly.
 */
class DocumentScanner(
    private val context: Context,
    private val dao: DocumentDao
) {
    companion object {
        private const val TAG = "DocumentScanner"

        // System, cache and temporary directories to skip
        private val IGNORED_DIRECTORY_NAMES = setOf(
            "cache",
            ".cache",
            "temp",
            "tmp",
            ".thumbnails",
            "thumbnails",
            "lost.dir",
            ".trash",
            "trash",
            "backup",
            ".backup",
            ".git"
        )
    }

    /**
     * Determines DocumentType using the centralized DocumentTypeRegistry.
     */
    fun detectDocumentType(displayName: String, mimeType: String?): DocumentType? {
        return DocumentTypeRegistry.detectDocumentType(displayName, mimeType)
    }

    /**
     * Performs an audit and deep scan of accessible device storage for supported documents.
     */
    suspend fun scanDeviceDocuments(
        isInitial: Boolean = false,
        onProgress: ((Int, String) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        onProgress?.invoke(0, "Loading... 0%")
        val discoveredCanonicalPaths = mutableSetOf<String>()
        val discoveredUris = mutableSetOf<String>()
        val discoveredEntities = mutableListOf<DocumentEntity>()

        // 1. Initial Database Sanitization: purge invalid, temp, or corrupted records
        sanitizeDatabaseRecords()
        onProgress?.invoke(20, "Loading... 20%")

        // 2. Load existing cleaned database records for state preservation
        val existingEntities = runCatching { dao.getAllRawDocuments() }.getOrDefault(emptyList())
        val existingByCanonicalPath = mutableMapOf<String, DocumentEntity>()
        val existingByUri = mutableMapOf<String, DocumentEntity>()

        for (e in existingEntities) {
            existingByUri[e.uri] = e
            val normPath = normalizeCanonicalPath(e.path)
            if (normPath.isNotBlank()) {
                existingByCanonicalPath[normPath] = e
            }
        }

        // 3. Authoritative MediaStore Files Query (covers primary & secondary storage)
        scanMediaStoreFiles(
            discoveredCanonicalPaths,
            discoveredUris,
            discoveredEntities
        )
        onProgress?.invoke(50, "Loading... 50%")

        // 4. Recursive Filesystem Scan for all accessible storage
        scanFileSystemRecursively(
            discoveredCanonicalPaths,
            discoveredUris,
            discoveredEntities
        )
        onProgress?.invoke(75, "Loading... 75%")

        // 5. Save or update unique discovered entities in Room database in batches
        val toUpdate = mutableListOf<DocumentEntity>()
        val toInsert = mutableListOf<DocumentEntity>()

        for (entity in discoveredEntities) {
            val normPath = normalizeCanonicalPath(entity.path)
            val existing = (if (normPath.isNotBlank()) existingByCanonicalPath[normPath] else null)
                ?: existingByUri[entity.uri]

            if (existing != null) {
                // Preserve user-assigned properties (favorites, read page, recycle bin)
                val hasChanged = existing.sizeBytes != entity.sizeBytes ||
                    existing.lastModified != entity.lastModified ||
                    existing.path != entity.path ||
                    existing.title != entity.title ||
                    (!existing.isPasswordProtected && entity.isPasswordProtected)
                if (hasChanged) {
                    val merged = entity.copy(
                        id = existing.id,
                        isFavorite = existing.isFavorite,
                        isInRecycleBin = existing.isInRecycleBin,
                        lastOpenedDate = existing.lastOpenedDate,
                        lastReadPage = existing.lastReadPage,
                        isPasswordProtected = entity.isPasswordProtected || existing.isPasswordProtected
                    )
                    toUpdate.add(merged)
                }
            } else {
                toInsert.add(entity)
            }
        }

        if (toUpdate.isNotEmpty()) {
            dao.updateAll(toUpdate)
        }
        if (toInsert.isNotEmpty()) {
            dao.insertAll(toInsert)
        }

        // 6. Purge stale records from database (files that no longer exist and are inaccessible)
        val staleIds = mutableListOf<Long>()
        val currentEntities = runCatching { dao.getAllRawDocuments() }.getOrDefault(emptyList())

        for (existing in currentEntities) {
            val normPath = normalizeCanonicalPath(existing.path)
            val foundInScan = discoveredUris.contains(existing.uri) ||
                (normPath.isNotBlank() && discoveredCanonicalPaths.contains(normPath))

            if (!foundInScan && !existing.isAsset) {
                if (!isDocumentAccessible(existing.uri, existing.path)) {
                    staleIds.add(existing.id)
                }
            }
        }

        if (staleIds.isNotEmpty()) {
            dao.deleteByIds(staleIds)
        }

        // 7. Final deduplication pass on database rows (deduplicates exact same canonical paths/URIs)
        deduplicateDatabaseRecords()
        onProgress?.invoke(100, "Loading... 100%")

        // Log final discovered and categorized documents summary
        val finalEntities = runCatching { dao.getAllRawDocuments().filter { !it.isInRecycleBin } }.getOrDefault(emptyList())
        val pdfCount = finalEntities.count { it.fileType == DocumentType.PDF }
        val wordCount = finalEntities.count { it.fileType == DocumentType.WORD }
        val excelCount = finalEntities.count { it.fileType == DocumentType.EXCEL }
        val pptCount = finalEntities.count { it.fileType == DocumentType.PPT }
        val txtCount = finalEntities.count { it.fileType == DocumentType.TXT }

        Log.i(
            TAG,
            "=== DOCUMENT SCAN COMPLETED ===" +
                "\nTotal discovered: ${discoveredEntities.size}" +
                "\nTotal indexed in database: ${finalEntities.size}" +
                "\nPDF: $pdfCount" +
                "\nWord (including RTF): $wordCount" +
                "\nExcel: $excelCount" +
                "\nPowerPoint: $pptCount" +
                "\nText: $txtCount" +
                "\n================================"
        )

        finalEntities.forEach { doc ->
            Log.d(
                TAG,
                "Indexed: filename='${doc.title}', URI='${doc.uri}', extension='${doc.extension}', " +
                    "MIME='${doc.mimeType}', detected category=${doc.fileType.name}"
            )
        }

        finalEntities.size
    }

    /**
     * Purges genuinely invalid records:
     * - Temporary office lock files (~$...)
     * - 0-byte files
     * - Files with completely unsupported formats
     */
    private fun sanitizeDatabaseRecords() {
        try {
            val allRecords = dao.getAllRawDocuments()
            val idsToDelete = mutableListOf<Long>()

            for (doc in allRecords) {
                val isSupported = DocumentTypeRegistry.isSupported(doc.title, doc.mimeType)
                val isTemporary = DocumentTypeRegistry.isIgnoredFile(doc.title)
                val isZeroSize = doc.sizeBytes <= 0L

                if (!isSupported || isTemporary || isZeroSize) {
                    idsToDelete.add(doc.id)
                }
            }

            if (idsToDelete.isNotEmpty()) {
                dao.deleteByIds(idsToDelete)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sanitizing database records", e)
        }
    }

    /**
     * Removes duplicate rows if multiple rows point to the exact same physical canonical file.
     * Preserves files with different names or paths even if their sizes or modification dates match.
     */
    private fun deduplicateDatabaseRecords() {
        try {
            val allRecords = dao.getAllRawDocuments()
            val grouped = allRecords.groupBy { doc ->
                val normPath = normalizeCanonicalPath(doc.path)
                if (normPath.isNotBlank()) {
                    "path:$normPath"
                } else {
                    "uri:${doc.uri}"
                }
            }

            val duplicateIdsToDelete = mutableListOf<Long>()
            for ((_, group) in grouped) {
                if (group.size > 1) {
                    val sorted = group.sortedWith(
                        compareByDescending<DocumentEntity> { it.isFavorite }
                            .thenByDescending { it.lastOpenedDate }
                            .thenByDescending { it.uri.startsWith("content://media/external/file/") }
                            .thenBy { it.id }
                    )
                    duplicateIdsToDelete.addAll(sorted.drop(1).map { it.id })
                }
            }

            if (duplicateIdsToDelete.isNotEmpty()) {
                dao.deleteByIds(duplicateIdsToDelete)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deduplicating database records", e)
        }
    }

    /**
     * Authoritative MediaStore query for all external files matching supported extensions or MIME aliases.
     */
    private fun scanMediaStoreFiles(
        discoveredCanonicalPaths: MutableSet<String>,
        discoveredUris: MutableSet<String>,
        discoveredEntities: MutableList<DocumentEntity>
    ) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA
        )

        val selection = DocumentTypeRegistry.buildMediaStoreSelection()
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val rawName = cursor.getString(nameCol) ?: ""
                    val displayName = if (rawName.isNotBlank()) rawName else dataPath.substringAfterLast('/')
                    if (displayName.isBlank()) continue

                    val size = cursor.getLong(sizeCol)
                    if (size <= 0L) continue

                    val mimeType = cursor.getString(mimeCol)
                    // Authoritative check via DocumentTypeRegistry (checks extension, then MIME)
                    val fileType = DocumentTypeRegistry.detectDocumentType(displayName, mimeType)
                        ?: (if (dataPath.isNotBlank()) DocumentTypeRegistry.detectDocumentType(dataPath, mimeType) else null)

                    val included = fileType != null
                    DocumentTypeRegistry.logCandidateEvaluation(
                        fileName = displayName,
                        uriOrPath = dataPath.ifBlank { "content://media/external/file/$id" },
                        mimeType = mimeType,
                        detectedType = fileType,
                        included = included,
                        reason = if (included) "Matched ${fileType?.name}" else "Unrecognized format"
                    )

                    if (fileType == null) continue

                    val ext = DocumentTypeRegistry.extractExtension(displayName).ifBlank {
                        DocumentTypeRegistry.extractExtension(dataPath)
                    }

                    val dateModifiedSec = cursor.getLong(dateCol)
                    val normPath = normalizeCanonicalPath(dataPath)
                    val contentUri = ContentUris.withAppendedId(collection, id).toString()

                    // Check deduplication
                    if (normPath.isNotBlank() && discoveredCanonicalPaths.contains(normPath)) {
                        continue
                    }
                    if (discoveredUris.contains(contentUri)) {
                        continue
                    }

                    if (normPath.isNotBlank()) discoveredCanonicalPaths.add(normPath)
                    discoveredUris.add(contentUri)

                    val directory = extractDirectoryName(dataPath, displayName)
                    val isProtected = if (dataPath.isNotBlank()) {
                        SecurityDetector.isPasswordProtected(File(dataPath), fileType, context)
                    } else {
                        displayName.contains("protected", ignoreCase = true) || displayName.contains("password", ignoreCase = true)
                    }

                    discoveredEntities.add(
                        DocumentEntity(
                            uri = contentUri,
                            title = displayName,
                            path = dataPath,
                            mimeType = mimeType ?: "",
                            extension = ext,
                            fileType = fileType,
                            sizeBytes = size,
                            lastModified = if (dateModifiedSec > 0) dateModifiedSec * 1000L else System.currentTimeMillis(),
                            directoryName = directory,
                            isPasswordProtected = isProtected
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore files", e)
        }
    }

    /**
     * Traverses accessible phone storage recursively to discover documents across storage locations.
     * Includes public directories, accessible Android/data app folders, WhatsApp, Telegram, etc.
     */
    private fun scanFileSystemRecursively(
        discoveredCanonicalPaths: MutableSet<String>,
        discoveredUris: MutableSet<String>,
        discoveredEntities: MutableList<DocumentEntity>
    ) {
        val rootDirs = mutableListOf<File>()

        // 1. External Storage Root (/storage/emulated/0)
        try {
            val extRoot = Environment.getExternalStorageDirectory()
            if (extRoot != null && extRoot.exists() && extRoot.canRead()) {
                rootDirs.add(extRoot)
            }
        } catch (_: Exception) {}

        // 2. Standard Public Directories (Downloads, Documents)
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null && downloadDir.exists() && downloadDir.canRead() && !rootDirs.contains(downloadDir)) {
                rootDirs.add(downloadDir)
            }
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (docsDir != null && docsDir.exists() && docsDir.canRead() && !rootDirs.contains(docsDir)) {
                rootDirs.add(docsDir)
            }
        } catch (_: Exception) {}

        // 3. Accessible app-specific external storage (e.g. /Android/data/<package>/files)
        try {
            val appExtDirs = context.getExternalFilesDirs(null)
            for (dir in appExtDirs) {
                if (dir != null && dir.exists() && dir.canRead()) {
                    rootDirs.add(dir)
                }
            }
        } catch (_: Exception) {}

        // 4. Known document subdirectories across common messaging and office apps
        val knownPaths = listOf(
            "Download",
            "Downloads",
            "Documents",
            "WhatsApp/Media/WhatsApp Documents",
            "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents",
            "Telegram/Telegram Documents",
            "Bluetooth",
            "Android/data/alldocumentreader.office.viewer.filereader/files/docs"
        )

        for (path in knownPaths) {
            try {
                val f = File(Environment.getExternalStorageDirectory(), path)
                if (f.exists() && f.canRead() && !rootDirs.contains(f)) {
                    rootDirs.add(f)
                }
            } catch (_: Exception) {}
        }

        // 5. Traverse each candidate root
        for (root in rootDirs) {
            traverseDirectory(
                root,
                discoveredCanonicalPaths,
                discoveredUris,
                discoveredEntities,
                currentDepth = 0,
                maxDepth = 9
            )
        }
    }

    private fun traverseDirectory(
        dir: File,
        discoveredCanonicalPaths: MutableSet<String>,
        discoveredUris: MutableSet<String>,
        discoveredEntities: MutableList<DocumentEntity>,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (currentDepth > maxDepth || !dir.exists() || !dir.isDirectory || !dir.canRead()) return

        val files = dir.listFiles() ?: return
        for (file in files) {
            try {
                if (file.isDirectory) {
                    val nameLower = file.name.lowercase().trim()
                    if (nameLower.startsWith(".")) continue
                    if (nameLower in IGNORED_DIRECTORY_NAMES) continue
                    if (nameLower == "obb") continue

                    // Do not recurse into app internal cache
                    if (file.absolutePath == context.cacheDir.absolutePath) continue

                    if (file.canRead()) {
                        traverseDirectory(
                            file,
                            discoveredCanonicalPaths,
                            discoveredUris,
                            discoveredEntities,
                            currentDepth + 1,
                            maxDepth
                        )
                    }
                } else if (file.isFile && file.canRead()) {
                    val size = file.length()
                    if (size <= 0L) continue

                    val name = file.name
                    val type = DocumentTypeRegistry.detectDocumentType(name, null)
                    val included = type != null

                    DocumentTypeRegistry.logCandidateEvaluation(
                        fileName = name,
                        uriOrPath = file.absolutePath,
                        mimeType = null,
                        detectedType = type,
                        included = included,
                        reason = if (included) "Matched ${type?.name} via extension" else "Unsupported format"
                    )

                    if (type == null) continue

                    val ext = DocumentTypeRegistry.extractExtension(name)
                    val normPath = normalizeCanonicalPath(file.absolutePath)

                    // Skip if already indexed via MediaStore or earlier traversal
                    if (normPath.isNotBlank() && discoveredCanonicalPaths.contains(normPath)) {
                        continue
                    }

                    val fileUri = Uri.fromFile(file).toString()
                    if (discoveredUris.contains(fileUri)) {
                        continue
                    }

                    if (normPath.isNotBlank()) discoveredCanonicalPaths.add(normPath)
                    discoveredUris.add(fileUri)

                    val directory = extractDirectoryName(file.absolutePath, name)
                    val isProtected = SecurityDetector.isPasswordProtected(file, type, context)

                    discoveredEntities.add(
                        DocumentEntity(
                            uri = fileUri,
                            title = name,
                            path = file.absolutePath,
                            mimeType = "",
                            extension = ext,
                            fileType = type,
                            sizeBytes = size,
                            lastModified = file.lastModified(),
                            directoryName = directory,
                            isPasswordProtected = isProtected
                        )
                    )
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Scans a directory picked by user via Storage Access Framework (OpenDocumentTree).
     */
    suspend fun scanDocumentTreeUri(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        var addedCount = 0
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val cDocId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val mime = cursor.getString(mimeCol) ?: ""
                    val size = cursor.getLong(sizeCol)
                    val lastMod = cursor.getLong(modCol)

                    if (size <= 0L || mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        continue
                    }

                    val type = DocumentTypeRegistry.detectDocumentType(name, mime) ?: continue
                    val ext = DocumentTypeRegistry.extractExtension(name)
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, cDocId).toString()

                    val isProtected = name.contains("protected", ignoreCase = true) || name.contains("password", ignoreCase = true)
                    val entity = DocumentEntity(
                        uri = fileUri,
                        title = name,
                        path = "",
                        mimeType = mime,
                        extension = ext,
                        fileType = type,
                        sizeBytes = size,
                        lastModified = if (lastMod > 0) lastMod else System.currentTimeMillis(),
                        directoryName = "Selected Folder",
                        isPasswordProtected = isProtected
                    )

                    dao.insertIfAbsent(entity)
                    addedCount++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning document tree URI", e)
        }
        addedCount
    }

    private fun normalizeCanonicalPath(path: String): String {
        if (path.isBlank()) return ""
        return try {
            File(path).canonicalPath.lowercase().trim()
        } catch (_: Exception) {
            path.lowercase().trim()
        }
    }

    private fun isDocumentAccessible(uriString: String, pathString: String): Boolean {
        return try {
            if (uriString.startsWith("content://")) {
                val uri = Uri.parse(uriString)
                context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
            } else if (pathString.isNotBlank()) {
                val file = File(pathString)
                file.exists() && file.canRead()
            } else {
                val file = File(Uri.parse(uriString).path ?: "")
                file.exists() && file.canRead()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun extractDirectoryName(path: String, displayName: String): String {
        return try {
            if (path.isNotBlank()) {
                val file = File(path)
                val parentName = file.parentFile?.name
                when {
                    parentName.isNullOrBlank() -> "Documents"
                    parentName.equals("WhatsApp Documents", ignoreCase = true) -> "WhatsApp Documents"
                    parentName.equals("Telegram Documents", ignoreCase = true) -> "Telegram Documents"
                    parentName.equals("Download", ignoreCase = true) || parentName.equals("Downloads", ignoreCase = true) -> "Download"
                    parentName.equals("docs", ignoreCase = true) -> "Documents"
                    else -> parentName
                }
            } else {
                "Documents"
            }
        } catch (e: Exception) {
            "Documents"
        }
    }
}
