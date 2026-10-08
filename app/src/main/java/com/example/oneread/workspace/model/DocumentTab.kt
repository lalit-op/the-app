package com.example.oneread.workspace.model

import com.example.oneread.data.DocumentItem
import java.io.File
import java.util.UUID

/**
 * Represents an independent document tab in the multi-tab document workspace.
 * Preserves the individual state of each document (scroll position, zoom,
 * current page/sheet/slide, modification status, pin state, etc.).
 */
data class DocumentTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val originalFileName: String,
    val filePath: String = "",
    val uriString: String = "",
    val fileType: DocumentFormat,
    val isActive: Boolean = false,
    val isModified: Boolean = false,
    val isPinned: Boolean = false,
    val scrollPosition: Int = 0,
    val zoomLevel: Float = 1.0f,
    val currentPage: Int = 1, // PDF page, Excel sheet index, or PPT slide index
    val currentSheet: Int = 1,
    val currentSlide: Int = 1,
    val totalPages: Int = 1,
    val contentText: String? = null, // In-memory content for editable Text, Markdown, CSV
    val isNewUnsavedDocument: Boolean = false,
    val documentItemId: Long? = null,
    val lastAccessTime: Long = System.currentTimeMillis()
) {
    val displayExtension: String
        get() {
            val ext = originalFileName.substringAfterLast('.', "")
            return if (ext.isNotBlank()) ext.uppercase() else fileType.primaryExtension.uppercase()
        }

    val locationText: String
        get() = when (fileType) {
            DocumentFormat.PDF -> "Page $currentPage of ${maxOf(1, totalPages)}"
            DocumentFormat.WORD -> "Page $currentPage"
            DocumentFormat.EXCEL -> "Sheet $currentSheet"
            DocumentFormat.PPT -> "Slide $currentSlide of ${maxOf(1, totalPages)}"
            DocumentFormat.CSV -> "Sheet 1"
            DocumentFormat.TEXT, DocumentFormat.MARKDOWN -> if (scrollPosition > 0) "Line $scrollPosition" else "Start"
            DocumentFormat.IMAGE -> "${(zoomLevel * 100).toInt()}%"
            DocumentFormat.UNKNOWN -> "Page $currentPage"
        }

    val fullPathOrUri: String
        get() = filePath.ifBlank { uriString.ifBlank { "Untitled" } }

    val fileExists: Boolean
        get() = if (filePath.isNotBlank()) File(filePath).exists() else true

    fun toDocumentItem(): DocumentItem {
        return DocumentItem(
            id = documentItemId ?: id.hashCode().toLong(),
            uri = uriString,
            title = originalFileName.ifBlank { title },
            path = filePath,
            fileType = fileType.toDocumentType(),
            pageCount = totalPages,
            lastReadPage = currentPage
        )
    }

    companion object {
        fun fromDocumentItem(doc: DocumentItem, activate: Boolean = true): DocumentTab {
            val format = DocumentFormat.fromDocumentType(doc.fileType, doc.extension)
            val fullTitle = if (doc.title.contains(".") || doc.extension.isBlank()) {
                doc.title
            } else {
                "${doc.title}.${doc.extension.trimStart('.')}"
            }
            return DocumentTab(
                id = "doc_${doc.id}_${UUID.randomUUID().toString().take(8)}",
                title = fullTitle,
                originalFileName = fullTitle,
                filePath = doc.path,
                uriString = doc.uri,
                fileType = format,
                isActive = activate,
                isModified = false,
                isPinned = false,
                scrollPosition = 0,
                zoomLevel = 1.0f,
                currentPage = doc.lastReadPage.coerceAtLeast(1),
                totalPages = doc.pageCount.coerceAtLeast(1),
                documentItemId = doc.id
            )
        }

        fun createBlankDocument(format: DocumentFormat, customName: String? = null): DocumentTab {
            val ext = format.primaryExtension
            val defaultName = customName ?: "Untitled.${ext}"
            val defaultContent = when (format) {
                DocumentFormat.MARKDOWN -> "# ${defaultName.removeSuffix(".$ext")}\n\nWelcome to your new markdown document.\n\n- Feature 1\n- Feature 2\n"
                DocumentFormat.CSV -> "Item,Quantity,Price,Status\nProduct A,10,$25.00,In Stock\nProduct B,5,$40.00,Low Stock\nProduct C,100,$5.00,Available\n"
                DocumentFormat.TEXT -> "Untitled Document\nCreated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n\nType your text here...\n"
                else -> ""
            }
            return DocumentTab(
                id = "new_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                title = defaultName,
                originalFileName = defaultName,
                filePath = "",
                uriString = "",
                fileType = format,
                isActive = true,
                isModified = true,
                isPinned = false,
                scrollPosition = 0,
                zoomLevel = 1.0f,
                currentPage = 1,
                totalPages = 1,
                contentText = defaultContent,
                isNewUnsavedDocument = true
            )
        }
    }
}
