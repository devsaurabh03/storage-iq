package com.storage.intelligence.feature.video.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.feature.video.benchmark.BenchmarkEngine
import com.storage.intelligence.feature.video.benchmark.CodecBenchmarkMetrics
import com.storage.intelligence.feature.video.codec.CodecCapabilityManager
import com.storage.intelligence.feature.video.codec.DeviceCodecProfile
import com.storage.intelligence.feature.video.engine.OptimizationMode
import com.storage.intelligence.feature.video.engine.OptimizationPlan
import com.storage.intelligence.feature.video.engine.VideoCharacteristics
import com.storage.intelligence.feature.video.engine.VideoMetadataExtractor
import com.storage.intelligence.feature.video.engine.VideoOptimizationEngine
import com.storage.intelligence.feature.video.ui.JobStatus
import com.storage.intelligence.feature.video.ui.OptimizationJobState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VideoUiState(
    val isLoading: Boolean = true,
    val videos: List<VideoCharacteristics> = emptyList(),
    val totalStorageBytes: Long = 0L,
    val potentialSavingsBytes: Long = 0L,
    val selectedVideo: VideoCharacteristics? = null,
    val selectedVideoPlan: OptimizationPlan? = null,
    val isCompressionSheetVisible: Boolean = false,
    val activeJobs: List<OptimizationJobState> = emptyList(),
    val deviceProfile: DeviceCodecProfile? = null,
    val isPerformanceHudEnabled: Boolean = false,
    val lastBenchmarkResult: CodecBenchmarkMetrics? = null,
    val isBenchmarking: Boolean = false
)

@HiltViewModel
class VideoViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoUiState())
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val profile = CodecCapabilityManager.detectCapabilities()
            val videoList = scanMediaStoreVideos()

            var totalBytes = 0L
            var totalSavings = 0L

            videoList.forEach { video ->
                val plan = VideoOptimizationEngine.generatePlan(video, profile, OptimizationMode.BALANCED)
                totalBytes += video.sizeBytes
                totalSavings += plan.estimatedSavingsBytes
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    videos = videoList,
                    totalStorageBytes = totalBytes,
                    potentialSavingsBytes = totalSavings,
                    deviceProfile = profile
                )
            }
        }
    }

    fun selectVideo(video: VideoCharacteristics) {
        val profile = _uiState.value.deviceProfile ?: CodecCapabilityManager.detectCapabilities()
        val plan = VideoOptimizationEngine.generatePlan(video, profile, OptimizationMode.BALANCED)
        _uiState.update {
            it.copy(
                selectedVideo = video,
                selectedVideoPlan = plan
            )
        }
    }

    fun updateOptimizationMode(mode: OptimizationMode) {
        val video = _uiState.value.selectedVideo ?: return
        val profile = _uiState.value.deviceProfile ?: CodecCapabilityManager.detectCapabilities()
        val updatedPlan = VideoOptimizationEngine.generatePlan(video, profile, mode)
        _uiState.update { it.copy(selectedVideoPlan = updatedPlan) }
    }

    fun showCompressionSheet(show: Boolean) {
        _uiState.update { it.copy(isCompressionSheetVisible = show) }
    }

    fun togglePerformanceHud(enabled: Boolean) {
        _uiState.update { it.copy(isPerformanceHudEnabled = enabled) }
    }

    fun startOptimization(plan: OptimizationPlan) {
        val video = _uiState.value.selectedVideo ?: return
        _uiState.update { it.copy(isCompressionSheetVisible = false) }

        val newJob = OptimizationJobState(
            id = System.currentTimeMillis().toString(),
            title = video.name,
            currentItemIndex = 1,
            totalItems = 1,
            progressPercent = 5,
            statusText = "Starting hardware optimization...",
            estimatedTimeRemainingText = "2 min",
            state = JobStatus.RUNNING,
            savedBytes = plan.estimatedSavingsBytes
        )

        _uiState.update { current ->
            current.copy(activeJobs = current.activeJobs + newJob)
        }
    }

    fun runBenchmark(testCaseName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBenchmarking = true) }
            val report = BenchmarkEngine.runFullBenchmarkSuite(context, null) { status, progress -> }
            _uiState.update {
                it.copy(
                    isBenchmarking = false,
                    lastBenchmarkResult = report.av1Result
                )
            }
        }
    }

    private fun scanMediaStoreVideos(): List<VideoCharacteristics> {
        val dummyFile = File(context.cacheDir, "sample_vacation.mp4")
        val v1 = VideoCharacteristics(
            file = dummyFile,
            name = "Vacation.mp4",
            sizeBytes = 1_095_216_640L, // 1.02 GB
            durationMs = 762_000L,
            width = 3840,
            height = 2160,
            fps = 60,
            bitrateBps = 42_000_000,
            codecName = "H.264",
            mimeType = "video/avc",
            isHdr = true,
            audioCodec = "AAC"
        )

        val v2 = VideoCharacteristics(
            file = dummyFile,
            name = "Drone_Footage_4K.mp4",
            sizeBytes = 2_450_000_000L, // 2.45 GB
            durationMs = 1_200_000L,
            width = 3840,
            height = 2160,
            fps = 60,
            bitrateBps = 50_000_000,
            codecName = "H.264",
            mimeType = "video/avc",
            isHdr = false,
            audioCodec = "AAC"
        )

        val v3 = VideoCharacteristics(
            file = dummyFile,
            name = "Family_Event_1080p.mp4",
            sizeBytes = 650_000_000L, // 650 MB
            durationMs = 900_000L,
            width = 1920,
            height = 1080,
            fps = 30,
            bitrateBps = 12_000_000,
            codecName = "H.264",
            mimeType = "video/avc",
            isHdr = false,
            audioCodec = "AAC"
        )

        return listOf(v1, v2, v3)
    }
}
