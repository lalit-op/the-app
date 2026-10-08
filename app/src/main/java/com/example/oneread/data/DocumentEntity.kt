package com.example.oneread.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documents",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["fileType"]),
        Index(value = ["isInRecycleBin"]),
        Index(value = ["lastOpenedDate"])
    ]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val title: String,
    val path: String = "",
    val mimeType: String = "",
    val extension: String = "",
    val fileType: DocumentType = DocumentType.TXT,
    val sizeBytes: Long = 0L,
    val lastModified: Long = System.currentTimeMillis(),
    val lastOpenedDate: Long = 0L,
    val isFavorite: Boolean = false,
    val isInRecycleBin: Boolean = false,
    val pageCount: Int = 1,
    val lastReadPage: Int = 1,
    val directoryName: String = "Documents",
    val isAsset: Boolean = false,
    val isPasswordProtected: Boolean = false
)

fun DocumentEntity.toDocumentItem(): DocumentItem = DocumentItem(
    id = id,
    uri = uri,
    title = title,
    path = path,
    mimeType = mimeType,
    extension = extension,
    fileType = fileType,
    sizeBytes = sizeBytes,
    lastModified = lastModified,
    pageCount = pageCount,
    isFavorite = isFavorite,
    isInRecycleBin = isInRecycleBin,
    lastReadPage = lastReadPage,
    lastReadTime = lastOpenedDate,
    lastOpenedDate = lastOpenedDate,
    directoryName = directoryName,
    isAsset = isAsset,
    isPasswordProtected = isPasswordProtected
)

fun DocumentItem.toDocumentEntity(): DocumentEntity = DocumentEntity(
    id = id,
    uri = if (uri.isNotBlank()) uri else path,
    title = title,
    path = path,
    mimeType = mimeType,
    extension = extension,
    fileType = fileType,
    sizeBytes = sizeBytes,
    lastModified = lastModified,
    lastOpenedDate = if (lastOpenedDate > 0) lastOpenedDate else lastReadTime,
    isFavorite = isFavorite,
    isInRecycleBin = isInRecycleBin,
    pageCount = pageCount,
    lastReadPage = lastReadPage,
    directoryName = directoryName,
    isAsset = isAsset,
    isPasswordProtected = isPasswordProtected
)
