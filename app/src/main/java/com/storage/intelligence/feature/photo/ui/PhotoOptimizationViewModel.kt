package com.storage.intelligence.feature.photo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.core.datastore.UserPreferencesRepository
import com.storage.intelligence.domain.model.FileItem
import com.storage.intelligence.domain.model.StorageCategory
import com.storage.intelligence.domain.repository.StorageRepository
import com.storage.intelligence.feature.photo.engine.PhotoOptimizationEngine
import com.storage.intelligence.feature.photo.model.PhotoOptimizationCandidate
import com.storage.intelligence.feature.photo.model.PhotoOptimizationConfig
import com.storage.intelligence.feature.photo.model.PhotoOptimizationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotoOptimizationUiState(
    val candidates: List<PhotoOptimizationCandidate> = emptyList(),
    val isEvaluating: Boolean = true,
    val isOptimizing: Boolean = false,
    val currentOptimizingIndex: Int = 0,
    val totalToOptimize: Int = 0,
    val totalEstimatedSavingsBytes: Long = 0L,
    val totalSavedSoFarBytes: Long = 0L,
    val lastResult: PhotoOptimizationResult? = null
)

@HiltViewModel
class PhotoOptimizationViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    private val photoEngine: PhotoOptimizationEngine,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoOptimizationUiState())
    val uiState: StateFlow<PhotoOptimizationUiState> = _uiState.asStateFlow()

    init {
        loadPhotoCandidates()
    }

    fun loadPhotoCandidates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEvaluating = true)
            storageRepository.getFilesByCategoryFlow(StorageCategory.PHOTOS)
                .catch { _uiState.value = _uiState.value.copy(isEvaluating = false) }
                .collect { photoList ->
                    val evaluated = photoList.map { photoEngine.evaluateCandidate(it) }
                    val worthOptimizing = evaluated.filter { it.isWorthOptimizing }
                    val totalSavings = worthOptimizing.sumOf { it.estimatedSavingsBytes }

                    _uiState.value = _uiState.value.copy(
                        candidates = evaluated,
                        isEvaluating = false,
                        totalEstimatedSavingsBytes = totalSavings
                    )
                }
        }
    }

    fun toggleCandidateSelection(fileId: Long, isSelected: Boolean) {
        val updated = _uiState.value.candidates.map {
            if (it.fileItem.id == fileId) it.copy(isSelected = isSelected) else it
        }
        val totalSavings = updated.filter { it.isSelected }.sumOf { it.estimatedSavingsBytes }
        _uiState.value = _uiState.value.copy(candidates = updated, totalEstimatedSavingsBytes = totalSavings)
    }

    fun selectAll(select: Boolean) {
        val updated = _uiState.value.candidates.map {
            if (it.isWorthOptimizing) it.copy(isSelected = select) else it
        }
        val totalSavings = updated.filter { it.isSelected }.sumOf { it.estimatedSavingsBytes }
        _uiState.value = _uiState.value.copy(candidates = updated, totalEstimatedSavingsBytes = totalSavings)
    }

    fun optimizeSelected(deleteOriginals: Boolean, onComplete: (Long, Int, String?) -> Unit) {
        val selected = _uiState.value.candidates.filter { it.isSelected }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isOptimizing = true,
                currentOptimizingIndex = 0,
                totalToOptimize = selected.size,
                totalSavedSoFarBytes = 0L
            )

            var savedTotal = 0L
            var successCount = 0
            var lastErrorMessage: String? = null
            val filesToDelete = mutableListOf<FileItem>()

            selected.forEachIndexed { index, candidate ->
                _uiState.value = _uiState.value.copy(currentOptimizingIndex = index + 1)
                val result = photoEngine.optimizePhoto(candidate.fileItem, PhotoOptimizationConfig())
                if (result.success) {
                    savedTotal += result.savedBytes
                    successCount++
                    preferencesRepository.recordPhotoOptimized(result.savedBytes)
                    if (deleteOriginals) {
                        filesToDelete.add(candidate.fileItem)
                    }
                } else {
                    lastErrorMessage = result.errorMessage
                }
                _uiState.value = _uiState.value.copy(
                    totalSavedSoFarBytes = savedTotal,
                    lastResult = result
                )
            }

            if (deleteOriginals && filesToDelete.isNotEmpty()) {
                storageRepository.deleteFiles(filesToDelete)
            }

            _uiState.value = _uiState.value.copy(isOptimizing = false)
            onComplete(savedTotal, successCount, lastErrorMessage)
            loadPhotoCandidates()
        }
    }
}
