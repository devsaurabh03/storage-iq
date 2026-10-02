package com.storage.intelligence.feature.performance.repository

import android.os.Environment
import android.os.StatFs
import com.storage.intelligence.core.database.dao.PerformanceSnapshotDao
import com.storage.intelligence.core.database.entity.PerformanceSnapshotEntity
import com.storage.intelligence.feature.performance.analyzer.CpuBehaviorEngine
import com.storage.intelligence.feature.performance.analyzer.DiagnosticResult
import com.storage.intelligence.feature.performance.analyzer.PerformanceDiagnosticEngine
import com.storage.intelligence.feature.performance.analyzer.PerformanceRecommendationEngine
import com.storage.intelligence.feature.performance.analyzer.RecommendationItem
import com.storage.intelligence.feature.performance.model.*
import com.storage.intelligence.feature.performance.provider.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceRepository @Inject constructor(
    private val cpuInfoProvider: CpuInfoProvider,
    private val ramInfoProvider: RamInfoProvider,
    private val thermalInfoProvider: ThermalInfoProvider,
    private val batteryInfoProvider: BatteryInfoProvider,
    private val backgroundActivityProvider: BackgroundActivityProvider,
    private val snapshotDao: PerformanceSnapshotDao,
    private val diagnosticEngine: PerformanceDiagnosticEngine,
    private val behaviorEngine: CpuBehaviorEngine,
    private val recommendationEngine: PerformanceRecommendationEngine
) {

    suspend fun getLatestPerformanceSnapshot(): PerformanceSnapshot = withContext(Dispatchers.IO) {
        val ram = ramInfoProvider.getRamInfo()
        val cpuActivity = cpuInfoProvider.getCpuActivity()
        val thermal = thermalInfoProvider.getThermalInfo(cpuActivity.currentLoadPercent)
        val battery = batteryInfoProvider.getBatteryInfo()
        val backgroundApps = backgroundActivityProvider.getBackgroundApps()

        val (storageUsedBytes, storageTotalBytes) = getStorageStats()

        val snapshot = PerformanceSnapshot(
            timestampMs = System.currentTimeMillis(),
            ramInfo = ram,
            cpuActivity = cpuActivity,
            thermalInfo = thermal,
            batteryInfo = battery,
            storageUsedBytes = storageUsedBytes,
            storageTotalBytes = storageTotalBytes,
            activeAppCount = backgroundApps.size
        )

        // Save lightweight snapshot to DB for historical performance charts
        saveSnapshotToDb(snapshot)

        snapshot
    }

    fun getPerformanceSnapshotFlow(): Flow<PerformanceSnapshot> = flow {
        while (true) {
            emit(getLatestPerformanceSnapshot())
            kotlinx.coroutines.delay(5000) // 5s refresh while active on performance screen
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getCpuIdentity(): CpuIdentity {
        return cpuInfoProvider.getCpuIdentity()
    }

    suspend fun getActiveAppMemoryList(): List<AppMemoryInfo> {
        return ramInfoProvider.getActiveAppMemoryList()
    }

    suspend fun getBackgroundApps(): List<BackgroundAppItem> {
        return backgroundActivityProvider.getBackgroundApps()
    }

    suspend fun diagnoseWhyPhoneIsSlow(): DiagnosticResult = withContext(Dispatchers.IO) {
        val snapshot = getLatestPerformanceSnapshot()
        val apps = getBackgroundApps().map { it.appName }
        diagnosticEngine.diagnoseWhyPhoneIsSlow(snapshot, apps)
    }

    suspend fun diagnoseWhyPhoneIsHot(): DiagnosticResult = withContext(Dispatchers.IO) {
        val snapshot = getLatestPerformanceSnapshot()
        val apps = getBackgroundApps().map { it.appName }
        diagnosticEngine.diagnoseWhyPhoneIsHot(snapshot, apps)
    }

    suspend fun getRecommendations(): List<RecommendationItem> = withContext(Dispatchers.IO) {
        val snapshot = getLatestPerformanceSnapshot()
        val slowDiag = diagnoseWhyPhoneIsSlow()
        recommendationEngine.generateRecommendations(snapshot, slowDiag.findings)
    }

    fun getHistoricalSnapshotsFlow(limit: Int = 50): Flow<List<PerformanceSnapshotEntity>> {
        return snapshotDao.getRecentSnapshotsFlow(limit)
    }

    private suspend fun saveSnapshotToDb(snapshot: PerformanceSnapshot) = withContext(Dispatchers.IO) {
        try {
            val entity = PerformanceSnapshotEntity(
                timestampMs = snapshot.timestampMs,
                ramPressure = snapshot.ramInfo.pressure.name,
                totalRamBytes = snapshot.ramInfo.totalBytes,
                usedRamBytes = snapshot.ramInfo.usedBytes,
                availableRamBytes = snapshot.ramInfo.availableBytes,
                cpuLoadPercent = snapshot.cpuActivity.currentLoadPercent,
                cpuFrequencyGhz = 2.4,
                thermalState = snapshot.thermalInfo.state.name,
                batteryLevel = snapshot.batteryInfo.levelPercent,
                batteryTempCelsius = snapshot.batteryInfo.tempCelsius,
                isCharging = snapshot.batteryInfo.isCharging,
                storageUsedBytes = snapshot.storageUsedBytes,
                storageTotalBytes = snapshot.storageTotalBytes,
                backgroundActiveAppCount = snapshot.activeAppCount
            )
            snapshotDao.insertSnapshot(entity)
        } catch (e: Exception) {
            com.storage.intelligence.core.logging.AppLogger.e(
                "PerformanceRepository",
                "Non-critical background performance snapshot insert failed: ${e.message}",
                e
            )
        }
    }

    private fun getStorageStats(): Pair<Long, Long> {
        return try {
            val path = Environment.getDataDirectory().path
            val stat = StatFs(path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            Pair(usedBytes, totalBytes)
        } catch (e: Exception) {
            com.storage.intelligence.core.logging.AppLogger.w(
                "PerformanceRepository",
                "StatFs failed to query data directory stats (${e.message}), using fallback estimates."
            )
            Pair(100 * 1024 * 1024 * 1024L, 128 * 1024 * 1024 * 1024L)
        }
    }
}
