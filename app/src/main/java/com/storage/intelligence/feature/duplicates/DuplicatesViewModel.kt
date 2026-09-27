package com.storage.intelligence.feature.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.domain.model.DuplicateGroup
import com.storage.intelligence.domain.model.FileItem
import com.storage.intelligence.domain.usecase.DeleteSelectedFilesUseCase
import com.storage.intelligence.domain.usecase.DetectDuplicatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    detectDuplicatesUseCase: DetectDuplicatesUseCase,
    private val deleteSelectedFilesUseCase: DeleteSelectedFilesUseCase,
    private val storageRepository: com.storage.intelligence.domain.repository.StorageRepository,
    private val duplicateRepository: com.storage.intelligence.domain.repository.DuplicateRepository
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Map of groupId -> preserved file ID
    private val _preservedFileOverrides = MutableStateFlow<Map<String, Long>>(emptyMap())

    val duplicateGroups: StateFlow<Result<List<DuplicateGroup>>> = combine(
        detectDuplicatesUseCase.getExactDuplicates(),
        _preservedFileOverrides
    ) { groups, overrides ->
        groups.map { group ->
            val overriddenPreservedId = overrides[group.groupId] ?: group.preservedFileId
            group.copy(
                preservedFileId = overriddenPreservedId,
                files = group.files.map { it.copy(isSelectedForDeletion = it.id != overriddenPreservedId) }
            )
        }
    }.map<List<DuplicateGroup>, Result<List<DuplicateGroup>>> { Result.Success(it) }
        .catch { emit(Result.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading)

    private val _selectedFiles = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFiles: StateFlow<Set<Long>> = _selectedFiles.asStateFlow()

    fun rescanStorage() {
        viewModelScope.launch {
            _isScanning.value = true
            storageRepository.scanStorage().collect { progress ->
                if (progress.isComplete) {
                    duplicateRepository.runExactDuplicateDetection()
                    _isScanning.value = false
                }
            }
        }
    }

    /**
     * SAFE SELECT ALL:
     * Selects all duplicate copies across all groups EXCEPT the preserved copy in each group.
     * Guarantees at least 1 copy is always preserved.
     */
    fun selectAllDuplicates(allGroups: List<DuplicateGroup>, keepNewest: Boolean = true) {
        val selected = mutableSetOf<Long>()
        val overrides = _preservedFileOverrides.value.toMutableMap()

        for (group in allGroups) {
            val preserved = if (keepNewest) {
                group.files.maxByOrNull { it.modifiedTimeMs } ?: group.files.first()
            } else {
                group.files.minByOrNull { it.modifiedTimeMs } ?: group.files.first()
            }
            overrides[group.groupId] = preserved.id

            for (file in group.files) {
                if (file.id != preserved.id) {
                    selected.add(file.id)
                }
            }
        }

        _preservedFileOverrides.value = overrides
        _selectedFiles.value = selected
    }

    fun deselectAllDuplicates() {
        _selectedFiles.value = emptySet()
    }

    fun toggleFileSelection(fileId: Long, isSelected: Boolean) {
        val current = _selectedFiles.value.toMutableSet()
        if (isSelected) current.add(fileId) else current.remove(fileId)
        _selectedFiles.value = current
    }

    fun setGroupPreservedFile(groupId: String, fileId: Long) {
        val overrides = _preservedFileOverrides.value.toMutableMap()
        overrides[groupId] = fileId
        _preservedFileOverrides.value = overrides

        // If the newly preserved file was selected for deletion, unselect it immediately
        val current = _selectedFiles.value.toMutableSet()
        current.remove(fileId)
        _selectedFiles.value = current
    }

    fun selectGroupAllExceptNewest(group: DuplicateGroup) {
        val newest = group.files.maxByOrNull { it.modifiedTimeMs } ?: return
        setGroupPreservedFile(group.groupId, newest.id)
        val current = _selectedFiles.value.toMutableSet()
        group.files.forEach { file ->
            if (file.id != newest.id) current.add(file.id) else current.remove(file.id)
        }
        _selectedFiles.value = current
    }

    fun selectGroupAllExceptOldest(group: DuplicateGroup) {
        val oldest = group.files.minByOrNull { it.modifiedTimeMs } ?: return
        setGroupPreservedFile(group.groupId, oldest.id)
        val current = _selectedFiles.value.toMutableSet()
        group.files.forEach { file ->
            if (file.id != oldest.id) current.add(file.id) else current.remove(file.id)
        }
        _selectedFiles.value = current
    }

    fun deleteSelected(allGroups: List<DuplicateGroup>, onComplete: () -> Unit) {
        viewModelScope.launch {
            val toDelete = allGroups.flatMap { group ->
                // Guard: Ensure we never delete a preserved file
                val preservedId = _preservedFileOverrides.value[group.groupId] ?: group.preservedFileId
                group.files.filter { _selectedFiles.value.contains(it.id) && it.id != preservedId }
            }
            if (toDelete.isNotEmpty()) {
                deleteSelectedFilesUseCase(toDelete)
                _selectedFiles.value = emptySet()
                duplicateRepository.runExactDuplicateDetection()
                onComplete()
            }
        }
    }
}
