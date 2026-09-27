package com.storage.intelligence.data.repository

import android.os.Environment
import android.os.StatFs
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.core.database.dao.FileMetadataDao
import com.storage.intelligence.core.database.dao.SnapshotDao
import com.storage.intelligence.core.database.entity.StorageSnapshotEntity
import com.storage.intelligence.domain.model.*
import com.storage.intelligence.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimelineRepositoryImpl @Inject constructor(
    private val snapshotDao: SnapshotDao,
    private val fileDao: FileMetadataDao
) : TimelineRepository {

    override fun getRecentSnapshotsFlow(limitDays: Int): Flow<List<StorageSnapshot>> {
        return snapshotDao.getRecentSnapshotsFlow(limitDays).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSnapshotsInRangeFlow(timeframeDays: Int): Flow<List<StorageSnapshot>> {
        val now = System.currentTimeMillis()
        val start = now - (timeframeDays.toLong() * 24L * 60L * 60L * 1000L)
        return snapshotDao.getSnapshotsInRangeFlow(start, now).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEarliestTrackingTimestamp(): Long? {
        return snapshotDao.getOldestSnapshot()?.timestampMs
    }

    override fun getGrowthReportFlow(timeframeDays: Int): Flow<StorageGrowthReport> {
        val now = System.currentTimeMillis()
        val start = now - (timeframeDays.toLong() * 24L * 60L * 60L * 1000L)
        return snapshotDao.getSnapshotsInRangeFlow(start, now).map { list ->
            calculateGrowthReport(list, timeframeDays)
        }
    }

    override suspend fun recordCurrentSnapshot() {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

        val photos = fileDao.getTotalSizeByCategory(StorageCategory.PHOTOS.name) ?: 0L
        val videos = fileDao.getTotalSizeByCategory(StorageCategory.VIDEOS.name) ?: 0L
        val downloads = fileDao.getTotalSizeByCategory(StorageCategory.DOWNLOADS.name) ?: 0L
        val documents = fileDao.getTotalSizeByCategory(StorageCategory.DOCUMENTS.name) ?: 0L
        val audio = fileDao.getTotalSizeByCategory(StorageCategory.AUDIO.name) ?: 0L
        val screenshots = fileDao.getTotalSizeByCategory(StorageCategory.SCREENSHOTS.name) ?: 0L
        val whatsapp = fileDao.getTotalSizeByCategory(StorageCategory.WHATSAPP.name) ?: 0L
        val telegram = fileDao.getTotalSizeByCategory(StorageCategory.TELEGRAM.name) ?: 0L
        val other = fileDao.getTotalSizeByCategory(StorageCategory.OTHER.name) ?: 0L

        // WhatsApp subcategories
        val waVideos = fileDao.getMessagingSizeByCategory("WhatsApp", StorageCategory.VIDEOS.name) ?: 0L
        val waImages = fileDao.getMessagingSizeByCategory("WhatsApp", StorageCategory.PHOTOS.name) ?: 0L
        val waDocs = fileDao.getMessagingSizeByCategory("WhatsApp", StorageCategory.DOCUMENTS.name) ?: 0L
        val waAudio = fileDao.getMessagingSizeByCategory("WhatsApp", StorageCategory.AUDIO.name) ?: 0L

        // Telegram subcategories
        val tgVideos = fileDao.getMessagingSizeByCategory("Telegram", StorageCategory.VIDEOS.name) ?: 0L
        val tgImages = fileDao.getMessagingSizeByCategory("Telegram", StorageCategory.PHOTOS.name) ?: 0L
        val tgDocs = fileDao.getMessagingSizeByCategory("Telegram", StorageCategory.DOCUMENTS.name) ?: 0L
        val tgAudio = fileDao.getMessagingSizeByCategory("Telegram", StorageCategory.AUDIO.name) ?: 0L

        val accounted = photos + videos + downloads + documents + audio + screenshots + whatsapp + telegram + other
        val unaccounted = (usedBytes - accounted).coerceAtLeast(0L)

        val snapshot = StorageSnapshotEntity(
            timestampMs = System.currentTimeMillis(),
            totalUsedBytes = usedBytes,
            photosBytes = photos,
            videosBytes = videos,
            downloadsBytes = downloads,
            whatsappBytes = whatsapp,
            telegramBytes = telegram,
            screenshotsBytes = screenshots,
            otherBytes = other,
            totalDeviceBytes = totalBytes,
            freeBytes = freeBytes,
            appsBytes = 0L,
            documentsBytes = documents,
            audioBytes = audio,
            unaccountedBytes = unaccounted,
            whatsappVideosBytes = waVideos,
            whatsappImagesBytes = waImages,
            whatsappDocsBytes = waDocs,
            whatsappAudioBytes = waAudio,
            telegramVideosBytes = tgVideos,
            telegramImagesBytes = tgImages,
            telegramDocsBytes = tgDocs,
            telegramAudioBytes = tgAudio
        )
        snapshotDao.insertSnapshot(snapshot)
    }

    private fun calculateGrowthReport(
        snapshots: List<StorageSnapshotEntity>,
        timeframeDays: Int
    ): StorageGrowthReport {
        if (snapshots.size < 2) {
            val earliest = snapshots.firstOrNull()?.timestampMs
            val daysTracked = if (earliest != null) {
                ((System.currentTimeMillis() - earliest) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
            } else 0
            return StorageGrowthReport(
                timeframeDays = timeframeDays,
                netGrowthBytes = 0L,
                netGrowthPercentage = 0.0,
                rankedCategoryGrowths = emptyList(),
                projection = GrowthRateProjection(0L, 0L, 0L, null, false, daysTracked),
                insights = listOf("Not enough history yet. StorageIQ has been tracking your storage for $daysTracked day${if (daysTracked == 1) "" else "s"}."),
                hasSufficientHistory = false,
                earliestSnapshotTimestampMs = earliest
            )
        }

        val oldest = snapshots.first()
        val latest = snapshots.last()
        val elapsedMs = (latest.timestampMs - oldest.timestampMs).coerceAtLeast(1L)
        val elapsedDays = (elapsedMs / (24 * 60 * 60 * 1000.0)).coerceAtLeast(1.0)

        val netGrowth = latest.totalUsedBytes - oldest.totalUsedBytes
        val netGrowthPercent = if (oldest.totalUsedBytes > 0) {
            ((netGrowth.toDouble() / oldest.totalUsedBytes.toDouble()) * 100.0)
        } else 0.0

        val categoryGrowths = listOf(
            buildCategoryGrowth("Videos", latest.videosBytes, oldest.videosBytes, netGrowth),
            buildCategoryGrowth("WhatsApp", latest.whatsappBytes, oldest.whatsappBytes, netGrowth),
            buildCategoryGrowth("Photos", latest.photosBytes, oldest.photosBytes, netGrowth),
            buildCategoryGrowth("Downloads", latest.downloadsBytes, oldest.downloadsBytes, netGrowth),
            buildCategoryGrowth("Documents", latest.documentsBytes, oldest.documentsBytes, netGrowth),
            buildCategoryGrowth("Audio", latest.audioBytes, oldest.audioBytes, netGrowth),
            buildCategoryGrowth("Telegram", latest.telegramBytes, oldest.telegramBytes, netGrowth),
            buildCategoryGrowth("Screenshots", latest.screenshotsBytes, oldest.screenshotsBytes, netGrowth),
            buildCategoryGrowth("Other / Unaccounted", latest.otherBytes + latest.unaccountedBytes, oldest.otherBytes + oldest.unaccountedBytes, netGrowth)
        ).sortedByDescending { it.growthBytes }

        val growthPerDay = (netGrowth / elapsedDays).toLong()
        val growthPerWeek = (growthPerDay * 7)
        val growthPerMonth = (growthPerDay * 30)

        val daysUntilFull = if (growthPerDay > 0 && latest.freeBytes > 0) {
            (latest.freeBytes / growthPerDay).toInt()
        } else null

        val trackingDaysCount = elapsedDays.toInt()
        val projection = GrowthRateProjection(
            growthBytesPerDay = growthPerDay,
            growthBytesPerWeek = growthPerWeek,
            growthBytesPerMonth = growthPerMonth,
            projectedDaysUntilFull = daysUntilFull,
            hasSufficientHistory = trackingDaysCount >= 2,
            trackingDaysCount = trackingDaysCount
        )

        val insights = mutableListOf<String>()
        val topContributor = categoryGrowths.firstOrNull { it.growthBytes > 0 }
        if (topContributor != null && netGrowth > 0) {
            val pct = ((topContributor.growthBytes.toDouble() / netGrowth.toDouble()) * 100).toInt()
            insights.add("${topContributor.categoryName} is responsible for $pct% of your observed storage growth over the last $timeframeDays days.")
        }
        if (growthPerDay > 0) {
            insights.add("At the current observed rate of ${FormattingUtils.formatBytes(growthPerDay)}/day, available storage may become limited in approximately ${daysUntilFull ?: 30} days.")
        } else if (netGrowth < 0) {
            insights.add("Storage decreased by ${FormattingUtils.formatBytes(-netGrowth)} over the selected period.")
        }

        return StorageGrowthReport(
            timeframeDays = timeframeDays,
            netGrowthBytes = netGrowth,
            netGrowthPercentage = netGrowthPercent,
            rankedCategoryGrowths = categoryGrowths,
            projection = projection,
            insights = insights,
            hasSufficientHistory = true,
            earliestSnapshotTimestampMs = oldest.timestampMs
        )
    }

    private fun buildCategoryGrowth(name: String, current: Long, previous: Long, totalGrowth: Long): CategoryGrowth {
        val diff = current - previous
        val pct = if (totalGrowth > 0 && diff > 0) (diff.toDouble() / totalGrowth.toDouble()) * 100.0 else 0.0
        return CategoryGrowth(
            categoryName = name,
            growthBytes = diff,
            growthPercentage = pct,
            currentBytes = current,
            previousBytes = previous
        )
    }

    private fun StorageSnapshotEntity.toDomain(): StorageSnapshot {
        return StorageSnapshot(
            id = id,
            timestampMs = timestampMs,
            totalUsedBytes = totalUsedBytes,
            photosBytes = photosBytes,
            videosBytes = videosBytes,
            downloadsBytes = downloadsBytes,
            whatsappBytes = whatsappBytes,
            telegramBytes = telegramBytes,
            screenshotsBytes = screenshotsBytes,
            otherBytes = otherBytes,
            totalDeviceBytes = totalDeviceBytes,
            freeBytes = freeBytes,
            appsBytes = appsBytes,
            documentsBytes = documentsBytes,
            audioBytes = audioBytes,
            unaccountedBytes = unaccountedBytes,
            whatsappVideosBytes = whatsappVideosBytes,
            whatsappImagesBytes = whatsappImagesBytes,
            whatsappDocsBytes = whatsappDocsBytes,
            whatsappAudioBytes = whatsappAudioBytes,
            telegramVideosBytes = telegramVideosBytes,
            telegramImagesBytes = telegramImagesBytes,
            telegramDocsBytes = telegramDocsBytes,
            telegramAudioBytes = telegramAudioBytes
        )
    }
}
