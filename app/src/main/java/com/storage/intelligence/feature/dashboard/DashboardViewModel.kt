package com.storage.intelligence.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.common.Result
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
    cpuMonitoringEngine: CpuMonitoringEngine
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgressText = MutableStateFlow<String?>(null)
    val scanProgressText: StateFlow<String?> = _scanProgressText.asStateFlow()

    val cpuSnapshot: StateFlow<CpuSnapshot?> = cpuMonitoringEngine.startMonitoring()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<Result<DashboardUiState>> = combine(
        getStorageOverviewUseCase(),
        getStorageGrowthUseCase(30),
        _isScanning,
        _scanProgressText
    ) { overview, growth, scanning, progressText ->
        DashboardUiState(
            overview = overview,
            growthReport = growth,
            isScanning = scanning,
            scanProgressText = progressText
        )
    }
        .map<DashboardUiState, Result<DashboardUiState>> { Result.Success(it) }
        .catch { error ->
            com.storage.intelligence.core.logging.AppLogger.e("DashboardViewModel", "Dashboard stream failure: ${error.message}", error)
            emit(Result.Error(error))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading)

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
     * Room DB is always live via Flow so the UI never shows a blank/loading state
     * at launch — it renders cached data immediately while the background scan runs.
     */
    private suspend fun smartScanOnLaunch() {
        val isDeltaStale = preferencesRepository.isDeltaScanStale()
        val isFullStale  = preferencesRepository.isFullScanStale()

        when {
            !isDeltaStale -> {
                com.storage.intelligence.core.logging.AppLogger.i("DashboardViewModel",
                    "Scan skipped — data is fresh (last scan < 15 min ago).")
            }
            !isFullStale -> {
                val lastScanTimeSec = preferencesRepository.lastScanTimestamp.first() / 1000L
                com.storage.intelligence.core.logging.AppLogger.i("DashboardViewModel",
                    "Running delta scan since $lastScanTimeSec...")
                runScan(delta = true, sinceTimestampSec = lastScanTimeSec)
                preferencesRepository.updateLastScanTimestamp(System.currentTimeMillis())
            }
            else -> {
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
