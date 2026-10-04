package com.storage.intelligence.domain.model

/**
 * Type of correlation established between media in WhatsApp's Sent and Received collections.
 *
 * Adheres strictly to StorageIQ semantic rules:
 * Never claims "Forwarded = true"; explicitly describes content and temporal correlation.
 */
enum class CorrelationType(
    val title: String,
    val description: String
) {
    EXACT_MATCH(
        title = "Exact Match",
        description = "Same media exists in Sent & Received."
    ),
    STRONG_TEMPORAL(
        title = "Seen in Received First",
        description = "Same media was seen in Received before Sent."
    ),
    VISUAL_SIMILARITY(
        title = "Visual Match",
        description = "Similar media exists in Sent & Received."
    )
}

/**
 * A correlated pair of media representing a file present in both Received and Sent collections.
 */
data class SentReceivedCorrelationGroup(
    val id: String,
    val sentFile: FileItem,
    val receivedFile: FileItem,
    val correlationType: CorrelationType,
    val reclaimableBytes: Long = sentFile.sizeBytes
)

/**
 * Summary metrics for WhatsApp Sent & Received intelligence.
 */
data class WhatsAppSentReceivedSummary(
    val totalCorrelatedPairs: Int = 0,
    val totalCorrelatedBytes: Long = 0L,
    val exactMatchCount: Int = 0,
    val similarMatchCount: Int = 0,
    val reclaimableBytes: Long = 0L
)
