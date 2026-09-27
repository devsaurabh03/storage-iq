package com.storage.intelligence.feature.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.domain.model.*
import com.storage.intelligence.domain.repository.StorageRepository
import com.storage.intelligence.domain.usecase.DeleteSelectedFilesUseCase
import com.storage.intelligence.domain.usecase.GetMessagingStorageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MessagingMediaViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    private val getMessagingStorageUseCase: GetMessagingStorageUseCase,
    private val deleteSelectedFilesUseCase: DeleteSelectedFilesUseCase,
    private val duplicateRepository: com.storage.intelligence.domain.repository.DuplicateRepository
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

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

    private val _selectedApp = MutableStateFlow("WhatsApp")
    val selectedApp: StateFlow<String> = _selectedApp.asStateFlow()

    private val _selectedSubcategory = MutableStateFlow<MessagingMediaType?>(null)
    val selectedSubcategory: StateFlow<MessagingMediaType?> = _selectedSubcategory.asStateFlow()

    val messagingReport: StateFlow<MessagingStorageReport> = getMessagingStorageUseCase()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            MessagingStorageReport(0L, 0L, emptyList())
        )

    val mediaFiles: StateFlow<Result<List<FileItem>>> = combine(
        _selectedApp,
        _selectedSubcategory
    ) { app, subcategory ->
        Pair(app, subcategory)
    }.flatMapLatest { (app, subcategory) ->
        storageRepository.getMessagingMediaFlow(app).map { files ->
            if (subcategory == null) {
                files
            } else {
                files.filter { file ->
                    when (subcategory) {
                        MessagingMediaType.VIDEOS -> file.category == StorageCategory.VIDEOS || file.mimeType.startsWith("video/")
                        MessagingMediaType.IMAGES -> file.category == StorageCategory.PHOTOS || file.mimeType.startsWith("image/")
                        MessagingMediaType.DOCUMENTS -> file.category == StorageCategory.DOCUMENTS || file.mimeType.contains("pdf") || file.mimeType.contains("document")
                        MessagingMediaType.AUDIO -> file.category == StorageCategory.AUDIO || file.mimeType.startsWith("audio/")
                        MessagingMediaType.OTHER -> file.category == StorageCategory.OTHER
                    }
                }
            }
        }
    }
        .map <List<FileItem>, Result<List<FileItem>>> { Result.Success(it) }
        .catch { emit(Result.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading)

    private val _selectedFiles = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFiles: StateFlow<Set<Long>> = _selectedFiles.asStateFlow()

    fun setSelectedApp(app: String) {
        _selectedApp.value = app
        _selectedSubcategory.value = null
        _selectedFiles.value = emptySet()
    }

    fun setSelectedSubcategory(type: MessagingMediaType?) {
        _selectedSubcategory.value = type
        _selectedFiles.value = emptySet()
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

    fun getDuplicateCopiesForFile(fileItem: FileItem, onResult: (List<FileItem>) -> Unit) {
        viewModelScope.launch {
            val hash = fileItem.sha256Hash
            val copies = if (hash != null) {
                storageRepository.getFilesByHash(hash)
            } else {
                storageRepository.getFilesBySize(fileItem.sizeBytes)
            }
            onResult(copies.filter { it.id != fileItem.id })
        }
    }

    fun deleteSpecificFiles(files: List<FileItem>) {
        viewModelScope.launch {
            if (files.isNotEmpty()) {
                deleteSelectedFilesUseCase(files)
            }
        }
    }
}
