package com.storage.intelligence.feature.ram.repository

import com.storage.intelligence.core.database.dao.PerformanceSnapshotDao
import com.storage.intelligence.core.database.entity.PerformanceSnapshotEntity
import com.storage.intelligence.feature.ram.model.MemoryObservation
import com.storage.intelligence.feature.ram.model.MemoryPressure
import com.storage.intelligence.feature.ram.model.SystemMemorySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages local persistence and trend observation for device RAM memory snapshots.
 */
@Singleton
class MemoryObservationRepository @Inject constructor(
    private val performanceSnapshotDao: PerformanceSnapshotDao
) {

    suspend fun recordObservation(snapshot: SystemMemorySnapshot, pressure: MemoryPressure) = withContext(Dispatchers.IO) {
        val entity = PerformanceSnapshotEntity(
            timestampMs = snapshot.timestamp,
            ramPressure = pressure.name,
            totalRamBytes = snapshot.totalBytes,
            usedRamBytes = snapshot.usedBytes,
            availableRamBytes = snapshot.availableBytes,
            cpuLoadPercent = 0,
            cpuFrequencyGhz = 0.0,
            thermalState = "NORMAL",
            batteryLevel = 0,
            batteryTempCelsius = 0.0,
            isCharging = false,
            storageUsedBytes = 0L,
            storageTotalBytes = 0L,
            backgroundActiveAppCount = 0
        )
        performanceSnapshotDao.insertSnapshot(entity)
    }

    fun getRecentObservationsFlow(limit: Int = 20): Flow<List<MemoryObservation>> {
        return performanceSnapshotDao.getRecentSnapshotsFlow(limit).map { entities ->
            entities.map { entity ->
                val pressure = try {
                    MemoryPressure.valueOf(entity.ramPressure)
                } catch (_: Exception) {
                    MemoryPressure.LOW
                }
                MemoryObservation(
                    id = entity.id,
                    timestamp = entity.timestampMs,
                    totalBytes = entity.totalRamBytes,
                    availableBytes = entity.availableRamBytes,
                    thresholdBytes = 0L,
                    lowMemory = pressure == MemoryPressure.CRITICAL,
                    pressure = pressure
                )
            }
        }
    }
}
