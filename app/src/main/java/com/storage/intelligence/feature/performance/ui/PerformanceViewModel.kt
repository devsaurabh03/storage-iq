package com.storage.intelligence.feature.performance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.feature.performance.analyzer.RecommendationItem
import com.storage.intelligence.feature.performance.model.AppMemoryInfo
import com.storage.intelligence.feature.performance.model.CpuIdentity
import com.storage.intelligence.feature.performance.model.PerformanceSnapshot
import com.storage.intelligence.feature.performance.provider.BackgroundAppItem
import com.storage.intelligence.feature.performance.repository.PerformanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PerformanceUiState(
    val isLoading: Boolean = true,
    val snapshot: PerformanceSnapshot? = null,
    val cpuIdentity: CpuIdentity? = null,
    val appMemoryList: List<AppMemoryInfo> = emptyList(),
    val backgroundApps: List<BackgroundAppItem> = emptyList(),
    val recommendations: List<RecommendationItem> = emptyList(),
    val isScanning: Boolean = false
)

@HiltViewModel
class PerformanceViewModel @Inject constructor(
    private val performanceRepository: PerformanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerformanceUiState())
    val uiState: StateFlow<PerformanceUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val identity = performanceRepository.getCpuIdentity()
            val appMemory = performanceRepository.getActiveAppMemoryList()
            val bgApps = performanceRepository.getBackgroundApps()
            val snapshot = performanceRepository.getLatestPerformanceSnapshot()
            val recs = performanceRepository.getRecommendations()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    snapshot = snapshot,
                    cpuIdentity = identity,
                    appMemoryList = appMemory,
                    backgroundApps = bgApps,
                    recommendations = recs
                )
            }
        }
    }

    fun refreshMemoryAnalysis() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            kotlinx.coroutines.delay(600)
            loadData()
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    fun runQuickPerformanceCheck() {
        refreshMemoryAnalysis()
    }
}
