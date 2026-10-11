package com.storage.intelligence.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.core.database.dao.FileMetadataDao
import com.storage.intelligence.core.datastore.UserPreferencesRepository
import com.storage.intelligence.domain.model.StorageGrowthReport
import com.storage.intelligence.domain.model.StorageOverview
import com.storage.intelligence.domain.repository.StorageRepository
import com.storage.intelligence.domain.usecase.GetStorageGrowthUseCase
import com.storage.intelligence.domain.usecase.GetStorageOverviewUseCase
import com.storage.intelligence.feature.performance.engine.CpuMonitoringEngine
import com.storage.intelligence.feature.performance.engine.CpuSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.storage.intelligence.feature.performance.provider.BatteryInfoProvider
import com.storage.intelligence.feature.performance.provider.RamInfoProvider

import com.storage.intelligence.feature.ram.model.MemoryPressure

data class LiveSystemTelemetry(
    val batteryPercent: Int = 0,
    val tempCelsius: Double = 0.0,
    val isCharging: Boolean = false,
    val ramUsedBytes: Long = 0L,
    val ramTotalBytes: Long = 0L,
    val ramUsedPercent: Int = 0,
    val ramPressure: MemoryPressure = MemoryPressure.LOW
)

data class DashboardUiState(
    val overview: StorageOverview,
    val growthReport: StorageGrowthReport? = null,
    val isScanning: Boolean = false,
    val scanProgressText: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getStorageOverviewUseCase: GetStorageOverviewUseCase,
    getStorageGrowthUseCase: GetStorageGrowthUseCase,
    private val storageRepository: StorageRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val fileDao: FileMetadataDao,
    cpuMonitoringEngine: CpuMonitoringEngine,
    private val ramInfoProvider: RamInfoProvider,
    private val batteryInfoProvider: BatteryInfoProvider
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgressText = MutableStateFlow<String?>(null)
    val scanProgressText: StateFlow<String?> = _scanProgressText.asStateFlow()

    val cpuSnapshot: StateFlow<CpuSnapshot?> = cpuMonitoringEngine.startMonitoring()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val systemTelemetry: StateFlow<LiveSystemTelemetry> = flow {
        while (true) {
            val battery = try { batteryInfoProvider.getBatteryInfo() } catch (_: Exception) { null }
            val ram = try { ramInfoProvider.getRamInfo() } catch (_: Exception) { null }
            val usedPercent = if (ram != null && ram.totalBytes > 0L) {
                ((ram.usedBytes.toDouble() / ram.totalBytes) * 100).toInt()
            } else 0
            val pressure = when {
                ram == null -> MemoryPressure.LOW
                ram.isLowMemory || ram.availableBytes <= ram.thresholdBytes || usedPercent >= 92 -> MemoryPressure.CRITICAL
                ram.availableBytes <= (ram.thresholdBytes * 1.5).toLong() || usedPercent >= 83 -> MemoryPressure.HIGH
                ram.availableBytes <= (ram.thresholdBytes * 2.2).toLong() || usedPercent >= 70 -> MemoryPressure.MODERATE
                else -> MemoryPressure.LOW
            }
            emit(
                LiveSystemTelemetry(
                    batteryPercent = battery?.levelPercent ?: 0,
                    tempCelsius = battery?.tempCelsius ?: 0.0,
                    isCharging = battery?.isCharging ?: false,
                    ramUsedBytes = ram?.usedBytes ?: 0L,
                    ramTotalBytes = ram?.totalBytes ?: 0L,
                    ramUsedPercent = usedPercent,
                    ramPressure = pressure
                )
            )
            val pollingDelayMs = when (pressure) {
                MemoryPressure.CRITICAL, MemoryPressure.HIGH -> 2000L
                MemoryPressure.MODERATE -> 3000L
                MemoryPressure.LOW -> 5000L
            }
            kotlinx.coroutines.delay(pollingDelayMs)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LiveSystemTelemetry())

    /**
     * Emits Result.Loading on first launch until:
     *   (a) the DB has at least 1 file (scan is populating data), OR
     *   (b) the scan finishes (_isScanning flips false).
     *
     * On subsequent opens Room already has data so this resolves immediately.
     */
    val uiState: StateFlow<Result<DashboardUiState>> = combine(
        getStorageOverviewUseCase(),
        getStorageGrowthUseCase(30),
        _isScanning,
        _scanProgressText,
        fileDao.getTotalFileCountFlow()
    ) { overview, growth, scanning, progressText, fileCount ->
        // On first launch: DB is empty AND scan is still running → keep loading
        if (fileCount == 0 && scanning) {
            null // sentinel → mapped to Result.Loading below
        } else {
            DashboardUiState(
                overview = overview,
                growthReport = growth,
                isScanning = scanning,
                scanProgressText = progressText
            )
        }
    }
        .map<DashboardUiState?, Result<DashboardUiState>> { state ->
            if (state == null) Result.Loading else Result.Success(state)
        }
        .catch { error ->
            com.storage.intelligence.core.logging.AppLogger.e("DashboardViewModel", "Dashboard stream failure: ${error.message}", error)
            emit(Result.Error(error))
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, Result.Loading)

    init {
        // Smart scan on launch: Room DB serves cached data instantly via Flow,
        // then we decide whether to run a delta or full scan in the background.
        viewModelScope.launch { smartScanOnLaunch() }
    }

    /**
     * 3-tier smart scan policy:
     *
     * Tier 1 — Skip: last scan < 15 min ago → data is fresh, no scan needed.
     * Tier 2 — Delta: 15 min – 6 h since last scan → query only files modified
     *           since last scan timestamp. Typically 0–50 rows, finishes in < 1 s.
     * Tier 3 — Full: > 6 h since last full scan (or first ever launch) →
     *           full MediaStore traversal + directory walk + stale-row cleanup.
     *
     * Room DB is always live via Flow so the UI never shows stale zero-data.
     * On first launch, uiState holds Result.Loading until the DB is populated.
     */
    private suspend fun smartScanOnLaunch() {
        val isDeltaStale = preferencesRepository.isDeltaScanStale()
        val isFullStale  = preferencesRepository.isFullScanStale()

        when {
            !isDeltaStale -> {
                // Cache is fresh — nothing to do. UI already shows live Room data.
                com.storage.intelligence.core.logging.AppLogger.i("DashboardViewModel",
                    "Scan skipped — data is fresh (last scan < 15 min ago).")
            }
            !isFullStale -> {
                // Delta scan: only process files modified since last scan
                val lastScanTimeSec = preferencesRepository.lastScanTimestamp.first() / 1000L
                com.storage.intelligence.core.logging.AppLogger.i("DashboardViewModel",
                    "Running delta scan since $lastScanTimeSec...")
                runScan(delta = true, sinceTimestampSec = lastScanTimeSec)
                preferencesRepository.updateLastScanTimestamp(System.currentTimeMillis())
            }
            else -> {
                // Full scan: all MediaStore rows + directory walk + stale-row cleanup.
                // Also the path taken on very first launch when DB is empty.
                com.storage.intelligence.core.logging.AppLogger.i("DashboardViewModel",
                    "Running full scan (first launch or > 6 h since last full scan).")
                runScan(delta = false, sinceTimestampSec = 0L)
                preferencesRepository.markFullScanComplete()
            }
        }
    }

    /** Force a full rescan — called when user taps the refresh button. */
    fun rescanStorage() {
        viewModelScope.launch {
            runScan(delta = false, sinceTimestampSec = 0L)
            preferencesRepository.markFullScanComplete()
        }
    }

    private suspend fun runScan(delta: Boolean, sinceTimestampSec: Long) {
        try {
            _isScanning.value = true
            val scanFlow = if (delta) storageRepository.scanStorageDelta(sinceTimestampSec)
                           else       storageRepository.scanStorage()
            scanFlow.collect { progress ->
                _scanProgressText.value = progress.currentPhase
                if (progress.isComplete) {
                    _isScanning.value = false
                    _scanProgressText.value = null
                }
            }
        } catch (e: Exception) {
            com.storage.intelligence.core.logging.AppLogger.e("DashboardViewModel", "Scan error: ${e.message}", e)
            _isScanning.value = false
            _scanProgressText.value = null
        }
    }
}
