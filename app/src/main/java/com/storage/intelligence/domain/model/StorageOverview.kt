package com.storage.intelligence.domain.model

data class StorageCategoryBreakdown(
    val category: StorageCategory,
    val totalSizeBytes: Long,
    val fileCount: Int
)

data class StorageOverview(
    val totalDeviceSizeBytes: Long,
    val usedDeviceSizeBytes: Long,
    val freeDeviceSizeBytes: Long,
    val usedPercentage: Int,
    val recoverableSummary: RecoverableSpace,
    val categoryBreakdowns: List<StorageCategoryBreakdown>,
    val insights: List<StorageInsight>,
    val reconciliation: StorageReconciliation? = null,
    val otherBreakdown: OtherStorageBreakdown? = null,
    val growthSummary: StorageGrowthReport? = null,
    val messagingSummary: MessagingStorageReport? = null
)

data class RecoverableSpace(
    val totalRecoverableBytes: Long,
    val duplicateVideosBytes: Long,
    val duplicatePhotosBytes: Long,
    val oldDownloadsBytes: Long,
    val screenshotsBytes: Long,
    val otherRecoverableBytes: Long
)

data class StorageInsight(
    val id: String,
    val title: String,
    val description: String,
    val potentialSavingsBytes: Long,
    val iconType: InsightIconType
)

enum class InsightIconType {
    GROWTH_TREND,
    DUPLICATE_FOUND,
    DOWNLOADS_WARNING,
    SCREENSHOTS_ALERT
}
