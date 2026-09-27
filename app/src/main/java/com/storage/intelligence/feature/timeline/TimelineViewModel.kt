package com.storage.intelligence.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.domain.model.StorageGrowthReport
import com.storage.intelligence.domain.model.StorageSnapshot
import com.storage.intelligence.domain.repository.TimelineRepository
import com.storage.intelligence.domain.usecase.GetStorageGrowthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class TimelineUiState(
    val selectedTimeframeDays: Int,
    val snapshots: List<StorageSnapshot>,
    val growthReport: StorageGrowthReport
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val timelineRepository: TimelineRepository,
    private val getStorageGrowthUseCase: GetStorageGrowthUseCase
) : ViewModel() {

    private val _selectedTimeframeDays = MutableStateFlow(30)
    val selectedTimeframeDays: StateFlow<Int> = _selectedTimeframeDays.asStateFlow()

    val uiState: StateFlow<Result<TimelineUiState>> = _selectedTimeframeDays
        .flatMapLatest { days ->
            combine(
                timelineRepository.getSnapshotsInRangeFlow(days),
                getStorageGrowthUseCase(days)
            ) { snapshots, growthReport ->
                TimelineUiState(
                    selectedTimeframeDays = days,
                    snapshots = snapshots,
                    growthReport = growthReport
                )
            }
        }
        .map <TimelineUiState, Result<TimelineUiState>> { Result.Success(it) }
        .catch { emit(Result.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading)

    fun selectTimeframe(days: Int) {
        _selectedTimeframeDays.value = days
    }
}
