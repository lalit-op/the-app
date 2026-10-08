package com.example.oneread.data

import androidx.room.TypeConverter

class DocumentConverters {
    @TypeConverter
    fun fromDocumentType(type: DocumentType?): String {
        return (type ?: DocumentType.ALL).name
    }

    @TypeConverter
    fun toDocumentType(value: String?): DocumentType {
        return if (value.isNullOrBlank()) {
            DocumentType.ALL
        } else {
            runCatching { DocumentType.valueOf(value) }.getOrDefault(DocumentType.ALL)
        }
    }
}
