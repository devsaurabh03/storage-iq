package com.storage.intelligence

import com.storage.intelligence.domain.model.CategoryGrowth
import com.storage.intelligence.domain.model.GrowthRateProjection
import com.storage.intelligence.domain.model.StorageGrowthReport
import com.storage.intelligence.domain.model.StorageSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageGrowthCalculatorTest {

    @Test
    fun `growth calculation correctly ranks top contributors by growth amount`() {
        val oldSnapshot = StorageSnapshot(
            id = 1L,
            timestampMs = 1000L,
            totalUsedBytes = 50L * 1024 * 1024 * 1024,
            photosBytes = 10L * 1024 * 1024 * 1024,
            videosBytes = 20L * 1024 * 1024 * 1024,
            downloadsBytes = 5L * 1024 * 1024 * 1024,
            whatsappBytes = 5L * 1024 * 1024 * 1024,
            telegramBytes = 2L * 1024 * 1024 * 1024,
            screenshotsBytes = 1L * 1024 * 1024 * 1024,
            otherBytes = 7L * 1024 * 1024 * 1024
        )

        val newSnapshot = StorageSnapshot(
            id = 2L,
            timestampMs = 1000L + (14L * 24 * 60 * 60 * 1000), // 14 days later
            totalUsedBytes = 58L * 1024 * 1024 * 1024,
            photosBytes = 11L * 1024 * 1024 * 1024, // +1 GB
            videosBytes = 24L * 1024 * 1024 * 1024, // +4 GB
            downloadsBytes = 5L * 1024 * 1024 * 1024, // 0 GB
            whatsappBytes = 7L * 1024 * 1024 * 1024, // +2 GB
            telegramBytes = 3L * 1024 * 1024 * 1024, // +1 GB
            screenshotsBytes = 1L * 1024 * 1024 * 1024, // 0 GB
            otherBytes = 7L * 1024 * 1024 * 1024 // 0 GB
        )

        val netGrowth = newSnapshot.totalUsedBytes - oldSnapshot.totalUsedBytes
        assertEquals(8L * 1024 * 1024 * 1024, netGrowth)

        val categories = listOf(
            CategoryGrowth("Videos", newSnapshot.videosBytes - oldSnapshot.videosBytes, 50.0, newSnapshot.videosBytes, oldSnapshot.videosBytes),
            CategoryGrowth("WhatsApp", newSnapshot.whatsappBytes - oldSnapshot.whatsappBytes, 25.0, newSnapshot.whatsappBytes, oldSnapshot.whatsappBytes),
            CategoryGrowth("Photos", newSnapshot.photosBytes - oldSnapshot.photosBytes, 12.5, newSnapshot.photosBytes, oldSnapshot.photosBytes),
            CategoryGrowth("Telegram", newSnapshot.telegramBytes - oldSnapshot.telegramBytes, 12.5, newSnapshot.telegramBytes, oldSnapshot.telegramBytes)
        ).sortedByDescending { it.growthBytes }

        assertEquals("Videos", categories[0].categoryName)
        assertEquals(4L * 1024 * 1024 * 1024, categories[0].growthBytes)
        assertEquals("WhatsApp", categories[1].categoryName)
        assertEquals(2L * 1024 * 1024 * 1024, categories[1].growthBytes)
    }

    @Test
    fun `daily growth rate and projected days until full calculation`() {
        val netGrowthBytes = 14L * 1024 * 1024 * 1024 // 14 GB
        val elapsedDays = 14.0
        val growthPerDay = (netGrowthBytes / elapsedDays).toLong() // 1 GB / day
        val freeBytes = 10L * 1024 * 1024 * 1024 // 10 GB free

        val daysUntilFull = (freeBytes / growthPerDay).toInt()

        val projection = GrowthRateProjection(
            growthBytesPerDay = growthPerDay,
            growthBytesPerWeek = growthPerDay * 7,
            growthBytesPerMonth = growthPerDay * 30,
            projectedDaysUntilFull = daysUntilFull,
            hasSufficientHistory = true,
            trackingDaysCount = 14
        )

        assertEquals(1L * 1024 * 1024 * 1024, projection.growthBytesPerDay)
        assertEquals(7L * 1024 * 1024 * 1024, projection.growthBytesPerWeek)
        assertEquals(10, projection.projectedDaysUntilFull)
        assertTrue(projection.hasSufficientHistory)
    }

    @Test
    fun `insufficient history handles single snapshot gracefully`() {
        val snapshots = listOf(
            StorageSnapshot(1L, System.currentTimeMillis(), 100L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)
        )

        val report = StorageGrowthReport(
            timeframeDays = 30,
            netGrowthBytes = 0L,
            netGrowthPercentage = 0.0,
            rankedCategoryGrowths = emptyList(),
            projection = GrowthRateProjection(0L, 0L, 0L, null, false, 1),
            insights = listOf("Not enough history yet. StorageIQ has been tracking your storage for 1 day."),
            hasSufficientHistory = false,
            earliestSnapshotTimestampMs = snapshots.first().timestampMs
        )

        assertFalse(report.hasSufficientHistory)
        assertTrue(report.rankedCategoryGrowths.isEmpty())
        assertTrue(report.insights.first().contains("Not enough history"))
    }
}
