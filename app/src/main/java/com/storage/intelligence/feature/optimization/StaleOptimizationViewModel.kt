package com.storage.intelligence.feature.optimization

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class StaleOptimizationViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    private val deleteSelectedFilesUseCase: DeleteSelectedFilesUseCase
) : ViewModel() {

    private val _selectedTimeframeDays = MutableStateFlow(90)
    val selectedTimeframeDays: StateFlow<Int> = _selectedTimeframeDays.asStateFlow()

    val staleFiles: StateFlow<Result<List<FileItem>>> = _selectedTimeframeDays
        .flatMapLatest { days ->
            storageRepository.getStaleFilesFlow(days)
        }
        .map <List<FileItem>, Result<List<FileItem>>> { Result.Success(it) }
        .catch { emit(Result.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading)

    val optimizedFiles: StateFlow<List<FileItem>> = storageRepository.getOptimizedFilesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFileIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFileIds: StateFlow<Set<Long>> = _selectedFileIds.asStateFlow()

    private val _isCompressing = MutableStateFlow(false)
    val isCompressing: StateFlow<Boolean> = _isCompressing.asStateFlow()

    fun selectTimeframe(days: Int) {
        _selectedTimeframeDays.value = days
        _selectedFileIds.value = emptySet()
    }

    fun toggleFileSelection(fileId: Long, isSelected: Boolean) {
        val current = _selectedFileIds.value.toMutableSet()
        if (isSelected) current.add(fileId) else current.remove(fileId)
        _selectedFileIds.value = current
    }

    fun selectAll(files: List<FileItem>) {
        _selectedFileIds.value = files.map { it.id }.toSet()
    }

    fun optimizeSelected(context: Context, allStaleFiles: List<FileItem>, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            _isCompressing.value = true
            val selected = allStaleFiles.filter { _selectedFileIds.value.contains(it.id) }
            val saved = storageRepository.optimizeStaleFiles(context.applicationContext, selected)
            _selectedFileIds.value = emptySet()
            _isCompressing.value = false
            onComplete(saved)
        }
    }

    fun decompressAndAccess(context: Context, fileItem: FileItem, onFileReady: (java.io.File?) -> Unit) {
        viewModelScope.launch {
            val file = storageRepository.decompressAndAccessFile(context.applicationContext, fileItem)
            onFileReady(file)
        }
    }

    fun restoreOriginal(context: Context, fileItem: FileItem, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = storageRepository.restoreOptimizedFile(context.applicationContext, fileItem)
            onDone(success)
        }
    }

    fun deleteFile(fileItem: FileItem, onDone: () -> Unit) {
        viewModelScope.launch {
            deleteSelectedFilesUseCase(listOf(fileItem))
            onDone()
        }
    }
}
