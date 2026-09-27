package com.storage.intelligence.feature.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.data.scanner.ScanProgress
import com.storage.intelligence.domain.repository.DuplicateRepository
import com.storage.intelligence.domain.repository.StorageRepository
import com.storage.intelligence.domain.repository.TimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    private val duplicateRepository: DuplicateRepository,
    private val timelineRepository: TimelineRepository
) : ViewModel() {

    private val _scanProgress = MutableStateFlow(ScanProgress(0, 0, "Initializing scan..."))
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            storageRepository.scanStorage().collect { progress ->
                _scanProgress.value = progress
                if (progress.isComplete) {
                    // Trigger background hashing & timeline snapshot recording
                    duplicateRepository.runExactDuplicateDetection()
                    duplicateRepository.runSimilarPhotoDetection()
                    timelineRepository.recordCurrentSnapshot()
                }
            }
        }
    }
}
