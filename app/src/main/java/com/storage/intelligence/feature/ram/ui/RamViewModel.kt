package com.storage.intelligence.feature.ram.ui

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.feature.ram.analyzer.BackgroundActivityAnalyzer
import com.storage.intelligence.feature.ram.classifier.MemoryPressureClassifier
import com.storage.intelligence.feature.ram.engine.MemoryRecommendationEngine
import com.storage.intelligence.feature.ram.model.*
import com.storage.intelligence.feature.ram.repository.MemoryObservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class RamUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val snapshot: SystemMemorySnapshot? = null,
    val pressure: MemoryPressure = MemoryPressure.LOW,
    val backgroundSignals: List<AppProcessSignal> = emptyList(),
    val recommendation: MemoryRecommendation? = null,
    val recentObservations: List<MemoryObservation> = emptyList()
)

@HiltViewModel
class RamViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val classifier: MemoryPressureClassifier,
    private val backgroundAnalyzer: BackgroundActivityAnalyzer,
    private val recommendationEngine: MemoryRecommendationEngine,
    private val observationRepository: MemoryObservationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RamUiState())
    val uiState: StateFlow<RamUiState> = _uiState.asStateFlow()

    init {
        loadRamIntelligence()
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            observationRepository.getRecentObservationsFlow(20).collect { observations ->
                _uiState.update { it.copy(recentObservations = observations) }
            }
        }
    }

    fun loadRamIntelligence() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.snapshot == null) }
            refreshInternal()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refreshRamIntelligence() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            refreshInternal()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun refreshInternal() = withContext(Dispatchers.IO) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (activityManager == null) {
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            return@withContext
        }

        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val snapshot = SystemMemorySnapshot(
            totalBytes = memoryInfo.totalMem,
            availableBytes = memoryInfo.availMem,
            thresholdBytes = memoryInfo.threshold,
            lowMemory = memoryInfo.lowMemory,
            timestamp = System.currentTimeMillis()
        )

        val pressure = classifier.classify(snapshot)
        val signals = backgroundAnalyzer.getObservableBackgroundActivities()
        val rec = recommendationEngine.generateRecommendation(pressure, signals)

        observationRepository.recordObservation(snapshot, pressure)

        _uiState.update {
            it.copy(
                snapshot = snapshot,
                pressure = pressure,
                backgroundSignals = signals,
                recommendation = rec
            )
        }
    }
}
