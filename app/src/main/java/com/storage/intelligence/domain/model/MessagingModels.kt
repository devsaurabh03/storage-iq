package com.storage.intelligence.domain.model

enum class MessagingMediaType(val displayName: String) {
    VIDEOS("Videos"),
    IMAGES("Images"),
    DOCUMENTS("Documents"),
    AUDIO("Audio"),
    OTHER("Other")
}

data class MessagingSubcategoryBreakdown(
    val type: MessagingMediaType,
    val totalSizeBytes: Long,
    val fileCount: Int,
    val growth14DaysBytes: Long = 0L
)

data class MessagingAppStorage(
    val appName: String, // "WhatsApp" or "Telegram"
    val totalSizeBytes: Long,
    val fileCount: Int,
    val growth14DaysBytes: Long = 0L,
    val subcategories: List<MessagingSubcategoryBreakdown>,
    val accessibleScopeNote: String? = null
)

data class MessagingStorageReport(
    val totalMessagingBytes: Long,
    val totalGrowth14DaysBytes: Long,
    val apps: List<MessagingAppStorage>
)
