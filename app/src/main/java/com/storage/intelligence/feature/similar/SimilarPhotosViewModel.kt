package com.storage.intelligence.feature.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.domain.model.SimilarPhotoGroup
import com.storage.intelligence.domain.usecase.DeleteSelectedFilesUseCase
import com.storage.intelligence.domain.usecase.DetectSimilarPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SimilarPhotosViewModel @Inject constructor(
    detectSimilarPhotosUseCase: DetectSimilarPhotosUseCase,
    private val deleteSelectedFilesUseCase: DeleteSelectedFilesUseCase,
    val permissionManager: com.storage.intelligence.core.permissions.PermissionManager
) : ViewModel() {

    fun hasAllFilesAccess(): Boolean = permissionManager.hasAllFilesAccess()

    fun requestAllFilesAccess(context: android.content.Context) {
        permissionManager.requestAllFilesAccess(context)
    }

    val similarGroups: StateFlow<Result<List<SimilarPhotoGroup>>> = detectSimilarPhotosUseCase()
        .map <List<SimilarPhotoGroup>, Result<List<SimilarPhotoGroup>>> { Result.Success(it) }
        .catch { emit(Result.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading)

    private val _selectedFiles = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFiles: StateFlow<Set<Long>> = _selectedFiles.asStateFlow()

    fun toggleSelection(photoId: Long, isSelected: Boolean) {
        val current = _selectedFiles.value.toMutableSet()
        if (isSelected) current.add(photoId) else current.remove(photoId)
        _selectedFiles.value = current
    }

    fun deleteSelected(allGroups: List<SimilarPhotoGroup>) {
        viewModelScope.launch {
            val toDelete = allGroups.flatMap { g -> g.photos.filter { _selectedFiles.value.contains(it.id) } }
            if (toDelete.isNotEmpty()) {
                deleteSelectedFilesUseCase(toDelete)
                _selectedFiles.value = emptySet()
            }
        }
    }
}
