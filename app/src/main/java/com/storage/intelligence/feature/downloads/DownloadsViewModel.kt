package com.storage.intelligence.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.common.AgeThreshold
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.domain.model.FileItem
import com.storage.intelligence.domain.repository.StorageRepository
import com.storage.intelligence.domain.usecase.DeleteSelectedFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    private val deleteSelectedFilesUseCase: DeleteSelectedFilesUseCase
) : ViewModel() {

    private val _selectedAge = MutableStateFlow(AgeThreshold.OLDER_THAN_90_DAYS)
    val selectedAge: StateFlow<AgeThreshold> = _selectedAge.asStateFlow()

    val downloads: StateFlow<Result<List<FileItem>>> = _selectedAge
        .flatMapLatest { age ->
            val timestamp = System.currentTimeMillis() - (age.minDays.toLong() * 24 * 60 * 60 * 1000)
            storageRepository.getOldDownloadsFlow(timestamp)
        }
        .map <List<FileItem>, Result<List<FileItem>>> { Result.Success(it) }
        .catch { emit(Result.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading)

    private val _selectedFiles = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFiles: StateFlow<Set<Long>> = _selectedFiles.asStateFlow()

    fun setAgeThreshold(age: AgeThreshold) {
        _selectedAge.value = age
    }

    fun toggleSelection(fileId: Long, isSelected: Boolean) {
        val current = _selectedFiles.value.toMutableSet()
        if (isSelected) current.add(fileId) else current.remove(fileId)
        _selectedFiles.value = current
    }

    fun deleteSelected(allFiles: List<FileItem>) {
        viewModelScope.launch {
            val toDelete = allFiles.filter { _selectedFiles.value.contains(it.id) }
            if (toDelete.isNotEmpty()) {
                deleteSelectedFilesUseCase(toDelete)
                _selectedFiles.value = emptySet()
            }
        }
    }
}
