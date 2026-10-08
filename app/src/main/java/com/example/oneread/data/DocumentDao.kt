package com.example.oneread.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE isInRecycleBin = 0 ORDER BY lastModified DESC")
    fun getAllActiveDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isInRecycleBin = 0 AND fileType = :type ORDER BY lastModified DESC")
    fun getDocumentsByType(type: DocumentType): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isInRecycleBin = 0 AND isFavorite = 1 ORDER BY lastModified DESC")
    fun getFavorites(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isInRecycleBin = 0 AND lastOpenedDate > 0 ORDER BY lastOpenedDate DESC LIMIT 50")
    fun getRecentDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isInRecycleBin = 1 ORDER BY lastModified DESC")
    fun getRecycleBinDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE uri = :uri LIMIT 1")
    fun getDocumentByUri(uri: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE path = :path LIMIT 1")
    fun getDocumentByPath(path: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE isInRecycleBin = 0 AND title LIKE '%' || :query || '%' ORDER BY lastModified DESC")
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(document: DocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIfAbsent(document: DocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(documents: List<DocumentEntity>): List<Long>

    @Update
    fun update(document: DocumentEntity): Int

    @Update
    fun updateAll(documents: List<DocumentEntity>): Int

    @Query("UPDATE documents SET isFavorite = :isFavorite WHERE id = :id")
    fun updateFavorite(id: Long, isFavorite: Boolean): Int

    @Query("UPDATE documents SET lastOpenedDate = :time, lastReadPage = :page WHERE id = :id")
    fun updateLastRead(id: Long, time: Long, page: Int): Int

    @Query("UPDATE documents SET isInRecycleBin = 1 WHERE id = :id")
    fun moveToRecycleBin(id: Long): Int

    @Query("UPDATE documents SET isInRecycleBin = 0 WHERE id = :id")
    fun restoreFromRecycleBin(id: Long): Int

    @Query("DELETE FROM documents WHERE id = :id")
    fun deletePermanently(id: Long): Int

    @Query("DELETE FROM documents WHERE isInRecycleBin = 1")
    fun emptyRecycleBin(): Int

    @Query("UPDATE documents SET title = :newTitle WHERE id = :id")
    fun renameDocument(id: Long, newTitle: String): Int

    @Query("SELECT COUNT(*) FROM documents WHERE isInRecycleBin = 0")
    fun getActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM documents WHERE isInRecycleBin = 1")
    fun getRecycleBinCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM documents WHERE isInRecycleBin = 0 AND fileType = :type")
    fun getCountByType(type: DocumentType): Flow<Int>

    @Query("SELECT COUNT(*) FROM documents WHERE isInRecycleBin = 0 AND lastOpenedDate > 0")
    fun getRecentCount(): Flow<Int>

    @Query("SELECT * FROM documents")
    fun getAllRawDocuments(): List<DocumentEntity>

    @Delete
    fun delete(document: DocumentEntity): Int

    @Query("DELETE FROM documents WHERE uri IN (:uris)")
    fun deleteByUris(uris: List<String>): Int

    @Query("DELETE FROM documents WHERE id IN (:ids)")
    fun deleteByIds(ids: List<Long>): Int
}
