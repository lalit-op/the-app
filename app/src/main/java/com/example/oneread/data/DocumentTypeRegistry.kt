package com.example.oneread.data

import android.provider.MediaStore
import android.util.Log

/**
 * Centralized, authoritative registry of supported document types, file extensions,
 * and MIME type aliases for HR Read.
 *
 * Provides:
 * - Authoritative extension and MIME mapping for all document categories (PDF, Word, Excel, PPT, Text, Images).
 * - Fallback extension resolution when MIME types are missing, generic (e.g. application/octet-stream), or inconsistent.
 * - Safe extension parsing handling case insensitivity and multiple dots (e.g. file-sample_100kB (1).rtf).
 * - Full RTF support mapped to DocumentType.WORD.
 * - Diagnostic logging for candidate file discovery.
 */
object DocumentTypeRegistry {
    private const val TAG = "DocumentDiscovery"

    // 1. Supported Extensions by Category
    val PDF_EXTENSIONS = setOf("pdf")

    val WORD_EXTENSIONS = setOf(
        "doc", "docx", "docm", "dot", "dotx", "dotm", "rtf", "odt"
    )

    val EXCEL_EXTENSIONS = setOf(
        "xls", "xlsx", "xlsm", "xlt", "xltx", "xltm", "ods", "csv"
    )

    val PPT_EXTENSIONS = setOf(
        "ppt", "pptx", "pptm", "pot", "potx", "potm", "pps", "ppsx", "ppsm", "odp"
    )

    val TEXT_EXTENSIONS = setOf(
        "txt", "text", "log", "md", "markdown", "json", "xml", "yaml", "yml"
    )

