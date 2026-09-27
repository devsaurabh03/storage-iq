package com.storage.intelligence.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storage_snapshots")
data class StorageSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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

@Entity(tableName = "duplicate_groups")
data class DuplicateGroupEntity(
    @PrimaryKey val groupId: String,
    val sha256Hash: String,
    val totalSizeBytes: Long,
    val recoverableSizeBytes: Long,
    val fileIdsJson: String, // Comma separated IDs or JSON string
    val preservedFileId: Long?
)
