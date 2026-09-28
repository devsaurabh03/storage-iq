package com.storage.intelligence.feature.photo.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.core.database.dao.FileMetadataDao
import com.storage.intelligence.core.database.entity.FileMetadataEntity
import com.storage.intelligence.core.datastore.UserPreferencesRepository
import com.storage.intelligence.core.logging.AppLogger
import com.storage.intelligence.domain.model.FileItem
import com.storage.intelligence.domain.model.StorageCategory
import com.storage.intelligence.domain.repository.StorageRepository
import com.storage.intelligence.feature.photo.engine.PhotoOptimizationEngine
import com.storage.intelligence.feature.photo.model.PhotoOptimizationCandidate
import com.storage.intelligence.feature.photo.model.PhotoOptimizationConfig
import com.storage.intelligence.feature.photo.model.PhotoOptimizationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class PhotoOptimizationUiState(
    val candidates: List<PhotoOptimizationCandidate> = emptyList(),
    val isEvaluating: Boolean = true,
    val isOptimizing: Boolean = false,
    val currentOptimizingIndex: Int = 0,
    val totalToOptimize: Int = 0,
    val totalEstimatedSavingsBytes: Long = 0L,
    val totalSavedSoFarBytes: Long = 0L,
    val lastResult: PhotoOptimizationResult? = null,
    val totalScannedCount: Int = 0,
    val unoptimizedCount: Int = 0,
    val alreadyOptimalCount: Int = 0
)

