package com.example.oneread.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class DocumentRepository(
    private val context: Context,
    private val dao: DocumentDao
) {
    private val scanner = DocumentScanner(context, dao)

    private fun filterAndDeduplicate(entities: List<DocumentEntity>): List<DocumentItem> {
        return entities
            .filter { entity ->
                val isSupported = DocumentTypeRegistry.isSupported(entity.title, entity.mimeType) ||
                    DocumentTypeRegistry.isSupported(entity.extension, entity.mimeType)
                val isIgnored = DocumentTypeRegistry.isIgnoredFile(entity.title)
                entity.sizeBytes > 0L && isSupported && !isIgnored
            }
            .distinctBy { entity ->
                val path = entity.path.trim()
                if (path.isNotBlank()) {
                    try { File(path).canonicalPath.lowercase() } catch (_: Exception) { path.lowercase() }
                } else {
                    entity.uri.ifBlank { "${entity.title}_${entity.lastModified}_${entity.sizeBytes}" }
                }
            }
            .map { it.toDocumentItem() }
    }

    val allActiveDocuments: Flow<List<DocumentItem>> = dao.getAllActiveDocuments().map { list ->
        filterAndDeduplicate(list)
    }

    val recentDocuments: Flow<List<DocumentItem>> = dao.getRecentDocuments().map { list ->
        filterAndDeduplicate(list)
    }

    val favorites: Flow<List<DocumentItem>> = dao.getFavorites().map { list ->
        filterAndDeduplicate(list)
    }

    val recycleBin: Flow<List<DocumentItem>> = dao.getRecycleBinDocuments().map { list ->
        filterAndDeduplicate(list)
    }

    val activeCount: Flow<Int> = allActiveDocuments.map { it.size }
    val recycleBinCount: Flow<Int> = recycleBin.map { it.size }
    val recentCount: Flow<Int> = recentDocuments.map { it.size }

    fun getCountByType(type: DocumentType): Flow<Int> =
        if (type == DocumentType.ALL) activeCount
        else allActiveDocuments.map { list -> list.count { it.fileType == type } }

    fun getDocumentsByType(type: DocumentType): Flow<List<DocumentItem>> =
        if (type == DocumentType.ALL) allActiveDocuments
        else allActiveDocuments.map { list -> list.filter { it.fileType == type } }

    fun searchDocuments(query: String): Flow<List<DocumentItem>> =
        allActiveDocuments.map { list ->
            if (query.isBlank()) list
            else {
                val q = query.trim().lowercase()
                list.filter { doc ->
                    doc.title.lowercase().contains(q) ||
                    doc.displayTitle.lowercase().contains(q) ||
                    doc.extension.lowercase().contains(q) ||
                    doc.directoryName.lowercase().contains(q) ||
                    doc.fileType.name.lowercase().contains(q) ||
                    doc.fileType.displayName.lowercase().contains(q) ||
                    doc.path.lowercase().contains(q)
                }
            }
        }

    suspend fun scanDeviceDocuments(
        isInitial: Boolean = false,
        onProgress: ((Int, String) -> Unit)? = null
    ): Int {
        return scanner.scanDeviceDocuments(isInitial, onProgress)
    }

    suspend fun scanDirectoryTree(treeUri: Uri): Int {
        return scanner.scanDocumentTreeUri(treeUri)
    }

    suspend fun getDocumentById(id: Long): DocumentItem? = withContext(Dispatchers.IO) {
        dao.getDocumentById(id)?.toDocumentItem()
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        dao.updateFavorite(id, isFavorite)
    }

    suspend fun updateLastRead(id: Long, page: Int = 1) = withContext(Dispatchers.IO) {
        dao.updateLastRead(id, System.currentTimeMillis(), page)
    }

    suspend fun moveToRecycleBin(id: Long) = withContext(Dispatchers.IO) {
        dao.moveToRecycleBin(id)
    }

    suspend fun restoreFromRecycleBin(id: Long) = withContext(Dispatchers.IO) {
        dao.restoreFromRecycleBin(id)
    }

    suspend fun permanentDelete(id: Long) = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentById(id)
        if (doc != null) {
            if (doc.path.isNotBlank()) {
                val file = File(doc.path)
                if (file.exists() && file.isFile) {
                    file.delete()
                }
            }
            dao.deletePermanently(id)
        }
    }

    suspend fun emptyRecycleBin() = withContext(Dispatchers.IO) {
        dao.emptyRecycleBin()
    }

    suspend fun renameDocument(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentById(id) ?: return@withContext
        if (doc.path.isNotBlank()) {
            val oldFile = File(doc.path)
            val ext = oldFile.extension
            val finalTitle = if (!newTitle.contains(".") && ext.isNotEmpty()) "$newTitle.$ext" else newTitle
            val newFile = File(oldFile.parentFile, finalTitle)
            if (oldFile.exists() && oldFile.renameTo(newFile)) {
                dao.update(doc.copy(title = finalTitle, path = newFile.absolutePath))
                return@withContext
            }
        }
        dao.renameDocument(id, newTitle)
    }

    suspend fun importFileFromUri(uri: Uri, displayName: String? = null): DocumentItem = withContext(Dispatchers.IO) {
        val resolvedName = if (!displayName.isNullOrBlank()) {
            displayName
        } else {
            com.example.oneread.workspace.docmanager.FileDetector.getFileNameFromUri(context, uri)
        }
        val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val targetFile = File(docsDir, resolvedName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        val mimeType = try { context.contentResolver.getType(uri) } catch (_: Exception) { null }
        val ext = targetFile.extension.lowercase().ifBlank {
            DocumentTypeRegistry.extractExtension(resolvedName)
        }

        // Authoritative detection: name + MIME, then magic bytes if needed
        val type = scanner.detectDocumentType(resolvedName, mimeType)
            ?: (if (targetFile.exists() && targetFile.length() >= 4) {
                when (com.example.oneread.workspace.docmanager.FileDetector.detectFormat(targetFile)) {
                    com.example.oneread.workspace.model.DocumentFormat.PDF -> DocumentType.PDF
                    com.example.oneread.workspace.model.DocumentFormat.WORD -> DocumentType.WORD
                    com.example.oneread.workspace.model.DocumentFormat.EXCEL -> DocumentType.EXCEL
                    com.example.oneread.workspace.model.DocumentFormat.PPT -> DocumentType.PPT
                    else -> null
                }
            } else null)
            ?: if (ext in DocumentTypeRegistry.PDF_EXTENSIONS) DocumentType.PDF else DocumentType.TXT

        val pageCount = if (type == DocumentType.PDF) countPdfPages(targetFile) else 1
        val isProtected = SecurityDetector.isPasswordProtected(targetFile, type, context)

        val entity = DocumentEntity(
            uri = Uri.fromFile(targetFile).toString(),
            title = resolvedName,
            path = targetFile.absolutePath,
            mimeType = mimeType ?: "",
            extension = ext,
            fileType = type,
            sizeBytes = targetFile.length(),
            lastModified = System.currentTimeMillis(),
            pageCount = pageCount,
            isFavorite = false,
            lastReadPage = 1,
            lastOpenedDate = System.currentTimeMillis(),
            directoryName = "HR Read Docs",
            isPasswordProtected = isProtected
        )

        val id = dao.insert(entity)
        entity.copy(id = id).toDocumentItem()
    }

    suspend fun createPdfFromImages(
        imageUris: List<Uri>,
        title: String,
        isLandscape: Boolean = false
    ): DocumentItem = withContext(Dispatchers.IO) {
        val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val fileName = if (title.endsWith(".pdf", ignoreCase = true)) title else "$title.pdf"
        val targetFile = File(docsDir, fileName)

        val pageWidth = if (isLandscape) 842 else 595
        val pageHeight = if (isLandscape) 595 else 842

        val pdfDoc = PdfDocument()

        try {
            imageUris.forEachIndexed { index, uri ->
                val bitmap = loadBitmapFromUri(uri)
                if (bitmap != null) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val canvas = page.canvas

                    val scale = minOf(
                        (pageWidth - 40f) / bitmap.width,
                        (pageHeight - 40f) / bitmap.height
                    )
                    val scaledW = bitmap.width * scale
                    val scaledH = bitmap.height * scale
                    val left = (pageWidth - scaledW) / 2f
                    val top = (pageHeight - scaledH) / 2f

                    val destRect = android.graphics.RectF(left, top, left + scaledW, top + scaledH)
                    canvas.drawBitmap(bitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))

                    pdfDoc.finishPage(page)
                    bitmap.recycle()
                }
            }

            FileOutputStream(targetFile).use { out ->
                pdfDoc.writeTo(out)
            }
        } finally {
            pdfDoc.close()
        }

        val entity = DocumentEntity(
            uri = Uri.fromFile(targetFile).toString(),
            title = fileName,
            path = targetFile.absolutePath,
            mimeType = "application/pdf",
            extension = "pdf",
            fileType = DocumentType.PDF,
            sizeBytes = targetFile.length(),
            lastModified = System.currentTimeMillis(),
            pageCount = imageUris.size,
            isFavorite = false,
            lastReadPage = 1,
            lastOpenedDate = System.currentTimeMillis(),
            directoryName = "HR Read Docs"
        )

        val id = dao.insert(entity)
        entity.copy(id = id).toDocumentItem()
    }

    suspend fun mergePdfs(pdfPaths: List<String>, title: String): DocumentItem = withContext(Dispatchers.IO) {
        val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val fileName = if (title.endsWith(".pdf", ignoreCase = true)) title else "$title.pdf"
        val targetFile = File(docsDir, fileName)

        val mergedDoc = PdfDocument()
        var totalPages = 0

        try {
            for (path in pdfPaths) {
                val file = File(path)
                if (!file.exists()) continue
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)

                for (i in 0 until renderer.pageCount) {
                    totalPages++
                    val page = renderer.openPage(i)
                    val width = page.width
                    val height = page.height

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    page.close()

                    val pageInfo = PdfDocument.PageInfo.Builder(width, height, totalPages).create()
                    val newPage = mergedDoc.startPage(pageInfo)
                    newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    mergedDoc.finishPage(newPage)
                    bitmap.recycle()
                }
                renderer.close()
                pfd.close()
            }

            FileOutputStream(targetFile).use { out ->
                mergedDoc.writeTo(out)
            }
        } finally {
            mergedDoc.close()
        }

        val entity = DocumentEntity(
            uri = Uri.fromFile(targetFile).toString(),
            title = fileName,
            path = targetFile.absolutePath,
            mimeType = "application/pdf",
            extension = "pdf",
            fileType = DocumentType.PDF,
            sizeBytes = targetFile.length(),
            lastModified = System.currentTimeMillis(),
            pageCount = totalPages,
            isFavorite = false,
            lastReadPage = 1,
            lastOpenedDate = System.currentTimeMillis(),
            directoryName = "HR Read Docs"
        )
        val id = dao.insert(entity)
        entity.copy(id = id).toDocumentItem()
    }

    suspend fun splitPdf(sourcePath: String, pageIndices: List<Int>, title: String): DocumentItem = withContext(Dispatchers.IO) {
        val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val fileName = if (title.endsWith(".pdf", ignoreCase = true)) title else "$title.pdf"
        val targetFile = File(docsDir, fileName)

        val sourceFile = File(sourcePath)
        val pfd = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val splitDoc = PdfDocument()

        try {
            pageIndices.forEachIndexed { newIndex, pageIdx ->
                if (pageIdx in 0 until renderer.pageCount) {
                    val page = renderer.openPage(pageIdx)
                    val width = page.width
                    val height = page.height
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    page.close()

                    val pageInfo = PdfDocument.PageInfo.Builder(width, height, newIndex + 1).create()
                    val newPage = splitDoc.startPage(pageInfo)
                    newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    splitDoc.finishPage(newPage)
                    bitmap.recycle()
                }
            }

            FileOutputStream(targetFile).use { out ->
                splitDoc.writeTo(out)
            }
        } finally {
            splitDoc.close()
            renderer.close()
            pfd.close()
        }

        val entity = DocumentEntity(
            uri = Uri.fromFile(targetFile).toString(),
            title = fileName,
            path = targetFile.absolutePath,
            mimeType = "application/pdf",
            extension = "pdf",
            fileType = DocumentType.PDF,
            sizeBytes = targetFile.length(),
            lastModified = System.currentTimeMillis(),
            pageCount = pageIndices.size,
            isFavorite = false,
            lastReadPage = 1,
            lastOpenedDate = System.currentTimeMillis(),
            directoryName = "HR Read Docs"
        )
        val id = dao.insert(entity)
        entity.copy(id = id).toDocumentItem()
    }

    fun shareDocument(context: Context, doc: DocumentItem) {
        val uri = if (doc.uri.startsWith("content://")) {
            Uri.parse(doc.uri)
        } else {
            val file = File(doc.path)
            if (!file.exists()) return
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = when (doc.fileType) {
                DocumentType.PDF -> "application/pdf"
                DocumentType.TXT -> "text/plain"
                else -> "*/*"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, doc.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share ${doc.title}"))
    }

    fun printDocument(context: Context, doc: DocumentItem) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val pdi = PrintDocumentInfo.Builder(doc.title)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback?.onLayoutFinished(pdi, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                var input: InputStream? = null
                var output: FileOutputStream? = null
                try {
                    input = if (doc.uri.startsWith("content://")) {
                        context.contentResolver.openInputStream(Uri.parse(doc.uri))
                    } else {
                        FileInputStream(File(doc.path))
                    }
                    output = FileOutputStream(destination?.fileDescriptor)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input?.read(buffer).also { bytesRead = it ?: -1 } != null && bytesRead >= 0) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onWriteCancelled()
                            return
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    try {
                        input?.close()
                        output?.close()
                    } catch (_: Exception) {}
                }
            }
        }
        printManager.print(doc.title, printAdapter, PrintAttributes.Builder().build())
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun countPdfPages(file: File): Int {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            1
        }
    }
}
