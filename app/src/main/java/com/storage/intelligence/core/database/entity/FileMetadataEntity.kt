package com.storage.intelligence.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "file_metadata",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["sizeBytes"]),
        Index(value = ["category"]),
        Index(value = ["sha256Hash"]),
        Index(value = ["modifiedTimeMs"])
    ]
)
data class FileMetadataEntity(
    @PrimaryKey val id: Long,
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val modifiedTimeMs: Long,
    val category: String,
    val sha256Hash: String? = null,
    val perceptualHash: Long? = null,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0,
    val isScreenshot: Boolean = false,
    val isScreenRecording: Boolean = false,
    val isDownload: Boolean = false,
    val sourceApp: String? = null,
    val lastScannedMs: Long = System.currentTimeMillis(),
    val isOptimized: Boolean = false,
    val originalSizeBytes: Long = 0L,
    val compressedFilePath: String? = null,
    val compressionAlgorithm: String? = null
)
