package com.storage.intelligence.feature.performance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.feature.performance.analyzer.CpuBehaviorEngine
import com.storage.intelligence.feature.performance.analyzer.CpuHealthTestRunner
import com.storage.intelligence.feature.performance.analyzer.TestProgressState
import com.storage.intelligence.feature.performance.model.CpuActivity
import com.storage.intelligence.feature.performance.model.CpuIdentity
import com.storage.intelligence.feature.performance.model.ThermalInfo
import com.storage.intelligence.feature.performance.repository.PerformanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CpuIntelligenceUiState(
    val isLoading: Boolean = true,
    val identity: CpuIdentity? = null,
    val activity: CpuActivity? = null,
    val thermalInfo: ThermalInfo? = null,
    val testProgress: TestProgressState? = null,
    val isTestRunning: Boolean = false
)

@HiltViewModel
class CpuIntelligenceViewModel @Inject constructor(
    private val performanceRepository: PerformanceRepository,
    private val healthTestRunner: CpuHealthTestRunner
) : ViewModel() {

    private val _uiState = MutableStateFlow(CpuIntelligenceUiState())
    val uiState: StateFlow<CpuIntelligenceUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val identity = performanceRepository.getCpuIdentity()
            val snapshot = performanceRepository.getLatestPerformanceSnapshot()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    identity = identity,
                    activity = snapshot.cpuActivity,
                    thermalInfo = snapshot.thermalInfo
                )
            }
        }
    }

    fun startHealthTest() {
        if (_uiState.value.isTestRunning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTestRunning = true) }
            healthTestRunner.runCpuHealthTest(20).collect { progress ->
                _uiState.update {
                    it.copy(
                        testProgress = progress,
                        isTestRunning = !progress.isComplete
                    )
                }
            }
        }
    }
}
