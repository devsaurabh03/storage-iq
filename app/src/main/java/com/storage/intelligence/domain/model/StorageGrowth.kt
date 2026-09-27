package com.storage.intelligence.domain.model

data class CategoryGrowth(
    val categoryName: String,
    val growthBytes: Long,
    val growthPercentage: Double,
    val currentBytes: Long,
    val previousBytes: Long,
    val iconKey: String? = null
)

data class GrowthRateProjection(
    val growthBytesPerDay: Long,
    val growthBytesPerWeek: Long,
    val growthBytesPerMonth: Long,
    val projectedDaysUntilFull: Int?,
    val hasSufficientHistory: Boolean,
    val trackingDaysCount: Int
)

data class StorageGrowthReport(
    val timeframeDays: Int,
    val netGrowthBytes: Long,
    val netGrowthPercentage: Double,
    val rankedCategoryGrowths: List<CategoryGrowth>,
    val projection: GrowthRateProjection,
    val insights: List<String>,
    val hasSufficientHistory: Boolean,
    val earliestSnapshotTimestampMs: Long?
)
