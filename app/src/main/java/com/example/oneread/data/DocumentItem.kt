package com.example.oneread.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DocumentType {
    ALL,
    PDF,
    WORD,
    EXCEL,
    PPT,
    TXT;

    val displayName: String
        get() = when (this) {
            ALL -> "All"
            PDF -> "PDF"
            WORD -> "Word"
            EXCEL -> "Excel"
            PPT -> "PowerPoint"
            TXT -> "Text"
        }

    val badgeColorHex: Long
        get() = when (this) {
            PDF -> 0xFFDC2626
            WORD -> 0xFF2563EB
            EXCEL -> 0xFF16A34A
            PPT -> 0xFFEA580C
            TXT -> 0xFF475569
            ALL -> 0xFF3B82F6
        }
}

data class DocumentItem(
    val id: Long = 0,
    val uri: String = "",
    val title: String,
    val path: String = "",
    val mimeType: String = "",
    val extension: String = "",
    val fileType: DocumentType,
    val sizeBytes: Long = 0,
    val lastModified: Long = System.currentTimeMillis(),
    val pageCount: Int = 1,
    val isFavorite: Boolean = false,
    val isInRecycleBin: Boolean = false,
    val lastReadPage: Int = 1,
    val lastReadTime: Long = 0,
    val lastOpenedDate: Long = lastReadTime,
    val directoryName: String = "Documents",
    val isAsset: Boolean = false,
    val isPasswordProtected: Boolean = false
) {
    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 KB"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format(Locale.US, "%.1f MB", mb)
            } else {
                String.format(Locale.US, "%.0f KB", kb)
            }
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            return sdf.format(Date(if (lastReadTime > 0) lastReadTime else lastModified))
        }

    val displayTitle: String
        get() {
            if (title.contains(".") || extension.isBlank()) return title
            return "$title.${extension.trimStart('.')}"
        }
}