    val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "tiff", "tif", "svg"
    )

    // Complete set of all supported document extensions
    val ALL_SUPPORTED_EXTENSIONS: Set<String> =
        PDF_EXTENSIONS + WORD_EXTENSIONS + EXCEL_EXTENSIONS + PPT_EXTENSIONS + TEXT_EXTENSIONS

    // 2. MIME Type Aliases by Category
    val PDF_MIMES = setOf(
        "application/pdf",
        "application/x-pdf",
        "application/acrobat",
        "applications/vnd.pdf",
        "text/pdf"
    )

    val RTF_MIMES = setOf(
        "application/rtf",
        "text/rtf",
        "application/x-rtf",
        "text/richtext"
    )

    val WORD_MIMES = setOf(
        "application/msword",
        "application/doc",
        "application/ms-word",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
        "application/vnd.ms-word.document.macroenabled.12",
        "application/vnd.ms-word.template.macroenabled.12",
        "application/vnd.oasis.opendocument.text"
    ) + RTF_MIMES

    val EXCEL_MIMES = setOf(
        "application/vnd.ms-excel",
        "application/msexcel",
        "application/x-msexcel",
        "application/x-ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
        "application/vnd.ms-excel.sheet.macroenabled.12",
        "application/vnd.ms-excel.template.macroenabled.12",
        "application/vnd.oasis.opendocument.spreadsheet",
        "text/csv",
        "application/csv",
        "text/comma-separated-values"
    )

    val PPT_MIMES = setOf(
        "application/vnd.ms-powerpoint",
        "application/mspowerpoint",
        "application/powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.openxmlformats-officedocument.presentationml.template",
        "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
        "application/vnd.ms-powerpoint.presentation.macroenabled.12",
        "application/vnd.oasis.opendocument.presentation"
    )

    val TEXT_MIMES = setOf(
        "text/plain",
        "text/markdown",
        "text/x-markdown",
        "application/json",
        "text/json",
        "application/xml",
        "text/xml",
        "text/yaml",
        "application/x-yaml",
        "text/x-log"
    )

    val ALL_SUPPORTED_MIMES: Set<String> =
        PDF_MIMES + WORD_MIMES + EXCEL_MIMES + PPT_MIMES + TEXT_MIMES

    /**
     * Safely extracts file extension in lowercase without dots.
     * Correctly handles multiple dots (e.g., "file-sample_100kB (1).rtf", "name.with.dots.pdf").
     * Case-insensitive: "DOC.PDF" -> "pdf", "file.RTF" -> "rtf".
     */
    fun extractExtension(fileNameOrPath: String): String {
        val clean = fileNameOrPath.trim()
        val simpleName = clean.substringAfterLast('/').substringAfterLast('\\')
        if (simpleName.startsWith(".") && !simpleName.drop(1).contains('.')) {
            // Hidden file like .nomedia without additional extension
            return ""
        }
        val dotIndex = simpleName.lastIndexOf('.')
        return if (dotIndex in 0 until simpleName.length - 1) {
            simpleName.substring(dotIndex + 1).lowercase().trim()
        } else {
            ""
        }
    }

    /**
     * Identifies genuinely temporary, lock, or cache files that should be filtered out.
     */
    fun isIgnoredFile(fileName: String): Boolean {
        val trimmed = fileName.trim()
        val simpleName = trimmed.substringAfterLast('/').substringAfterLast('\\')
        if (simpleName.isBlank()) return true
        if (simpleName.startsWith("~")) return true // Office lock file
        if (simpleName.startsWith(".")) return true // Hidden Unix file
        val lower = simpleName.lowercase()
        return lower.endsWith(".tmp") ||
            lower.endsWith(".crdownload") ||
            lower.endsWith(".part") ||
            lower.endsWith(".bak")
    }

    /**
     * Determines DocumentType by checking BOTH extension and MIME type.
     * Extension serves as the authoritative fallback if MIME type is missing,
     * generic ("application/octet-stream"), or inaccurate.
     */
    fun detectDocumentType(fileName: String, mimeType: String? = null): DocumentType? {
        if (isIgnoredFile(fileName)) {
            return null
        }

        val ext = extractExtension(fileName)
        val cleanMime = mimeType?.lowercase()?.trim()

        // 1. Authoritative extension check
        when {
            ext in PDF_EXTENSIONS -> return DocumentType.PDF
            ext in WORD_EXTENSIONS -> return DocumentType.WORD
            ext in EXCEL_EXTENSIONS -> return DocumentType.EXCEL
            ext in PPT_EXTENSIONS -> return DocumentType.PPT
            ext in TEXT_EXTENSIONS -> return DocumentType.TXT
        }

        // 2. MIME type check if extension was not recognized or absent
        if (!cleanMime.isNullOrBlank() && cleanMime != "application/octet-stream" && cleanMime != "binary/octet-stream") {
            when {
                cleanMime in PDF_MIMES || cleanMime.contains("pdf") -> return DocumentType.PDF
                cleanMime in WORD_MIMES || cleanMime.contains("word") || cleanMime.contains("officedocument.wordprocessing") -> return DocumentType.WORD
                cleanMime in EXCEL_MIMES || cleanMime.contains("spreadsheet") || cleanMime.contains("excel") || cleanMime.contains("csv") -> return DocumentType.EXCEL
                cleanMime in PPT_MIMES || cleanMime.contains("presentation") || cleanMime.contains("powerpoint") -> return DocumentType.PPT
                cleanMime in TEXT_MIMES || cleanMime.startsWith("text/") -> return DocumentType.TXT
            }
        }

        return null
    }

    fun isSupported(fileName: String, mimeType: String? = null): Boolean {
        return detectDocumentType(fileName, mimeType) != null
    }

    fun isPdf(fileName: String, mimeType: String? = null): Boolean {
        return detectDocumentType(fileName, mimeType) == DocumentType.PDF
    }

    fun isRtf(fileName: String, mimeType: String? = null): Boolean {
        val ext = extractExtension(fileName)
        val cleanMime = mimeType?.lowercase()?.trim()
        return ext == "rtf" || (cleanMime != null && cleanMime in RTF_MIMES)
    }

    fun isWord(fileName: String, mimeType: String? = null): Boolean {
        return detectDocumentType(fileName, mimeType) == DocumentType.WORD
    }

    fun isExcel(fileName: String, mimeType: String? = null): Boolean {
        return detectDocumentType(fileName, mimeType) == DocumentType.EXCEL
    }

    fun isPowerPoint(fileName: String, mimeType: String? = null): Boolean {
        return detectDocumentType(fileName, mimeType) == DocumentType.PPT
    }

    fun isText(fileName: String, mimeType: String? = null): Boolean {
        return detectDocumentType(fileName, mimeType) == DocumentType.TXT
    }

    fun getAllSupportedExtensions(): Set<String> = ALL_SUPPORTED_EXTENSIONS

    fun getExtensionsForType(type: DocumentType): Set<String> = when (type) {
        DocumentType.ALL -> ALL_SUPPORTED_EXTENSIONS
        DocumentType.PDF -> PDF_EXTENSIONS
        DocumentType.WORD -> WORD_EXTENSIONS
        DocumentType.EXCEL -> EXCEL_EXTENSIONS
        DocumentType.PPT -> PPT_EXTENSIONS
        DocumentType.TXT -> TEXT_EXTENSIONS
    }

    fun getMimeTypesForType(type: DocumentType): Set<String> = when (type) {
        DocumentType.ALL -> ALL_SUPPORTED_MIMES
        DocumentType.PDF -> PDF_MIMES
        DocumentType.WORD -> WORD_MIMES
        DocumentType.EXCEL -> EXCEL_MIMES
        DocumentType.PPT -> PPT_MIMES
        DocumentType.TXT -> TEXT_MIMES
    }

    /**
     * Builds an optimized SQL selection query for MediaStore to discover all candidate documents
     * by either supported extensions in DISPLAY_NAME/_DATA or supported MIME types.
     */
    fun buildMediaStoreSelection(): String {
        val nameConditions = ALL_SUPPORTED_EXTENSIONS.joinToString(" OR ") { ext ->
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.$ext'"
        }
        val mimeConditions = ALL_SUPPORTED_MIMES.joinToString("', '", prefix = "${MediaStore.Files.FileColumns.MIME_TYPE} IN ('", postfix = "')")

        return "(($nameConditions) OR ($mimeConditions)) AND ${MediaStore.Files.FileColumns.SIZE} > 0"
    }

    /**
     * Internal diagnostic logger for candidate file evaluation.
     */
    fun logCandidateEvaluation(
        fileName: String,
        uriOrPath: String,
        mimeType: String?,
        detectedType: DocumentType?,
        included: Boolean,
        reason: String
    ) {
        val ext = extractExtension(fileName)
        Log.d(
            TAG,
            "Candidate: $fileName | Ext: $ext | Mime: $mimeType | Type: ${detectedType?.name ?: "NONE"} | " +
                "Included: $included | Reason: $reason | Path/URI: $uriOrPath"
        )
    }
}
