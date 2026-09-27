package com.storage.intelligence.domain.model

enum class ConfidenceLevel(val label: String) {
    EXACT("Exact measurement"),
    HIGH_CONFIDENCE("High confidence"),
    ESTIMATED("Estimated based on known patterns"),
    UNAVAILABLE("Android platform restricted")
}

data class StorageExplanation(
    val title: String,
    val description: String,
    val estimatedBytes: Long?,
    val confidence: ConfidenceLevel,
    val isDirectlyAccessible: Boolean
)

data class StorageReconciliation(
    val systemReportedUsedBytes: Long,
    val accountedBytes: Long,
    val unaccountedBytes: Long,
    val confidence: ConfidenceLevel,
    val explanations: List<StorageExplanation>
)

data class ExplainableStorageItem(
    val categoryName: String,
    val sizeBytes: Long,
    val confidence: ConfidenceLevel,
    val description: String
)

data class OtherStorageBreakdown(
    val totalUnaccountedBytes: Long,
    val explainableBytes: Long,
    val unavailableBytes: Long,
    val explainableItems: List<ExplainableStorageItem>,
    val inaccessibleSources: List<StorageExplanation>
)
