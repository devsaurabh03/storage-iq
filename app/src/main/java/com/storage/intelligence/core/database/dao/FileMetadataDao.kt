package com.storage.intelligence.core.database.dao

import androidx.room.*
import com.storage.intelligence.core.database.entity.FileMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(files: List<FileMetadataEntity>)

    @Query("SELECT * FROM file_metadata WHERE id = :id")
    suspend fun getById(id: Long): FileMetadataEntity?

    @Query("SELECT * FROM file_metadata WHERE path = :path")
    suspend fun getByPath(path: String): FileMetadataEntity?

    @Query("SELECT * FROM file_metadata ORDER BY sizeBytes DESC")
    fun getAllFilesFlow(): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE category = :category ORDER BY sizeBytes DESC")
    fun getFilesByCategoryFlow(category: String): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE category = :category ORDER BY sizeBytes DESC")
    suspend fun getFilesByCategory(category: String): List<FileMetadataEntity>

    @Query("SELECT * FROM file_metadata WHERE sizeBytes >= :minSizeBytes ORDER BY sizeBytes DESC")
    fun getLargeFilesFlow(minSizeBytes: Long): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE isScreenshot = 1 ORDER BY modifiedTimeMs DESC")
    fun getScreenshotsFlow(): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE isScreenRecording = 1 ORDER BY modifiedTimeMs DESC")
    fun getScreenRecordingsFlow(): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE isDownload = 1 AND modifiedTimeMs <= :beforeTimestampMs ORDER BY modifiedTimeMs ASC")
    fun getOldDownloadsFlow(beforeTimestampMs: Long): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE sourceApp = :sourceApp ORDER BY sizeBytes DESC")
    fun getMessagingMediaFlow(sourceApp: String): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE sourceApp = :sourceApp AND category = :category ORDER BY sizeBytes DESC")
    fun getMessagingMediaByCategoryFlow(sourceApp: String, category: String): Flow<List<FileMetadataEntity>>

    @Query("SELECT SUM(sizeBytes) FROM file_metadata WHERE sourceApp = :sourceApp AND category = :category")
    suspend fun getMessagingSizeByCategory(sourceApp: String, category: String): Long?

    @Query("SELECT COUNT(*) FROM file_metadata WHERE sourceApp = :sourceApp AND category = :category")
    suspend fun getMessagingCountByCategory(sourceApp: String, category: String): Int

    @Query("SELECT sizeBytes, COUNT(*) as count FROM file_metadata GROUP BY sizeBytes HAVING COUNT(*) > 1")
    suspend fun getDuplicateCandidateSizes(): List<DuplicateSizeCandidate>

    @Query("SELECT * FROM file_metadata WHERE sizeBytes = :sizeBytes")
    suspend fun getFilesBySize(sizeBytes: Long): List<FileMetadataEntity>

    @Query("SELECT * FROM file_metadata WHERE sha256Hash = :hash AND sha256Hash IS NOT NULL")
    suspend fun getFilesByHash(hash: String): List<FileMetadataEntity>

    @Query("SELECT id FROM file_metadata")
    suspend fun getAllIds(): List<Long>

    @Query("DELETE FROM file_metadata WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM file_metadata")
    suspend fun deleteAll()

    @Query("SELECT SUM(sizeBytes) FROM file_metadata WHERE category = :category")
    suspend fun getTotalSizeByCategory(category: String): Long?

    @Query("SELECT COUNT(*) FROM file_metadata")
    suspend fun getTotalFileCount(): Int

    @Query("SELECT COUNT(*) FROM file_metadata")
    fun getTotalFileCountFlow(): Flow<Int>

    @Query("SELECT * FROM file_metadata WHERE modifiedTimeMs <= :beforeTimestampMs AND isOptimized = 0 ORDER BY sizeBytes DESC")
    fun getStaleFilesFlow(beforeTimestampMs: Long): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE isOptimized = 1 ORDER BY modifiedTimeMs DESC")
    fun getOptimizedFilesFlow(): Flow<List<FileMetadataEntity>>

    @Query("UPDATE file_metadata SET isOptimized = 1, originalSizeBytes = :originalSize, sizeBytes = :compressedSize, compressedFilePath = :compressedPath, compressionAlgorithm = :algorithm WHERE id = :id")
    suspend fun updateOptimizationState(id: Long, originalSize: Long, compressedSize: Long, compressedPath: String, algorithm: String)

    @Query("UPDATE file_metadata SET isOptimized = 0, sizeBytes = :restoredSize, compressedFilePath = NULL, compressionAlgorithm = NULL WHERE id = :id")
    suspend fun updateRestoredState(id: Long, restoredSize: Long)

    @Query("SELECT id, path FROM file_metadata")
    suspend fun getAllIdPathPairs(): List<IdPathTuple>

    @Query("SELECT SUM(sizeBytes) FROM file_metadata WHERE category = :category")
    fun getTotalSizeByCategoryFlow(category: String): Flow<Long?>
}

data class IdPathTuple(
    val id: Long,
    val path: String
)

data class DuplicateSizeCandidate(
    val sizeBytes: Long,
    val count: Int
)
