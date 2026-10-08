package com.example.oneread.workspace.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.oneread.data.DocumentType

/**
 * Extensible registry of supported document formats for the multi-tab workspace.
 * Supports PDF, Word, Excel, PowerPoint, Text, CSV, Markdown, and Images.
 */
enum class DocumentFormat(
    val displayName: String,
    val primaryExtension: String,
    val extensions: Set<String>,
    val badgeColor: Color,
    val defaultIconName: String,
    val isEditable: Boolean = false
) {
    PDF(
        displayName = "PDF",
        primaryExtension = "pdf",
        extensions = setOf("pdf"),
        badgeColor = Color(0xFFDC2626), // Red
        defaultIconName = "pdf"
    ),
    WORD(
        displayName = "Word",
        primaryExtension = "docx",
        extensions = setOf("doc", "docx", "dot", "dotx", "rtf"),
        badgeColor = Color(0xFF2563EB), // Blue
        defaultIconName = "word"
    ),
    EXCEL(
        displayName = "Excel",
        primaryExtension = "xlsx",
        extensions = setOf("xls", "xlsx", "xlsm", "xlt", "xltx", "xltm"),
        badgeColor = Color(0xFF16A34A), // Green
        defaultIconName = "excel"
    ),
    PPT(
        displayName = "PowerPoint",
        primaryExtension = "pptx",
        extensions = setOf("ppt", "pptx", "pptm", "pot", "potx", "potm", "pps", "ppsx", "ppsm", "odp"),
        badgeColor = Color(0xFFEA580C), // Orange
        defaultIconName = "ppt"
    ),
    TEXT(
        displayName = "Text",
        primaryExtension = "txt",
        extensions = setOf("txt", "log", "ini", "conf", "json", "xml", "html", "java", "kt", "c", "cpp", "py"),
        badgeColor = Color(0xFF64748B), // Slate
        defaultIconName = "txt",
        isEditable = true
    ),
    CSV(
        displayName = "CSV",
        primaryExtension = "csv",
        extensions = setOf("csv", "tsv"),
        badgeColor = Color(0xFF0D9488), // Teal
        defaultIconName = "csv",
        isEditable = true
    ),
    MARKDOWN(
        displayName = "Markdown",
        primaryExtension = "md",
        extensions = setOf("md", "markdown"),
        badgeColor = Color(0xFF9333EA), // Purple
        defaultIconName = "markdown",
        isEditable = true
    ),
    IMAGE(
        displayName = "Image",
        primaryExtension = "png",
        extensions = setOf("png", "jpg", "jpeg", "webp", "bmp", "gif"),
        badgeColor = Color(0xFFD97706), // Amber
        defaultIconName = "image"
    ),
    UNKNOWN(
        displayName = "Document",
        primaryExtension = "doc",
        extensions = emptySet(),
        badgeColor = Color(0xFF6B7280), // Gray
        defaultIconName = "generic"
    );

    fun getIcon(): ImageVector = when (this) {
        PDF -> Icons.Default.PictureAsPdf
        WORD -> Icons.Default.Description
        EXCEL -> Icons.Default.GridOn
        PPT -> Icons.Default.Slideshow
        TEXT -> Icons.Default.TextSnippet
        CSV -> Icons.Default.TableChart
        MARKDOWN -> Icons.Default.Notes
        IMAGE -> Icons.Default.Image
        UNKNOWN -> Icons.Default.Description
    }

    fun toDocumentType(): DocumentType = when (this) {
        PDF -> DocumentType.PDF
        WORD -> DocumentType.WORD
        EXCEL -> DocumentType.EXCEL
        PPT -> DocumentType.PPT
        TEXT, CSV, MARKDOWN -> DocumentType.TXT
        IMAGE, UNKNOWN -> DocumentType.ALL
    }

    companion object {
        fun fromExtension(extension: String): DocumentFormat {
            val cleanExt = extension.trim().lowercase().removePrefix(".")
            return values().firstOrNull { it.extensions.contains(cleanExt) } ?: when (cleanExt) {
                "csv", "tsv" -> CSV
                "md", "markdown" -> MARKDOWN
                "png", "jpg", "jpeg", "webp", "bmp", "gif" -> IMAGE
                else -> UNKNOWN
            }
        }

        fun fromDocumentType(type: DocumentType, extension: String = ""): DocumentFormat {
            val fromExt = fromExtension(extension)
            if (fromExt != UNKNOWN) return fromExt
            return when (type) {
                DocumentType.PDF -> PDF
                DocumentType.WORD -> WORD
                DocumentType.EXCEL -> EXCEL
                DocumentType.PPT -> PPT
                DocumentType.TXT -> TEXT
                DocumentType.ALL -> UNKNOWN
            }
        }
    }
}
