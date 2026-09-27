package com.storage.intelligence.domain.model

data class StorageSnapshot(
    val id: Long,
    val timestampMs: Long,
    val totalUsedBytes: Long,
    val photosBytes: Long,
    val videosBytes: Long,
    val downloadsBytes: Long,
    val whatsappBytes: Long,
    val telegramBytes: Long,
    val screenshotsBytes: Long,
    val otherBytes: Long,
    val totalDeviceBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val appsBytes: Long = 0L,
    val documentsBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val unaccountedBytes: Long = 0L,
    val whatsappVideosBytes: Long = 0L,
    val whatsappImagesBytes: Long = 0L,
    val whatsappDocsBytes: Long = 0L,
    val whatsappAudioBytes: Long = 0L,
    val telegramVideosBytes: Long = 0L,
    val telegramImagesBytes: Long = 0L,
    val telegramDocsBytes: Long = 0L,
    val telegramAudioBytes: Long = 0L
)
