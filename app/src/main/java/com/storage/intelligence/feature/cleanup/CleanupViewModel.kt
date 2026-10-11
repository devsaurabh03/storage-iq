package com.storage.intelligence.feature.cleanup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.domain.model.StorageOverview
import com.storage.intelligence.domain.usecase.GetStorageOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CleanupViewModel @Inject constructor(
    getStorageOverviewUseCase: GetStorageOverviewUseCase
) : ViewModel() {

    val uiState: StateFlow<Result<StorageOverview>> = getStorageOverviewUseCase()
        .map <StorageOverview, Result<StorageOverview>> { Result.Success(it) }
        .catch { error ->
            com.storage.intelligence.core.logging.AppLogger.e("CleanupViewModel", "Failed to load storage overview: ${error.message}", error)
            emit(Result.Error(error))
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, Result.Loading)
}
