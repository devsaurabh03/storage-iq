package com.storage.intelligence.data.repository

import android.graphics.BitmapFactory
import com.storage.intelligence.core.logging.AppLogger
import com.storage.intelligence.core.database.dao.DuplicateGroupDao
import com.storage.intelligence.core.database.dao.FileMetadataDao
import com.storage.intelligence.core.database.entity.DuplicateGroupEntity
import com.storage.intelligence.core.database.entity.FileMetadataEntity
import com.storage.intelligence.data.scanner.FileHasher
import com.storage.intelligence.domain.model.DuplicateGroup
import com.storage.intelligence.domain.model.FileItem
import com.storage.intelligence.domain.model.SimilarPhotoGroup
import com.storage.intelligence.domain.model.StorageCategory
import com.storage.intelligence.domain.repository.DuplicateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicateRepositoryImpl @Inject constructor(
    private val fileDao: FileMetadataDao,
    private val duplicateGroupDao: DuplicateGroupDao,
    private val fileHasher: FileHasher
) : DuplicateRepository {

    override fun getExactDuplicateGroupsFlow(): Flow<List<DuplicateGroup>> = flow {
        runExactDuplicateDetection()
        val allFiles = StorageCategory.entries.flatMap { fileDao.getFilesByCategory(it.name) }

        val groups = allFiles.filter { it.sha256Hash != null }
            .groupBy { it.sha256Hash!! }
            .filter { it.value.size > 1 }
            .map { (hash, entities) ->
                val sorted = entities.sortedByDescending { it.modifiedTimeMs }
                val preserved = sorted.first()
                val duplicates = sorted.drop(1)
                val totalSize = entities.sumOf { it.sizeBytes }
                val recoverableSize = duplicates.sumOf { it.sizeBytes }

                DuplicateGroup(
                    groupId = hash,
                    sha256Hash = hash,
                    totalSizeBytes = totalSize,
                    recoverableSizeBytes = recoverableSize,
                    files = sorted.map { it.toDomain(isSelected = it.id != preserved.id) },
                    preservedFileId = preserved.id
                )
            }
        emit(groups)
    }.flowOn(Dispatchers.IO)

    override fun getDuplicateVideosFlow(): Flow<List<DuplicateGroup>> = flow {
        runExactDuplicateDetection()
        val videoFiles = fileDao.getFilesByCategory(StorageCategory.VIDEOS.name)
        val groups = videoFiles.filter { it.sha256Hash != null }
            .groupBy { it.sha256Hash!! }
            .filter { it.value.size > 1 }
            .map { (hash, entities) ->
                val sorted = entities.sortedByDescending { it.modifiedTimeMs }
                val preserved = sorted.first()
                val duplicates = sorted.drop(1)

                DuplicateGroup(
                    groupId = hash,
                    sha256Hash = hash,
                    totalSizeBytes = entities.sumOf { it.sizeBytes },
                    recoverableSizeBytes = duplicates.sumOf { it.sizeBytes },
                    files = sorted.map { it.toDomain(isSelected = it.id != preserved.id) },
                    preservedFileId = preserved.id
                )
            }
        emit(groups)
    }.flowOn(Dispatchers.IO)

    override fun getSimilarPhotoGroupsFlow(): Flow<List<SimilarPhotoGroup>> = flow {
        runSimilarPhotoDetection()
        val photoFiles = fileDao.getFilesByCategory(StorageCategory.PHOTOS.name).filter { it.perceptualHash != null }
        val similarGroups = mutableListOf<SimilarPhotoGroup>()

        val processedIds = mutableSetOf<Long>()
        for (i in photoFiles.indices) {
            val photoA = photoFiles[i]
            if (processedIds.contains(photoA.id)) continue

            val cluster = mutableListOf(photoA)
            for (j in i + 1 until photoFiles.size) {
                val photoB = photoFiles[j]
                if (processedIds.contains(photoB.id)) continue

                val distance = fileHasher.calculateHammingDistance(photoA.perceptualHash!!, photoB.perceptualHash!!)
                if (distance <= 10) {
                    cluster.add(photoB)
                }
            }

            if (cluster.size > 1) {
                cluster.forEach { processedIds.add(it.id) }
                // Rank best candidate: highest resolution (width * height)
                val sortedByRank = cluster.sortedByDescending { it.width * it.height }
                val best = sortedByRank.first()

                similarGroups.add(
                    SimilarPhotoGroup(
                        groupId = "similar_${best.id}",
                        recommendedKeepFileId = best.id,
                        reasonForKeepRecommendation = "Highest resolution (${best.width}x${best.height})",
                        photos = cluster.map { it.toDomain(isSelected = false) }
                    )
                )
            }
        }
        emit(similarGroups)
    }.flowOn(Dispatchers.IO)

    override suspend fun runExactDuplicateDetection() = withContext(Dispatchers.IO) {
        val candidates = fileDao.getDuplicateCandidateSizes()
        for (candidate in candidates) {
            val matchingFiles = fileDao.getFilesBySize(candidate.sizeBytes)
            val updated = mutableListOf<FileMetadataEntity>()
            for (fileEntity in matchingFiles) {
                if (fileEntity.sha256Hash == null) {
                    val hash = fileHasher.computeSha256(File(fileEntity.path), fileEntity.id)
                    if (hash != null) {
                        updated.add(fileEntity.copy(sha256Hash = hash))
                    }
                }
            }
            if (updated.isNotEmpty()) {
                fileDao.insertOrUpdateAll(updated)
            }
        }
    }

    override suspend fun runSimilarPhotoDetection() = withContext(Dispatchers.IO) {
        val unhashedPhotos = fileDao.getFilesByCategory(StorageCategory.PHOTOS.name).filter { it.perceptualHash == null }
        val updated = mutableListOf<FileMetadataEntity>()
        val options = BitmapFactory.Options().apply { inSampleSize = 4 }

        for (photo in unhashedPhotos) {
            val file = File(photo.path)
            if (file.exists() && file.canRead()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(photo.path, options)
                    if (bitmap != null) {
                        val dHash = fileHasher.computeDHash(bitmap)
                        bitmap.recycle()
                        updated.add(photo.copy(perceptualHash = dHash))
                    }
                } catch (e: Exception) {
                    AppLogger.e("DuplicateRepository", "Corrupted or unreadable photo at ${photo.path}: ${e.message}", e)
                }
            }
            if (updated.size >= 50) {
                fileDao.insertOrUpdateAll(updated)
                updated.clear()
            }
        }
        if (updated.isNotEmpty()) {
            fileDao.insertOrUpdateAll(updated)
        }
    }

    private fun FileMetadataEntity.toDomain(isSelected: Boolean): FileItem {
        return FileItem(
            id = id,
            path = path,
            name = name,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            modifiedTimeMs = modifiedTimeMs,
            category = StorageCategory.valueOf(category),
            sha256Hash = sha256Hash,
            perceptualHash = perceptualHash,
            width = width,
            height = height,
            durationMs = durationMs,
            isScreenshot = isScreenshot,
            isScreenRecording = isScreenRecording,
            isDownload = isDownload,
            sourceApp = sourceApp,
            isSelectedForDeletion = isSelected
        )
    }
}
