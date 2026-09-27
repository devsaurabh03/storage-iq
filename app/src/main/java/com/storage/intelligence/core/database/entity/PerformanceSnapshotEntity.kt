package com.storage.intelligence.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "performance_snapshots")
data class PerformanceSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val ramPressure: String,
    val totalRamBytes: Long,
    val usedRamBytes: Long,
    val availableRamBytes: Long,
    val cpuLoadPercent: Int,
    val cpuFrequencyGhz: Double,
    val thermalState: String,
    val batteryLevel: Int,
    val batteryTempCelsius: Double,
    val isCharging: Boolean,
    val storageUsedBytes: Long,
    val storageTotalBytes: Long,
    val backgroundActiveAppCount: Int
)