@HiltViewModel
class PhotoOptimizationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageRepository: StorageRepository,
    private val photoEngine: PhotoOptimizationEngine,
    private val preferencesRepository: UserPreferencesRepository,
    private val fileDao: FileMetadataDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoOptimizationUiState())
    val uiState: StateFlow<PhotoOptimizationUiState> = _uiState.asStateFlow()

    init {
        loadPhotoCandidates()
    }

    fun loadPhotoCandidates() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isEvaluating = true)
            AppLogger.i("PhotoOptimizer", "Starting device photo scan (MediaStore & Database)...")

            // 1. Scan photos directly from MediaStore
            val mediaStorePhotos = scanMediaStoreImages()
            AppLogger.i("PhotoOptimizer", "MediaStore returned ${mediaStorePhotos.size} photo records.")

            // 2. Fetch and merge from Room DB
            val dbEntities = try {
                fileDao.getFilesByCategory(StorageCategory.PHOTOS.name)
            } catch (e: Exception) {
                AppLogger.e("PhotoOptimizer", "Error querying database photos: ${e.message}", e)
                emptyList()
            }

            val existingPaths = mediaStorePhotos.map { it.path }.toSet()
            val mergedPhotos = mediaStorePhotos.toMutableList()
            dbEntities.forEach { entity ->
                if (!existingPaths.contains(entity.path)) {
                    val file = File(entity.path)
                    if (file.exists() && file.length() > 0) {
                        mergedPhotos.add(entity.toDomain())
                    }
                }
            }

            AppLogger.i("PhotoOptimizer", "Total unique photos discovered: ${mergedPhotos.size}")

            if (mergedPhotos.isEmpty()) {
                AppLogger.w("PhotoOptimizer", "0 photos found on device. Check storage permissions (READ_MEDIA_IMAGES).")
                _uiState.value = _uiState.value.copy(
                    candidates = emptyList(),
                    isEvaluating = false,
                    totalEstimatedSavingsBytes = 0L,
                    totalScannedCount = 0,
                    unoptimizedCount = 0,
                    alreadyOptimalCount = 0
                )
                return@launch
            }

            val evaluated = mergedPhotos.map { photoEngine.evaluateCandidate(it) }
            val worthOptimizing = evaluated.filter { it.isWorthOptimizing }
            val alreadyOptimal = evaluated.filter { !it.isWorthOptimizing }
            val totalSavings = worthOptimizing.sumOf { it.estimatedSavingsBytes }

            AppLogger.i(
                "PhotoOptimizer",
                "Photo scan complete: ${evaluated.size} total images scanned. ${worthOptimizing.size} can be optimized (${FormattingUtils.formatBytes(totalSavings)} potential savings), ${alreadyOptimal.size} already optimal/compact."
            )

            _uiState.value = _uiState.value.copy(
                candidates = evaluated,
                isEvaluating = false,
                totalEstimatedSavingsBytes = totalSavings,
                totalScannedCount = evaluated.size,
                unoptimizedCount = worthOptimizing.size,
                alreadyOptimalCount = alreadyOptimal.size
            )
        }
    }

    private suspend fun scanMediaStoreImages(): List<FileItem> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<FileItem>()
        try {
            val projection = arrayOf(
                android.provider.MediaStore.Images.Media._ID,
                android.provider.MediaStore.Images.Media.DATA,
                android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                android.provider.MediaStore.Images.Media.SIZE,
                android.provider.MediaStore.Images.Media.MIME_TYPE,
                android.provider.MediaStore.Images.Media.DATE_MODIFIED,
                android.provider.MediaStore.Images.Media.WIDTH,
                android.provider.MediaStore.Images.Media.HEIGHT
            )

            val uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val cursor = context.contentResolver.query(
                uri,
                projection,
                "${android.provider.MediaStore.Images.Media.SIZE} > 0",
                null,
                "${android.provider.MediaStore.Images.Media.SIZE} DESC"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media._ID)
                val pathCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA)
                val nameCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.SIZE)
                val mimeCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.MIME_TYPE)
                val dateCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATE_MODIFIED)
                val widthCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.WIDTH)
                val heightCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.HEIGHT)

                while (c.moveToNext()) {
                    val path = c.getString(pathCol) ?: continue
                    val file = File(path)
                    val size = c.getLong(sizeCol)
                    if (size <= 0L) continue

                    val id = c.getLong(idCol)
                    val name = c.getString(nameCol) ?: file.name
                    val mime = c.getString(mimeCol) ?: "image/jpeg"
                    val dateSec = c.getLong(dateCol)
                    val width = c.getInt(widthCol)
                    val height = c.getInt(heightCol)

                    resultList.add(
                        FileItem(
                            id = id,
                            path = path,
                            name = name,
                            sizeBytes = size,
                            mimeType = mime,
                            modifiedTimeMs = dateSec * 1000L,
                            category = StorageCategory.PHOTOS,
                            width = if (width > 0) width else 0,
                            height = if (height > 0) height else 0
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLogger.e("PhotoOptimizer", "MediaStore image scan failed: ${e.message}", e)
        }
        resultList
    }

    private fun FileMetadataEntity.toDomain(): FileItem {
        return FileItem(
            id = id,
            path = path,
            name = name,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            modifiedTimeMs = modifiedTimeMs,
            category = StorageCategory.valueOf(category),
            sha256Hash = sha256Hash,
            perceptualHash = perceptualHash,
            width = width,
            height = height,
            durationMs = durationMs,
            isScreenshot = isScreenshot,
            isScreenRecording = isScreenRecording,
            isDownload = isDownload,
            sourceApp = sourceApp,
            isOptimized = isOptimized,
            originalSizeBytes = originalSizeBytes,
            compressedFilePath = compressedFilePath,
            compressionAlgorithm = compressionAlgorithm
        )
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

        com.storage.intelligence.core.logging.AppLogger.i(
            "PhotoOptimizer",
            "User started optimization of ${selected.size} selected images (deleteOriginals=$deleteOriginals)"
        )

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
                com.storage.intelligence.core.logging.AppLogger.i("PhotoOptimizer", "Cleaned up ${filesToDelete.size} original photos after successful optimization.")
            }

            com.storage.intelligence.core.logging.AppLogger.i(
                "PhotoOptimizer",
                "Completed batch optimization: $successCount of ${selected.size} succeeded. Reclaimed ${savedTotal / 1024} KB total storage."
            )

            _uiState.value = _uiState.value.copy(isOptimizing = false)
            onComplete(savedTotal, successCount, lastErrorMessage)
            loadPhotoCandidates()
        }
    }
}
