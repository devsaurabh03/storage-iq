package com.storage.intelligence.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.common.Result
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
        .catch { emit(Result.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading)

    init {
        // Trigger non-blocking incremental check on launch
        rescanStorage()
    }

    fun rescanStorage() {
        viewModelScope.launch {
            _isScanning.value = true
            storageRepository.scanStorage().collect { progress ->
                _scanProgressText.value = progress.currentPhase
                if (progress.isComplete) {
                    _isScanning.value = false
                    _scanProgressText.value = null
                }
            }
        }
    }
}
