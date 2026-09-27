package com.storage.intelligence.feature.performance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.feature.performance.analyzer.DiagnosticResult
import com.storage.intelligence.feature.performance.repository.PerformanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsUiState(
    val isLoading: Boolean = true,
    val slowDiagnostic: DiagnosticResult? = null,
    val hotDiagnostic: DiagnosticResult? = null
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val performanceRepository: PerformanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        runDiagnostics()
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val slow = performanceRepository.diagnoseWhyPhoneIsSlow()
            val hot = performanceRepository.diagnoseWhyPhoneIsHot()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    slowDiagnostic = slow,
                    hotDiagnostic = hot
                )
            }
        }
    }
}
