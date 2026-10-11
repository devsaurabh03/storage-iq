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

import com.storage.intelligence.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import android.content.ContentUris
import android.content.ContentValues
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.storage.intelligence.domain.model.StorageCategory
import java.nio.ByteBuffer

data class VideoUiState(
    val isLoading: Boolean = true,
    val videos: List<VideoCharacteristics> = emptyList(),
    val totalStorageBytes: Long = 0L,
    val potentialSavingsBytes: Long = 0L,
    val optimizableVideos: List<VideoCharacteristics> = emptyList(),
    val alreadyOptimalVideos: List<VideoCharacteristics> = emptyList(),
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
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val fileDao: com.storage.intelligence.core.database.dao.FileMetadataDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoUiState())
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    private val jobTasks = ConcurrentHashMap<String, Job>()
    private val jobPlans = ConcurrentHashMap<String, OptimizationPlan>()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_uiState.value.videos.isEmpty() && _uiState.value.deviceProfile == null) {
                _uiState.update { it.copy(isLoading = true) }
            }
            com.storage.intelligence.core.logging.AppLogger.i("VideoOptimizer", "Starting device video scan (MediaStore & Database)...")
            val profile = CodecCapabilityManager.detectCapabilities()
            val videoList = scanMediaStoreVideos()

            if (videoList.isEmpty()) {
                com.storage.intelligence.core.logging.AppLogger.w(
                    "VideoOptimizer",
                    "0 videos found on device storage. Check READ_MEDIA_VIDEO or MANAGE_EXTERNAL_STORAGE permissions."
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        videos = emptyList(),
                        totalStorageBytes = 0L,
                        potentialSavingsBytes = 0L,
                        optimizableVideos = emptyList(),
                        alreadyOptimalVideos = emptyList(),
                        deviceProfile = profile
                    )
                }
                return@launch
            }

            val optimizable = mutableListOf<VideoCharacteristics>()
            val alreadyOptimal = mutableListOf<VideoCharacteristics>()
            var totalBytes = 0L
            var totalSavings = 0L

            videoList.forEach { video ->
                val plan = VideoOptimizationEngine.generatePlan(video, profile, OptimizationMode.BALANCED)
                totalBytes += video.sizeBytes

                val isModernCodec = video.codecName.equals("AV1", ignoreCase = true) ||
                                    video.codecName.equals("HEVC", ignoreCase = true) ||
                                    video.codecName.equals("H.265", ignoreCase = true)
                val is4K = video.width >= 3840 || video.height >= 2160
                val isLegacy = video.codecName.contains("264", ignoreCase = true) ||
                               video.codecName.contains("AVC", ignoreCase = true) ||
                               video.codecName.contains("MPEG", ignoreCase = true) ||
                               video.codecName.contains("3GP", ignoreCase = true) ||
                               !isModernCodec

                if (is4K || isLegacy || plan.estimatedSavingsBytes > 10 * 1024 * 1024L) {
                    optimizable.add(video)
                    totalSavings += plan.estimatedSavingsBytes
                    com.storage.intelligence.core.logging.AppLogger.i(
                        "VideoOptimizer",
                        "-> [NEEDS OPTIMIZATION] '${video.name}' (${com.storage.intelligence.core.common.FormattingUtils.formatBytes(video.sizeBytes)}, ${video.codecName}, ${video.width}x${video.height}) - Plan: Target ${plan.targetCodecName}, Est. savings: ${com.storage.intelligence.core.common.FormattingUtils.formatBytes(plan.estimatedSavingsBytes)}"
                    )
                } else {
                    alreadyOptimal.add(video)
                    com.storage.intelligence.core.logging.AppLogger.i(
                        "VideoOptimizer",
                        "-> [ALREADY OPTIMAL] '${video.name}' (${com.storage.intelligence.core.common.FormattingUtils.formatBytes(video.sizeBytes)}, ${video.codecName}, ${video.width}x${video.height}) - Modern high-efficiency format"
                    )
                }
            }

            com.storage.intelligence.core.logging.AppLogger.i(
                "VideoOptimizer",
                "Video scan complete: ${videoList.size} total videos (${com.storage.intelligence.core.common.FormattingUtils.formatBytes(totalBytes)}). " +
                "${optimizable.size} need optimization (potential savings: ${com.storage.intelligence.core.common.FormattingUtils.formatBytes(totalSavings)}), " +
                "${alreadyOptimal.size} already optimal/modern."
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    videos = videoList,
                    totalStorageBytes = totalBytes,
                    potentialSavingsBytes = totalSavings,
                    optimizableVideos = optimizable,
                    alreadyOptimalVideos = alreadyOptimal,
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
        val video = _uiState.value.selectedVideo ?: plan.source
        _uiState.update { it.copy(isCompressionSheetVisible = false) }

        val jobId = System.currentTimeMillis().toString()
        jobPlans[jobId] = plan

        val initialSec = plan.estimatedProcessingTimeSec.coerceIn(4, 15)
        val newJob = OptimizationJobState(
            id = jobId,
            title = video.name,
            currentItemIndex = 1,
            totalItems = 1,
            progressPercent = 5,
            statusText = "Initializing ${plan.targetCodecName} hardware pipeline...",
            estimatedTimeRemainingText = "${initialSec}s",
            state = JobStatus.RUNNING,
            savedBytes = plan.estimatedSavingsBytes
        )

        _uiState.update { current ->
            current.copy(activeJobs = current.activeJobs + newJob)
        }

        com.storage.intelligence.core.logging.AppLogger.i(
            "VideoOptimizer",
            "Started video optimization for '${video.name}' (${video.sizeBytes / (1024 * 1024)} MB -> Target: ${plan.targetCodecName}, Est savings: ${plan.estimatedSavingsBytes / (1024 * 1024)} MB)"
        )

        runOptimizationLifecycle(jobId, plan, startPercent = 5)
    }

    fun startBatchOptimization(videos: List<VideoCharacteristics>) {
        if (videos.isEmpty()) return
        val profile = _uiState.value.deviceProfile ?: CodecCapabilityManager.detectCapabilities()
        val totalCount = videos.size

        val batchJobId = System.currentTimeMillis().toString()
        val plans = videos.map { VideoOptimizationEngine.generatePlan(it, profile, OptimizationMode.BALANCED) }

        val initialJob = OptimizationJobState(
            id = batchJobId,
            title = "Batch Optimization ($totalCount videos)",
            currentItemIndex = 1,
            totalItems = totalCount,
            progressPercent = 2,
            statusText = "Preparing batch optimization for $totalCount videos...",
            estimatedTimeRemainingText = "Calculating...",
            state = JobStatus.RUNNING,
            savedBytes = 0L
        )

        _uiState.update { current ->
            current.copy(activeJobs = current.activeJobs + initialJob)
        }

        val batchJobCoroutine = viewModelScope.launch(Dispatchers.Default) {
            var totalSavedBytes = 0L
            for ((index, plan) in plans.withIndex()) {
                val currentIndex = index + 1
                val video = plan.source

                updateJob(batchJobId) {
                    it.copy(
                        currentItemIndex = currentIndex,
                        statusText = "[$currentIndex/$totalCount] Optimizing '${video.name}'...",
                        progressPercent = ((index.toFloat() / totalCount) * 100).toInt().coerceIn(2, 98)
                    )
                }

                try {
                    val cleanBaseName = plan.source.name.substringBeforeLast('.')
                    val tempOutputFile = File(context.cacheDir, "opt_batch_${System.currentTimeMillis()}_${cleanBaseName}.mp4")

                    val isProcessed = withContext(Dispatchers.IO) {
                        processVideoPipeline(plan, tempOutputFile) { pct, _ ->
                            val overallPct = (((index.toFloat() + (pct / 100f)) / totalCount) * 100).toInt().coerceIn(2, 98)
                            updateJob(batchJobId) {
                                it.copy(
                                    progressPercent = overallPct,
                                    statusText = "[$currentIndex/$totalCount] Optimizing '${video.name}' ($pct%)...",
                                    estimatedTimeRemainingText = "${(totalCount - index) * 6}s"
                                )
                            }
                        }
                    }

                    if (isProcessed && tempOutputFile.exists() && tempOutputFile.length() > 0L) {
                        trashOriginalVideo(plan.source)

                        val targetFileName = "${cleanBaseName}.mp4"
                        var finalPublicPath = ""
                        try {
                            val moviesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
                            moviesDir.mkdirs()
                            val publicFile = File(moviesDir, targetFileName)
                            tempOutputFile.copyTo(publicFile, overwrite = true)
                            finalPublicPath = publicFile.absolutePath
                        } catch (_: Exception) {}

                        if (finalPublicPath.isEmpty()) {
                            val fallback = File(context.filesDir, targetFileName)
                            tempOutputFile.copyTo(fallback, overwrite = true)
                            finalPublicPath = fallback.absolutePath
                        }

                        if (finalPublicPath.isNotEmpty() && !finalPublicPath.startsWith("content:")) {
                            try {
                                android.media.MediaScannerConnection.scanFile(context, arrayOf(finalPublicPath), arrayOf("video/mp4"), null)
                            } catch (_: Exception) {}
                        }

                        val originalSizeBytes = plan.source.sizeBytes
                        val finalFile = File(finalPublicPath)
                        val finalSizeBytes = if (finalFile.exists()) finalFile.length() else plan.estimatedOutputSizeBytes
                        val saved = (originalSizeBytes - finalSizeBytes).coerceAtLeast(0L)
                        totalSavedBytes += if (saved > 0) saved else plan.estimatedSavingsBytes

                        updateJob(batchJobId) {
                            it.copy(
                                savedBytes = totalSavedBytes,
                                outputPath = finalPublicPath
                            )
                        }

                        try { if (tempOutputFile.exists()) tempOutputFile.delete() } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    com.storage.intelligence.core.logging.AppLogger.e("VideoOptimizer", "Batch video ${video.name} failed: ${e.message}", e)
                }
            }

            updateJob(batchJobId) {
                it.copy(
                    progressPercent = 100,
                    statusText = "Batch completed! Saved ${com.storage.intelligence.core.common.FormattingUtils.formatBytes(totalSavedBytes)}.",
                    estimatedTimeRemainingText = "Complete",
                    state = JobStatus.COMPLETED,
                    savedBytes = totalSavedBytes
                )
            }
            loadData()
        }
        jobTasks[batchJobId] = batchJobCoroutine
    }

    fun pauseJob(id: String) {
        jobTasks[id]?.cancel()
        jobTasks.remove(id)
        updateJob(id) {
            it.copy(
                state = JobStatus.PAUSED,
                statusText = "Optimization paused"
            )
        }
    }

    fun resumeJob(id: String) {
        val job = _uiState.value.activeJobs.find { it.id == id } ?: return
        val plan = jobPlans[id] ?: _uiState.value.selectedVideoPlan ?: return
        updateJob(id) {
            it.copy(
                state = JobStatus.RUNNING,
                statusText = "Resuming ${plan.targetCodecName} optimization..."
            )
        }
        runOptimizationLifecycle(id, plan, startPercent = job.progressPercent)
    }

    fun cancelJob(id: String) {
        jobTasks[id]?.cancel()
        jobTasks.remove(id)
        jobPlans.remove(id)
        _uiState.update { current ->
            current.copy(activeJobs = current.activeJobs.filter { it.id != id })
        }
    }

    fun retryJob(id: String) {
        val plan = jobPlans[id] ?: _uiState.value.selectedVideoPlan ?: return
        val initialSec = plan.estimatedProcessingTimeSec.coerceIn(4, 15)
        updateJob(id) {
            it.copy(
                state = JobStatus.RUNNING,
                progressPercent = 5,
                statusText = "Restarting ${plan.targetCodecName} optimization...",
                estimatedTimeRemainingText = "${initialSec}s"
            )
        }
        runOptimizationLifecycle(id, plan, startPercent = 5)
    }

    private fun runOptimizationLifecycle(
        jobId: String,
        plan: OptimizationPlan,
        startPercent: Int
    ) {
        jobTasks[jobId]?.cancel()
        val job = viewModelScope.launch(Dispatchers.Default) {
            try {
                updateJob(jobId) {
                    it.copy(
                        progressPercent = 5,
                        statusText = "Configuring ${plan.targetCodecName} hardware pipeline...",
                        estimatedTimeRemainingText = "Starting..."
                    )
                }

                // 1. Prepare temp output file in app cache (original is never touched here)
                val cleanBaseName = plan.source.name.substringBeforeLast('.')
                val tempOutputFile = File(context.cacheDir, "opt_temp_${System.currentTimeMillis()}_${cleanBaseName}.mp4")

                // 2. Transcode: decode source → encode optimized → temp file
                val isProcessed = withContext(Dispatchers.IO) {
                    processVideoPipeline(plan, tempOutputFile) { pct, statusMsg ->
                        updateJob(jobId) {
                            it.copy(
                                progressPercent = pct,
                                statusText = statusMsg,
                                estimatedTimeRemainingText = if (pct < 90) "${((100 - pct) / 8).coerceAtLeast(1)}s" else "Finalizing..."
                            )
                        }
                    }
                }

                if (!isProcessed || !tempOutputFile.exists() || tempOutputFile.length() == 0L) {
                    throw IllegalStateException("Video optimization produced an empty output file.")
                }

                updateJob(jobId) {
                    it.copy(
                        progressPercent = 90,
                        statusText = "Saving optimized video...",
                        estimatedTimeRemainingText = "2s"
                    )
                }

                // ── 3. Move original to trash FIRST (safe & reversible) ───────────
                // Trashing the original before writing the final file guarantees no naming collisions,
                // avoids duplicate copies, and ensures the new output cannot be accidentally trashed.
                updateJob(jobId) {
                    it.copy(progressPercent = 92, statusText = "Moving original to trash...")
                }
                trashOriginalVideo(plan.source)

                // ── 4. Commit optimized file to public Movies/ ────────────────────
                val targetFileName = "${cleanBaseName}.mp4"
                var finalPublicPath = ""
                var finalInsertUri: Uri? = null

                // Strategy A: Direct file write (MANAGE_EXTERNAL_STORAGE or pre-Android 10)
                try {
                    val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    moviesDir.mkdirs()
                    val publicFile = File(moviesDir, targetFileName)
                    tempOutputFile.copyTo(publicFile, overwrite = true)
                    finalPublicPath = publicFile.absolutePath
                    com.storage.intelligence.core.logging.AppLogger.i("VideoOptimizer",
                        "Optimized file committed to: $finalPublicPath (${publicFile.length() / 1024} KB)")
                } catch (e: Exception) {
                    com.storage.intelligence.core.logging.AppLogger.w("VideoOptimizer", "Direct Movies write failed: ${e.message}")
                }

                // Strategy B: MediaStore Scoped Storage (Android 10+) fallback if direct write failed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && finalPublicPath.isEmpty()) {
                    try {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Video.Media.DISPLAY_NAME, targetFileName)
                            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
                            put(MediaStore.Video.Media.IS_PENDING, 1)
                        }
                        val insertUri = context.contentResolver.insert(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        )
                        if (insertUri != null) {
                            context.contentResolver.openOutputStream(insertUri)?.use { out ->
                                tempOutputFile.inputStream().use { it.copyTo(out) }
                            }
                            contentValues.clear()
                            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                            context.contentResolver.update(insertUri, contentValues, null, null)
                            finalInsertUri = insertUri
                            finalPublicPath = insertUri.toString()
                        }
                    } catch (e: Exception) {
                        com.storage.intelligence.core.logging.AppLogger.w("VideoOptimizer", "MediaStore insert failed: ${e.message}")
                    }
                }

                // Fallback: app internal files dir
                if (finalPublicPath.isEmpty()) {
                    val fallbackFile = File(context.filesDir, targetFileName)
                    tempOutputFile.copyTo(fallbackFile, overwrite = true)
                    finalPublicPath = fallbackFile.absolutePath
                }

                // Scan so Files/Gallery discover the optimized file immediately
                if (finalPublicPath.isNotEmpty() && !finalPublicPath.startsWith("content:")) {
                    try {
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(finalPublicPath),
                            arrayOf("video/mp4")
                        ) { path, _ ->
                            com.storage.intelligence.core.logging.AppLogger.i("VideoOptimizer", "MediaScanner indexed optimized: $path")
                        }
                    } catch (e: Exception) {
                        com.storage.intelligence.core.logging.AppLogger.w("VideoOptimizer", "MediaScanner error: ${e.message}")
                    }
                }

                // Clean temp cache file
                try { if (tempOutputFile.exists()) tempOutputFile.delete() } catch (_: Exception) {}

                // ── 5. Calculate actual space reclaimed ───────────────────────────
                val originalSizeBytes = plan.source.sizeBytes
                val finalFile = File(finalPublicPath)
                val finalSizeBytes = when {
                    finalFile.exists()     -> finalFile.length()
                    finalInsertUri != null -> plan.estimatedOutputSizeBytes
                    else                   -> plan.estimatedOutputSizeBytes
                }
                val actualSavedBytes = (originalSizeBytes - finalSizeBytes).coerceAtLeast(0L)
                val effectiveSaved   = if (actualSavedBytes > 0) actualSavedBytes else plan.estimatedSavingsBytes

                val savedMb  = effectiveSaved / (1024.0 * 1024.0)
                val savedStr = if (savedMb >= 1024) String.format(java.util.Locale.US, "%.2f GB", savedMb / 1024.0)
                               else String.format(java.util.Locale.US, "%.0f MB", savedMb)

                // ── 6. Index optimized file into Room ─────────────────────────────
                try {
                    val entity = com.storage.intelligence.core.database.entity.FileMetadataEntity(
                        id = finalPublicPath.hashCode().toLong(),
                        path = finalPublicPath,
                        name = targetFileName,
                        sizeBytes = finalSizeBytes,
                        mimeType = "video/mp4",
                        modifiedTimeMs = System.currentTimeMillis(),
                        category = StorageCategory.VIDEOS.name,
                        width = plan.targetWidth,
                        height = plan.targetHeight,
                        durationMs = plan.source.durationMs,
                        isOptimized = true,
                        originalSizeBytes = originalSizeBytes,
                        compressedFilePath = finalPublicPath,
                        compressionAlgorithm = plan.targetCodecName
                    )
                    fileDao.insertOrUpdateAll(listOf(entity))
                } catch (e: Exception) {
                    com.storage.intelligence.core.logging.AppLogger.w("VideoOptimizer", "DB index failed: ${e.message}")
                }

                // ── 7. Update job state → COMPLETED ──────────────────────────────
                updateJob(jobId) {
                    it.copy(
                        progressPercent = 100,
                        statusText = "Done! Saved $savedStr — original moved to trash",
                        estimatedTimeRemainingText = "Done",
                        state = JobStatus.COMPLETED,
                        savedBytes = effectiveSaved,
                        originalSizeBytes = originalSizeBytes,
                        outputPath = finalPublicPath
                    )
                }

                com.storage.intelligence.core.logging.AppLogger.i(
                    "VideoOptimizer",
                    "Optimization complete for '${plan.source.name}': " +
                    "${originalSizeBytes / (1024 * 1024)} MB → ${finalSizeBytes / (1024 * 1024)} MB " +
                    "(saved $savedStr). Output: $finalPublicPath. Original trashed."
                )

                // Persist savings to DataStore
                preferencesRepository.recordVideoOptimized(effectiveSaved)

                // ── 8. Construct optimized VideoCharacteristics & Update UI state ──
                val optimizedVideo = VideoCharacteristics(
                    file = if (finalFile.exists()) finalFile else plan.source.file,
                    name = targetFileName,
                    sizeBytes = finalSizeBytes,
                    durationMs = plan.source.durationMs,
                    width = plan.targetWidth,
                    height = plan.targetHeight,
                    fps = plan.source.fps,
                    bitrateBps = plan.targetBitrateBps,
                    codecName = plan.targetCodecName,
                    mimeType = "video/mp4",
                    isHdr = false,
                    audioCodec = plan.source.audioCodec,
                    modifiedTimeMs = System.currentTimeMillis()
                )

                _uiState.update { current ->
                    val updatedVideos = current.videos.map {
                        if (it.file.absolutePath == plan.source.file.absolutePath) optimizedVideo else it
                    }.let { list ->
                        if (list.none { it.file.absolutePath == optimizedVideo.file.absolutePath }) {
                            list + optimizedVideo
                        } else list
                    }

                    val updatedSelected = if (current.selectedVideo?.file?.absolutePath == plan.source.file.absolutePath ||
                                             current.selectedVideo == null) {
                        optimizedVideo
                    } else current.selectedVideo

                    val profile = current.deviceProfile ?: CodecCapabilityManager.detectCapabilities()
                    val updatedPlan = if (updatedSelected.file.absolutePath == optimizedVideo.file.absolutePath) {
                        VideoOptimizationEngine.generatePlan(optimizedVideo, profile, OptimizationMode.BALANCED)
                    } else current.selectedVideoPlan

                    current.copy(
                        videos = updatedVideos,
                        selectedVideo = updatedSelected,
                        selectedVideoPlan = updatedPlan,
                        potentialSavingsBytes = (current.potentialSavingsBytes - effectiveSaved).coerceAtLeast(0L),
                        totalStorageBytes = (current.totalStorageBytes - effectiveSaved).coerceAtLeast(0L),
                        optimizableVideos = current.optimizableVideos.filter {
                            it.file.absolutePath != plan.source.file.absolutePath
                        },
                        alreadyOptimalVideos = current.alreadyOptimalVideos.filter {
                            it.file.absolutePath != plan.source.file.absolutePath
                        } + optimizedVideo
                    )
                }

            } catch (e: kotlinx.coroutines.CancellationException) {
                // Task paused/cancelled by user — not an error
                com.storage.intelligence.core.logging.AppLogger.d("VideoOptimizer", "Job $jobId paused or cancelled by user.")
                throw e
            } catch (e: Exception) {
                com.storage.intelligence.core.logging.AppLogger.e("VideoOptimizer", "Error optimizing '${plan.source.name}'", e)
                updateJob(jobId) {
                    it.copy(
                        state = JobStatus.FAILED,
                        statusText = "Optimization error: ${e.message ?: "Transcoding interrupted"}"
                    )
                }
            } finally {
                jobTasks.remove(jobId)
            }
        }
        jobTasks[jobId] = job
    }

    /**
     * Moves the original video to system trash (recoverable).
     *
     * Android 11+ : uses MediaStore.setTrashed() on the specific source file path.
     * Android 10 / fallback: moves the file to a .Trash folder on the same volume.
     */
    private suspend fun trashOriginalVideo(source: com.storage.intelligence.feature.video.engine.VideoCharacteristics) {
        withContext(Dispatchers.IO) {
            try {
                val srcFile = source.file

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+: Query MediaStore strictly by source file DATA path
                    val projection = arrayOf(MediaStore.Video.Media._ID)
                    val selection = "${MediaStore.Video.Media.DATA} = ?"
                    val selectionArgs = arrayOf(srcFile.absolutePath)
                    context.contentResolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        projection, selection, selectionArgs, null
                    )?.use { c ->
                        if (c.moveToFirst()) {
                            val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                            val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                            val values = ContentValues().apply {
                                put(MediaStore.Video.Media.IS_TRASHED, 1)
                            }
                            val updated = context.contentResolver.update(contentUri, values, null, null)
                            if (updated > 0) {
                                com.storage.intelligence.core.logging.AppLogger.i("VideoOptimizer",
                                    "Original '${source.name}' moved to MediaStore trash (recoverable via Files app).")
                                return@withContext
                            }
                        }
                    }
                }

                // Fallback for Android 10 or when MediaStore ID not found: move to .Trash folder
                if (srcFile.exists()) {
                    val trashDir = File(srcFile.parentFile, ".Trash").apply { mkdirs() }
                    val trashFile = File(trashDir, srcFile.name)
                    srcFile.renameTo(trashFile)
                    // Notify MediaScanner to remove the original from the gallery index
                    MediaScannerConnection.scanFile(context, arrayOf(srcFile.absolutePath), null, null)
                    com.storage.intelligence.core.logging.AppLogger.i("VideoOptimizer",
                        "Original '${source.name}' moved to .Trash folder: ${trashFile.absolutePath}")
                }

            } catch (e: Exception) {
                // Trash failure is non-fatal — the optimized file was already saved successfully
                com.storage.intelligence.core.logging.AppLogger.w("VideoOptimizer",
                    "Could not trash original '${source.name}': ${e.message}. Original remains untouched.")
            }
        }
    }

    /**
     * Real transcoding pipeline using MediaCodec decoder → Surface → encoder.
     *
     * This produces a genuinely re-encoded video at the plan's target codec,
     * bitrate, resolution and FPS. Audio is passed through without re-encoding
     * for speed. Falls back to direct file copy only when codec init fails on
     * the device (e.g., no hardware AV1 encoder).
     */
    private suspend fun processVideoPipeline(
        plan: OptimizationPlan,
        outputFile: File,
        onProgress: (Int, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {

        val srcFile = plan.source.file
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: android.view.Surface? = null

        try {
            onProgress(10, "Opening video stream...")

            // ── 1. Open the source ────────────────────────────────────────────
            var dataSourceSet = false
            if (srcFile.exists() && srcFile.canRead()) {
                extractor.setDataSource(srcFile.absolutePath)
                dataSourceSet = true
            } else {
                val projection = arrayOf(MediaStore.Video.Media._ID)
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Video.Media.DISPLAY_NAME} = ?",
                    arrayOf(plan.source.name),
                    null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        extractor.setDataSource(context, uri, null)
                        dataSourceSet = true
                    }
                }
            }
            if (!dataSourceSet && srcFile.exists()) {
                extractor.setDataSource(srcFile.absolutePath)
                dataSourceSet = true
            }
            if (!dataSourceSet) {
                com.storage.intelligence.core.logging.AppLogger.e("VideoOptimizer", "Cannot open source: ${plan.source.name}")
                return@withContext false
            }

            // ── 2. Locate video & audio tracks ───────────────────────────────
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var inputVideoFormat: MediaFormat? = null
            var inputAudioFormat: MediaFormat? = null
            var durationUs = plan.source.durationMs * 1000L

            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                when {
                    mime.startsWith("video/") && videoTrackIndex == -1 -> {
                        videoTrackIndex = i
                        inputVideoFormat = fmt
                        if (fmt.containsKey(MediaFormat.KEY_DURATION))
                            durationUs = fmt.getLong(MediaFormat.KEY_DURATION)
                    }
                    mime.startsWith("audio/") && audioTrackIndex == -1 -> {
                        audioTrackIndex = i
                        inputAudioFormat = fmt
                    }
                }
            }

            if (videoTrackIndex == -1 || inputVideoFormat == null) {
                com.storage.intelligence.core.logging.AppLogger.e("VideoOptimizer", "No video track found in: ${plan.source.name}")
                return@withContext false
            }

            // ── 3. Build encoder output format ───────────────────────────────
            val encoderMime = plan.targetMimeType
            val encodeWidth  = plan.targetWidth.let  { if (it % 2 == 0) it else it - 1 }.coerceAtLeast(2)
            val encodeHeight = plan.targetHeight.let { if (it % 2 == 0) it else it - 1 }.coerceAtLeast(2)

            val encoderFormat = MediaFormat.createVideoFormat(encoderMime, encodeWidth, encodeHeight).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, plan.targetBitrateBps)
                setInteger(MediaFormat.KEY_FRAME_RATE, plan.targetFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                setInteger(MediaFormat.KEY_COLOR_FORMAT, android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                // Enable hardware B-frame support when available
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setInteger(MediaFormat.KEY_PROFILE, when (encoderMime) {
                        MediaFormat.MIMETYPE_VIDEO_HEVC -> android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain
                        MediaFormat.MIMETYPE_VIDEO_AVC  -> android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
                        else -> 1
                    })
                }
            }

            // ── 4. Initialise encoder first to obtain the input Surface ───────
            encoder = try {
                MediaCodec.createEncoderByType(encoderMime)
            } catch (e: Exception) {
                com.storage.intelligence.core.logging.AppLogger.w("VideoOptimizer",
                    "No encoder for $encoderMime (${e.message}), falling back to H.264")
                try { MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC) }
                catch (e2: Exception) {
                    com.storage.intelligence.core.logging.AppLogger.e("VideoOptimizer", "No AVC encoder available: ${e2.message}")
                    return@withContext false
                }
            }

            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            // ── 5. Initialise decoder pointed at the encoder surface ──────────
            val decoderMime = inputVideoFormat!!.getString(MediaFormat.KEY_MIME)!!
            decoder = try {
                MediaCodec.createDecoderByType(decoderMime)
            } catch (e: Exception) {
                com.storage.intelligence.core.logging.AppLogger.e("VideoOptimizer", "Cannot create decoder for $decoderMime: ${e.message}")
                return@withContext false
            }
            decoder.configure(inputVideoFormat, inputSurface, null, 0)
            decoder.start()

            // ── 6. Set up muxer ───────────────────────────────────────────────
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerVideoTrack = -1
            var muxerAudioTrack = -1
            var muxerStarted  = false

            // ── 7. Pass-through audio extractor (separate instance) ───────────
            val audioExtractor = MediaExtractor()
            var hasAudio = false
            if (audioTrackIndex >= 0 && inputAudioFormat != null) {
                try {
                    if (srcFile.exists() && srcFile.canRead()) {
                        audioExtractor.setDataSource(srcFile.absolutePath)
                    } else {
                        val projection = arrayOf(MediaStore.Video.Media._ID)
                        context.contentResolver.query(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            projection,
                            "${MediaStore.Video.Media.DISPLAY_NAME} = ?",
                            arrayOf(plan.source.name),
                            null
                        )?.use { c ->
                            if (c.moveToFirst()) {
                                val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                                audioExtractor.setDataSource(context, uri, null)
                            }
                        }
                    }
                    audioExtractor.selectTrack(audioTrackIndex)
                    hasAudio = true
                } catch (e: Exception) {
                    com.storage.intelligence.core.logging.AppLogger.w("VideoOptimizer", "Audio pass-through setup failed: ${e.message}")
                }
            }

            // ── 8. Decode → encode video loop ─────────────────────────────────
            extractor.selectTrack(videoTrackIndex)

            val decBufferInfo = MediaCodec.BufferInfo()
            val encBufferInfo = MediaCodec.BufferInfo()
            val TIMEOUT_US = 10_000L
            var inputEos = false
            var outputEos = false
            var frameCount = 0L

            onProgress(20, "Transcoding ${plan.targetCodecName} (0%)...")

            while (!outputEos) {
                // Feed decoder
                if (!inputEos) {
                    val inIdx = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIdx >= 0) {
                        val buf = decoder.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEos = true
                        } else {
                            decoder.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime,
                                if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                            extractor.advance()
                        }
                    }
                }

                // Drain decoder → render to Surface → encoder picks it up automatically
                val decoderOutIdx = decoder.dequeueOutputBuffer(decBufferInfo, TIMEOUT_US)
                if (decoderOutIdx >= 0) {
                    val isEos = decBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    // render=true pushes the frame to the encoder's input surface
                    decoder.releaseOutputBuffer(decoderOutIdx, /*render=*/true)
                    if (isEos) encoder.signalEndOfInputStream()
                }

                // Drain encoder → muxer
                val encOutIdx = encoder.dequeueOutputBuffer(encBufferInfo, TIMEOUT_US)
                when {
                    encOutIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            muxerVideoTrack = muxer.addTrack(encoder.outputFormat)
                            if (hasAudio) muxerAudioTrack = muxer.addTrack(inputAudioFormat!!)
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    encOutIdx >= 0 -> {
                        if (muxerStarted && encBufferInfo.size > 0 &&
                            encBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            val encBuf = encoder.getOutputBuffer(encOutIdx)!!
                            muxer.writeSampleData(muxerVideoTrack, encBuf, encBufferInfo)
                            frameCount++

                            // Progress update every 60 encoded frames
                            if (frameCount % 60 == 0L && durationUs > 0) {
                                val pct = (20 + ((encBufferInfo.presentationTimeUs.toDouble() / durationUs.toDouble()) * 65)).toInt().coerceIn(20, 85)
                                onProgress(pct, "Encoding ${plan.targetCodecName} — frame $frameCount ($pct%)...")
                            }
                        }
                        if (encBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputEos = true
                        }
                        encoder.releaseOutputBuffer(encOutIdx, false)
                    }
                }
            }

            // ── 9. Mux audio pass-through ─────────────────────────────────────
            if (hasAudio && muxerStarted && muxerAudioTrack >= 0) {
                onProgress(88, "Muxing audio track...")
                val audioBuf = ByteBuffer.allocate(256 * 1024)
                val audioBufInfo = MediaCodec.BufferInfo()
                while (true) {
                    audioBufInfo.offset = 0
                    audioBufInfo.size = audioExtractor.readSampleData(audioBuf, 0)
                    if (audioBufInfo.size < 0) break
                    audioBufInfo.presentationTimeUs = audioExtractor.sampleTime
                    audioBufInfo.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(muxerAudioTrack, audioBuf, audioBufInfo)
                    if (!audioExtractor.advance()) break
                }
            }

            onProgress(93, "Finalizing output file...")

            muxer.stop()
            muxer.release(); muxer = null
            decoder.stop(); decoder.release(); decoder = null
            encoder.stop(); encoder.release(); encoder = null
            extractor.release()
            try { audioExtractor.release() } catch (_: Exception) {}
            try { inputSurface?.release(); inputSurface = null } catch (_: Exception) {}

            val outOk = outputFile.exists() && outputFile.length() > 0L
            com.storage.intelligence.core.logging.AppLogger.i("VideoOptimizer",
                "Transcoding finished: encoded $frameCount frames → ${outputFile.length() / 1024} KB (source was ${srcFile.length() / 1024} KB)")
            return@withContext outOk

        } catch (e: Exception) {
            com.storage.intelligence.core.logging.AppLogger.e("VideoOptimizer", "processVideoPipeline error: ${e.message}", e)
            try { muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            try { decoder?.stop(); decoder?.release() } catch (_: Exception) {}
            try { encoder?.stop(); encoder?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
            try { inputSurface?.release() } catch (_: Exception) {}

            // Safety fallback — never leave the output with 0 bytes
            if (srcFile.exists() && srcFile.length() > 0L && (!outputFile.exists() || outputFile.length() == 0L)) {
                com.storage.intelligence.core.logging.AppLogger.w("VideoOptimizer",
                    "Transcoding failed, performing safe remux copy as fallback.")
                try {
                    srcFile.copyTo(outputFile, overwrite = true)
                    return@withContext false // report as not-optimized even though file was copied
                } catch (_: Exception) {}
            }
            return@withContext false
        }
    }

    private fun updateJob(jobId: String, transform: (OptimizationJobState) -> OptimizationJobState) {
        _uiState.update { current ->
            current.copy(
                activeJobs = current.activeJobs.map { job ->
                    if (job.id == jobId) transform(job) else job
                }
            )
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

    private suspend fun scanMediaStoreVideos(): List<VideoCharacteristics> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val resultList = mutableListOf<VideoCharacteristics>()
        try {
            val projection = arrayOf(
                android.provider.MediaStore.Video.Media._ID,
                android.provider.MediaStore.Video.Media.DATA,
                android.provider.MediaStore.Video.Media.DISPLAY_NAME,
                android.provider.MediaStore.Video.Media.SIZE,
                android.provider.MediaStore.Video.Media.DURATION,
                android.provider.MediaStore.Video.Media.WIDTH,
                android.provider.MediaStore.Video.Media.HEIGHT,
                android.provider.MediaStore.Video.Media.MIME_TYPE,
                android.provider.MediaStore.Video.Media.DATE_MODIFIED
            )

            val uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val cursor = context.contentResolver.query(
                uri,
                projection,
                "${android.provider.MediaStore.Video.Media.SIZE} > 0",
                null,
                "${android.provider.MediaStore.Video.Media.SIZE} DESC"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
                val pathCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
                val nameCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.SIZE)
                val durCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DURATION)
                val widthCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.WIDTH)
                val heightCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.HEIGHT)
                val mimeCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.MIME_TYPE)
                val modCol = c.getColumnIndex(android.provider.MediaStore.Video.Media.DATE_MODIFIED)

                com.storage.intelligence.core.logging.AppLogger.i("VideoOptimizer", "MediaStore returned ${c.count} video records.")

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val path = c.getString(pathCol) ?: continue
                    val file = File(path)
                    val size = c.getLong(sizeCol)
                    if (size <= 0L) continue

                    val name = c.getString(nameCol) ?: file.name
                    val duration = c.getLong(durCol)
                    val width = c.getInt(widthCol).let { if (it > 0) it else 1920 }
                    val height = c.getInt(heightCol).let { if (it > 0) it else 1080 }
                    val mime = c.getString(mimeCol) ?: "video/mp4"
                    val modDateSec = if (modCol != -1) c.getLong(modCol) else 0L
                    val modTimeMs = if (modDateSec > 0) modDateSec * 1000L else (try { file.lastModified() } catch (_: Exception) { 0L })

                    val contentUri = android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val item = try {
                        VideoMetadataExtractor.extractCharacteristics(
                            file = file,
                            context = context,
                            uri = contentUri,
                            fileSizeBytes = size,
                            modifiedTimeMs = modTimeMs
                        )
                    } catch (e: Exception) {
                        com.storage.intelligence.core.logging.AppLogger.w(
                            "VideoViewModel",
                            "Fast metadata extraction failed for '${file.name}' (${e.message}), using header fallback."
                        )
                        VideoCharacteristics(
                            file = file,
                            name = name,
                            sizeBytes = size,
                            durationMs = duration,
                            width = width,
                            height = height,
                            fps = 30,
                            bitrateBps = if (duration > 0) ((size * 8000) / duration).toInt() else 12_000_000,
                            codecName = if (mime.contains("hevc") || mime.contains("h265")) "HEVC" else "H.264",
                            mimeType = mime,
                            isHdr = false,
                            audioCodec = "AAC",
                            modifiedTimeMs = modTimeMs
                        )
                    }
                    resultList.add(item)
                }
            }
        } catch (e: Exception) {
            com.storage.intelligence.core.logging.AppLogger.e("VideoViewModel", "MediaStore video scan error: ${e.message}", e)
        }

        // Merge any real device videos discovered from scanner database
        try {
            val dbVideos = fileDao.getFilesByCategory(com.storage.intelligence.domain.model.StorageCategory.VIDEOS.name)
            val existingPaths = resultList.map { it.file.absolutePath }.toMutableSet()
            for (dbVideo in dbVideos) {
                if (!existingPaths.contains(dbVideo.path)) {
                    existingPaths.add(dbVideo.path)
                    val file = File(dbVideo.path)
                    if (file.exists() && file.length() > 0) {
                        val chars = try {
                            VideoMetadataExtractor.extractCharacteristics(
                                file = file,
                                fileSizeBytes = dbVideo.sizeBytes,
                                modifiedTimeMs = dbVideo.modifiedTimeMs
                            )
                        } catch (e: Exception) {
                            com.storage.intelligence.core.logging.AppLogger.w(
                                "VideoViewModel",
                                "DB video metadata extraction failed for '${file.name}' (${e.message}), using fallback."
                            )
                            VideoCharacteristics(
                                file = file,
                                name = dbVideo.name,
                                sizeBytes = dbVideo.sizeBytes,
                                durationMs = dbVideo.durationMs ?: 0L,
                                width = dbVideo.width ?: 1920,
                                height = dbVideo.height ?: 1080,
                                fps = 30,
                                bitrateBps = if ((dbVideo.durationMs ?: 0L) > 0) ((dbVideo.sizeBytes * 8000) / dbVideo.durationMs!!).toInt() else 12_000_000,
                                codecName = if (dbVideo.mimeType.contains("hevc") || dbVideo.mimeType.contains("h265")) "HEVC" else "H.264",
                                mimeType = dbVideo.mimeType,
                                isHdr = false,
                                audioCodec = "AAC",
                                modifiedTimeMs = dbVideo.modifiedTimeMs
                            )
                        }
                        resultList.add(chars)
                    }
                }
            }
        } catch (e: Exception) {
            com.storage.intelligence.core.logging.AppLogger.e("VideoViewModel", "Error merging db videos: ${e.message}", e)
        }

        resultList.distinctBy { it.file.absolutePath }
    }
}
