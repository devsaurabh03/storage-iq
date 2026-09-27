package com.storage.intelligence.domain.model

data class FileItem(
    val id: Long,
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val modifiedTimeMs: Long,
    val category: StorageCategory,
    val sha256Hash: String? = null,
    val perceptualHash: Long? = null,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0,
    val isScreenshot: Boolean = false,
    val isScreenRecording: Boolean = false,
    val isDownload: Boolean = false,
    val sourceApp: String? = null, // e.g. WhatsApp, Telegram
    val isSelectedForDeletion: Boolean = false,
    val isDuplicate: Boolean = false,
    val duplicateCopyCount: Int = 0,
    val isOptimized: Boolean = false,
    val originalSizeBytes: Long = 0L,
    val compressedFilePath: String? = null,
    val compressionAlgorithm: String? = null
)
