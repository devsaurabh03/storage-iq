package com.storage.intelligence.feature.other

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.domain.model.OtherStorageBreakdown
import com.storage.intelligence.domain.model.StorageReconciliation
import com.storage.intelligence.domain.usecase.GetOtherStorageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class OtherStorageUiState(
    val breakdown: OtherStorageBreakdown,
    val reconciliation: StorageReconciliation
)

@HiltViewModel
class OtherStorageViewModel @Inject constructor(
    getOtherStorageUseCase: GetOtherStorageUseCase
) : ViewModel() {

    val uiState: StateFlow<Result<OtherStorageUiState>> = combine(
        getOtherStorageUseCase.getOtherStorageBreakdown(),
        getOtherStorageUseCase.getReconciliation()
    ) { breakdown, reconciliation ->
        Result.Success(OtherStorageUiState(breakdown, reconciliation))
    }
        .catch <Result<OtherStorageUiState>> { emit(Result.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading)
}
