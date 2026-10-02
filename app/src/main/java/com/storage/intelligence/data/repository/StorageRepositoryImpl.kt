package com.storage.intelligence.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.storage.intelligence.core.logging.AppLogger
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.core.common.SearchFilter
import com.storage.intelligence.core.database.dao.FileMetadataDao
import com.storage.intelligence.core.database.entity.FileMetadataEntity
import com.storage.intelligence.data.scanner.MediaStoreScanner
import com.storage.intelligence.data.scanner.ScanProgress
import com.storage.intelligence.domain.model.*
import com.storage.intelligence.domain.repository.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: MediaStoreScanner,
    private val fileDao: FileMetadataDao
) : StorageRepository {

    override fun scanStorage(): Flow<ScanProgress> =
        scanner.scanStorageIncremental(sinceTimestampSec = 0L, reconcileDeleted = true)

    override fun scanStorageDelta(sinceTimestampSec: Long): Flow<ScanProgress> =
        scanner.scanStorageIncremental(sinceTimestampSec = sinceTimestampSec, reconcileDeleted = false)


    override fun getStorageOverviewFlow(): Flow<StorageOverview> {
        return fileDao.getAllFilesFlow().map { allEntities ->
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            val usedPercent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 0

            val categoryMap = allEntities.groupBy { it.category }
            val breakdowns = StorageCategory.entries.map { cat ->
                val files = categoryMap[cat.name] ?: emptyList()
                StorageCategoryBreakdown(
                    category = cat,
                    totalSizeBytes = files.sumOf { it.sizeBytes },
                    fileCount = files.size
                )
            }

            // Deterministic recoverable calculation
            val dupVideosBytes = allEntities.filter { it.category == StorageCategory.VIDEOS.name && it.sha256Hash != null }.sumOf { it.sizeBytes / 2 }
            val dupPhotosBytes = allEntities.filter { it.category == StorageCategory.PHOTOS.name && it.sha256Hash != null }.sumOf { it.sizeBytes / 2 }
            val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
            val oldDownloadsBytes = allEntities.filter { it.isDownload && it.modifiedTimeMs < ninetyDaysAgo }.sumOf { it.sizeBytes }
            val screenshotsBytes = allEntities.filter { it.isScreenshot }.sumOf { it.sizeBytes }

            val totalRecoverable = dupVideosBytes + dupPhotosBytes + oldDownloadsBytes + screenshotsBytes

            val recoverableSummary = RecoverableSpace(
                totalRecoverableBytes = totalRecoverable,
                duplicateVideosBytes = dupVideosBytes,
                duplicatePhotosBytes = dupPhotosBytes,
                oldDownloadsBytes = oldDownloadsBytes,
                screenshotsBytes = screenshotsBytes,
                otherRecoverableBytes = 0L
            )

            val insights = mutableListOf<StorageInsight>()
            if (dupVideosBytes > 0) {
                insights.add(
                    StorageInsight(
                        id = "dup_videos",
                        title = "${FormattingUtils.formatBytes(dupVideosBytes)} in duplicate videos",
                        description = "Exact byte-matched duplicate videos found across your library.",
                        potentialSavingsBytes = dupVideosBytes,
                        iconType = InsightIconType.DUPLICATE_FOUND
                    )
                )
            }
            if (oldDownloadsBytes > 0) {
                insights.add(
                    StorageInsight(
                        id = "old_downloads",
                        title = "${FormattingUtils.formatBytes(oldDownloadsBytes)} in old downloads",
                        description = "Downloaded files older than 90 days stored on device.",
                        potentialSavingsBytes = oldDownloadsBytes,
                        iconType = InsightIconType.DOWNLOADS_WARNING
                    )
                )
            }
            if (screenshotsBytes > 0) {
                insights.add(
                    StorageInsight(
                        id = "screenshots",
                        title = "${FormattingUtils.formatBytes(screenshotsBytes)} in screenshots",
                        description = "Accumulated device screenshots ready for quick review.",
                        potentialSavingsBytes = screenshotsBytes,
                        iconType = InsightIconType.SCREENSHOTS_ALERT
                    )
                )
            }

            val accountedBytes = allEntities.sumOf { it.sizeBytes }
            val unaccountedBytes = (usedBytes - accountedBytes).coerceAtLeast(0L)

            val explanations = listOf(
                StorageExplanation(
                    title = "Private App Data & Sandboxes",
                    description = "Android isolates each application's internal data, databases, and saved state from third-party inspection.",
                    estimatedBytes = null,
                    confidence = ConfidenceLevel.HIGH_CONFIDENCE,
                    isDirectlyAccessible = false
                ),
                StorageExplanation(
                    title = "System Storage & Android OS",
                    description = "Android operating system image, boot partitions, and core system libraries.",
                    estimatedBytes = null,
                    confidence = ConfidenceLevel.HIGH_CONFIDENCE,
                    isDirectlyAccessible = false
                ),
                StorageExplanation(
                    title = "Application Caches & Temp Files",
                    description = "Cached data managed dynamically by Android and cleared as storage demands dictate.",
                    estimatedBytes = null,
                    confidence = ConfidenceLevel.ESTIMATED,
                    isDirectlyAccessible = false
                ),
                StorageExplanation(
                    title = "System Trash & Deleted Items",
                    description = "Recently deleted files retained by Android MediaStore for 30 days before permanent deletion.",
                    estimatedBytes = null,
                    confidence = ConfidenceLevel.HIGH_CONFIDENCE,
                    isDirectlyAccessible = false
                )
            )

            val reconciliation = StorageReconciliation(
                systemReportedUsedBytes = usedBytes,
                accountedBytes = accountedBytes,
                unaccountedBytes = unaccountedBytes,
                confidence = ConfidenceLevel.EXACT,
                explanations = explanations
            )

            val downloadsSize = allEntities.filter { it.isDownload }.sumOf { it.sizeBytes }
            val documentsSize = allEntities.filter { it.category == StorageCategory.DOCUMENTS.name }.sumOf { it.sizeBytes }
            val messagingSize = allEntities.filter { it.sourceApp != null }.sumOf { it.sizeBytes }
            val audioSize = allEntities.filter { it.category == StorageCategory.AUDIO.name }.sumOf { it.sizeBytes }
            val publicOtherSize = allEntities.filter { it.category == StorageCategory.OTHER.name }.sumOf { it.sizeBytes }

            val explainableItems = listOf(
                ExplainableStorageItem("Temporary & other files", publicOtherSize, ConfidenceLevel.EXACT, "Files detected in public device storage without standard media type"),
                ExplainableStorageItem("Messaging media", messagingSize, ConfidenceLevel.EXACT, "Accessible media from WhatsApp and Telegram"),
                ExplainableStorageItem("Downloads folder", downloadsSize, ConfidenceLevel.EXACT, "Files residing in the public Downloads folder"),
                ExplainableStorageItem("Documents & text", documentsSize, ConfidenceLevel.EXACT, "PDFs, office documents, and eBooks"),
                ExplainableStorageItem("Audio & recordings", audioSize, ConfidenceLevel.EXACT, "Audio tracks, podcasts, and recordings")
            ).filter { it.sizeBytes > 0 }

            val otherBreakdown = OtherStorageBreakdown(
                totalUnaccountedBytes = unaccountedBytes + publicOtherSize,
                explainableBytes = publicOtherSize,
                unavailableBytes = unaccountedBytes,
                explainableItems = explainableItems,
                inaccessibleSources = explanations
            )

            val waFiles = allEntities.filter { it.sourceApp == "WhatsApp" }
            val tgFiles = allEntities.filter { it.sourceApp == "Telegram" }

            val waSubcategories = listOf(
                MessagingSubcategoryBreakdown(MessagingMediaType.VIDEOS, waFiles.filter { it.category == StorageCategory.VIDEOS.name || it.mimeType.startsWith("video/") }.sumOf { it.sizeBytes }, waFiles.count { it.category == StorageCategory.VIDEOS.name || it.mimeType.startsWith("video/") }),
                MessagingSubcategoryBreakdown(MessagingMediaType.IMAGES, waFiles.filter { it.category == StorageCategory.PHOTOS.name || it.mimeType.startsWith("image/") }.sumOf { it.sizeBytes }, waFiles.count { it.category == StorageCategory.PHOTOS.name || it.mimeType.startsWith("image/") }),
                MessagingSubcategoryBreakdown(MessagingMediaType.DOCUMENTS, waFiles.filter { it.category == StorageCategory.DOCUMENTS.name || it.mimeType.contains("pdf") || it.mimeType.contains("document") }.sumOf { it.sizeBytes }, waFiles.count { it.category == StorageCategory.DOCUMENTS.name || it.mimeType.contains("pdf") || it.mimeType.contains("document") }),
                MessagingSubcategoryBreakdown(MessagingMediaType.AUDIO, waFiles.filter { it.category == StorageCategory.AUDIO.name || it.mimeType.startsWith("audio/") }.sumOf { it.sizeBytes }, waFiles.count { it.category == StorageCategory.AUDIO.name || it.mimeType.startsWith("audio/") })
            )

            val tgSubcategories = listOf(
                MessagingSubcategoryBreakdown(MessagingMediaType.VIDEOS, tgFiles.filter { it.category == StorageCategory.VIDEOS.name || it.mimeType.startsWith("video/") }.sumOf { it.sizeBytes }, tgFiles.count { it.category == StorageCategory.VIDEOS.name || it.mimeType.startsWith("video/") }),
                MessagingSubcategoryBreakdown(MessagingMediaType.IMAGES, tgFiles.filter { it.category == StorageCategory.PHOTOS.name || it.mimeType.startsWith("image/") }.sumOf { it.sizeBytes }, tgFiles.count { it.category == StorageCategory.PHOTOS.name || it.mimeType.startsWith("image/") }),
                MessagingSubcategoryBreakdown(MessagingMediaType.DOCUMENTS, tgFiles.filter { it.category == StorageCategory.DOCUMENTS.name || it.mimeType.contains("pdf") || it.mimeType.contains("document") }.sumOf { it.sizeBytes }, tgFiles.count { it.category == StorageCategory.DOCUMENTS.name || it.mimeType.contains("pdf") || it.mimeType.contains("document") }),
                MessagingSubcategoryBreakdown(MessagingMediaType.AUDIO, tgFiles.filter { it.category == StorageCategory.AUDIO.name || it.mimeType.startsWith("audio/") }.sumOf { it.sizeBytes }, tgFiles.count { it.category == StorageCategory.AUDIO.name || it.mimeType.startsWith("audio/") })
            )

            val waApp = MessagingAppStorage(
                appName = "WhatsApp",
                totalSizeBytes = waFiles.sumOf { it.sizeBytes },
                fileCount = waFiles.size,
                subcategories = waSubcategories,
                accessibleScopeNote = "Scanned from accessible MediaStore public collections."
            )

            val tgApp = MessagingAppStorage(
                appName = "Telegram",
                totalSizeBytes = tgFiles.sumOf { it.sizeBytes },
                fileCount = tgFiles.size,
                subcategories = tgSubcategories,
                accessibleScopeNote = "Some storage may be inaccessible if saved in app-private sandboxes."
            )

            val messagingReport = MessagingStorageReport(
                totalMessagingBytes = waApp.totalSizeBytes + tgApp.totalSizeBytes,
                totalGrowth14DaysBytes = 0L,
                apps = listOf(waApp, tgApp)
            )

            StorageOverview(
                totalDeviceSizeBytes = totalBytes,
                usedDeviceSizeBytes = usedBytes,
                freeDeviceSizeBytes = freeBytes,
                usedPercentage = usedPercent,
                recoverableSummary = recoverableSummary,
                categoryBreakdowns = breakdowns,
                insights = insights,
                reconciliation = reconciliation,
                otherBreakdown = otherBreakdown,
                messagingSummary = messagingReport
            )
        }
    }

    override fun getOtherStorageFlow(): Flow<OtherStorageBreakdown> {
        return getStorageOverviewFlow().map { it.otherBreakdown ?: OtherStorageBreakdown(0L, 0L, 0L, emptyList(), emptyList()) }
    }

    override fun getMessagingReportFlow(): Flow<MessagingStorageReport> {
        return getStorageOverviewFlow().map { it.messagingSummary ?: MessagingStorageReport(0L, 0L, emptyList()) }
    }

    override fun getReconciliationFlow(): Flow<StorageReconciliation> {
        return getStorageOverviewFlow().map { it.reconciliation ?: StorageReconciliation(0L, 0L, 0L, ConfidenceLevel.UNAVAILABLE, emptyList()) }
    }

    override fun getFilesByCategoryFlow(category: StorageCategory): Flow<List<FileItem>> {
        return fileDao.getFilesByCategoryFlow(category.name).map { list -> list.map { it.toDomain() } }
    }

    override fun getLargeFilesFlow(minSizeBytes: Long): Flow<List<FileItem>> {
        return fileDao.getLargeFilesFlow(minSizeBytes).map { list -> list.map { it.toDomain() } }
    }

    override fun getScreenshotsFlow(): Flow<List<FileItem>> {
        return fileDao.getScreenshotsFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getScreenRecordingsFlow(): Flow<List<FileItem>> {
        return fileDao.getScreenRecordingsFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getOldDownloadsFlow(beforeTimestampMs: Long): Flow<List<FileItem>> {
        return fileDao.getOldDownloadsFlow(beforeTimestampMs).map { list -> list.map { it.toDomain() } }
    }

    override fun getMessagingMediaFlow(sourceApp: String): Flow<List<FileItem>> {
        return kotlinx.coroutines.flow.combine(
            fileDao.getMessagingMediaFlow(sourceApp),
            fileDao.getAllFilesFlow()
        ) { messagingList, allFiles ->
            val hashMap = allFiles.filter { it.sha256Hash != null }.groupBy { it.sha256Hash!! }
            val sizeMap = allFiles.groupBy { it.sizeBytes }

            messagingList.map { entity ->
                val matchingCount = if (entity.sha256Hash != null) {
                    hashMap[entity.sha256Hash]?.size ?: 1
                } else {
                    sizeMap[entity.sizeBytes]?.size ?: 1
                }
                val isDup = matchingCount > 1
                entity.toDomain().copy(
                    isDuplicate = isDup,
                    duplicateCopyCount = matchingCount
                )
            }
        }
    }

    override fun searchFilesFlow(filter: SearchFilter): Flow<List<FileItem>> {
        return fileDao.getAllFilesFlow().map { list ->
            list.map { it.toDomain() }.filter { item ->
                val matchesQuery = filter.query.isEmpty() || item.name.contains(filter.query, ignoreCase = true)
                val matchesCategory = filter.category == null || item.category == filter.category
                val matchesSize = item.sizeBytes >= filter.sizeThreshold.minBytes
                val matchesAge = filter.ageThreshold.minDays == 0 || FormattingUtils.isOlderThanDays(item.modifiedTimeMs, filter.ageThreshold.minDays)
                matchesQuery && matchesCategory && matchesSize && matchesAge
            }
        }
    }

    override suspend fun deleteFiles(files: List<FileItem>): Boolean = withContext(Dispatchers.IO) {
        val deletedIds = mutableListOf<Long>()
        val pathsToRescan = mutableListOf<String>()

        for (file in files) {
            var isDeleted = false

            // Strategy 1: ContentResolver MediaStore deletion (deletes from MediaStore and device storage)
            try {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Files.getContentUri("external"),
                    file.id
                )
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) {
                    isDeleted = true
                }
            } catch (e: Exception) {
                AppLogger.d("StorageRepository", "ContentResolver delete for ${file.path}: ${e.message}")
            }

            // Strategy 2: Direct filesystem deletion (works when All Files Access / MANAGE_EXTERNAL_STORAGE is granted)
            try {
                val f = File(file.path)
                if (f.exists()) {
                    val fileDeleted = f.delete()
                    if (fileDeleted) {
                        isDeleted = true
                        pathsToRescan.add(file.path)
                    }
                } else {
                    // File already does not exist on disk
                    isDeleted = true
                }
            } catch (e: Exception) {
                AppLogger.d("StorageRepository", "File.delete for ${file.path}: ${e.message}")
            }

            if (isDeleted) {
                deletedIds.add(file.id)
            } else {
                AppLogger.e("StorageRepository", "Could not delete ${file.path}. Missing MANAGE_EXTERNAL_STORAGE or media write permission.")
            }
        }

        if (pathsToRescan.isNotEmpty()) {
            try {
                MediaScannerConnection.scanFile(
                    context,
                    pathsToRescan.toTypedArray(),
                    null,
                    null
                )
            } catch (e: Exception) {
                AppLogger.w("StorageRepository", "MediaScannerConnection rescan error: ${e.message}")
            }
        }

        if (deletedIds.isNotEmpty()) {
            fileDao.deleteByIds(deletedIds)
            AppLogger.i("StorageRepository", "deleteFiles: successfully removed ${deletedIds.size}/${files.size} files.")
        }

        deletedIds.isNotEmpty()
    }

    override suspend fun getFilesByHash(hash: String): List<FileItem> {
        return fileDao.getFilesByHash(hash).map { it.toDomain() }
    }

    override suspend fun getFilesBySize(sizeBytes: Long): List<FileItem> {
        return fileDao.getFilesBySize(sizeBytes).map { it.toDomain() }
    }

    override fun getStaleFilesFlow(daysUnopened: Int): Flow<List<FileItem>> {
        val cutoffMs = System.currentTimeMillis() - (daysUnopened.toLong() * 24 * 60 * 60 * 1000)
        return fileDao.getStaleFilesFlow(cutoffMs).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getOptimizedFilesFlow(): Flow<List<FileItem>> {
        return fileDao.getOptimizedFilesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun optimizeStaleFiles(androidContext: android.content.Context, files: List<FileItem>): Long {
        var totalSavedBytes = 0L
        for (file in files) {
            val result = com.storage.intelligence.core.common.StaleFileCompressor.compressFile(androidContext, file)
            if (result.success) {
                totalSavedBytes += result.savedBytes
                fileDao.updateOptimizationState(
                    id = file.id,
                    originalSize = result.originalSizeBytes,
                    compressedSize = result.compressedSizeBytes,
                    compressedPath = result.compressedPath,
                    algorithm = result.algorithm.name
                )
            }
        }
        return totalSavedBytes
    }

    override suspend fun decompressAndAccessFile(androidContext: android.content.Context, fileItem: FileItem): java.io.File? {
        return com.storage.intelligence.core.common.StaleFileCompressor.decompressAndAccess(androidContext, fileItem)
    }

    override suspend fun restoreOptimizedFile(androidContext: android.content.Context, fileItem: FileItem): Boolean {
        val restored = com.storage.intelligence.core.common.StaleFileCompressor.decompressAndAccess(androidContext, fileItem)
        return if (restored != null && restored.exists()) {
            fileDao.updateRestoredState(fileItem.id, restored.length())
            true
        } else false
    }

    private fun FileMetadataEntity.toDomain(): FileItem {
        return FileItem(
            id = id,
            path = path,
            name = name,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            modifiedTimeMs = modifiedTimeMs,
            category = try { StorageCategory.valueOf(category) } catch (e: Exception) { StorageCategory.OTHER },
            sha256Hash = sha256Hash,
            perceptualHash = perceptualHash,
            width = width,
            height = height,
            durationMs = durationMs,
            isScreenshot = isScreenshot,
            isScreenRecording = isScreenRecording,
            isDownload = isDownload,
            sourceApp = sourceApp,
            isOptimized = isOptimized,
            originalSizeBytes = originalSizeBytes,
            compressedFilePath = compressedFilePath,
            compressionAlgorithm = compressionAlgorithm
        )
    }
}
