package com.storage.intelligence.feature.photo.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.storage.intelligence.core.datastore.UserPreferencesRepository
import com.storage.intelligence.domain.repository.StorageRepository
import com.storage.intelligence.feature.performance.provider.BatteryInfoProvider
import com.storage.intelligence.feature.performance.provider.ThermalInfoProvider
import com.storage.intelligence.feature.photo.engine.PhotoOptimizationEngine
import com.storage.intelligence.feature.photo.model.PhotoOptimizationConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class PhotoOptimizationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val storageRepository: StorageRepository,
    private val photoEngine: PhotoOptimizationEngine,
    private val preferencesRepository: UserPreferencesRepository,
    private val batteryInfoProvider: BatteryInfoProvider,
    private val thermalInfoProvider: ThermalInfoProvider
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TOTAL_PHOTOS = "total_photos"
        const val KEY_CURRENT_INDEX = "current_index"
        const val KEY_SAVED_BYTES = "saved_bytes"
        const val KEY_ERROR_MESSAGE = "error_message"
    }

    override suspend fun doWork(): Result {
        // Check battery & thermal policies
        val onlyCharging = preferencesRepository.onlyWhileCharging.first()
        val pauseLowBattery = preferencesRepository.pauseOnLowBattery.first()
        val pauseThermal = preferencesRepository.pauseOnThermalStress.first()

        val battery = batteryInfoProvider.getBatteryInfo()
        val thermal = thermalInfoProvider.getThermalInfo()

        if (onlyCharging && !battery.isCharging) {
            return Result.retry()
        }
        if (pauseLowBattery && !battery.isCharging && battery.levelPercent < 20) {
            return Result.retry()
        }
        if (pauseThermal && thermal.isThrottlingDetected) {
            return Result.retry()
        }

        val photos = storageRepository.getFilesByCategoryFlow(com.storage.intelligence.domain.model.StorageCategory.PHOTOS).first()
        val candidates = photos.map { photoEngine.evaluateCandidate(it) }.filter { it.isWorthOptimizing }

        var totalSavedBytes = 0L
        candidates.forEachIndexed { index, candidate ->
            if (isStopped) return Result.failure()

            setProgress(
                workDataOf(
                    KEY_TOTAL_PHOTOS to candidates.size,
                    KEY_CURRENT_INDEX to (index + 1),
                    KEY_SAVED_BYTES to totalSavedBytes
                )
            )

            val result = photoEngine.optimizePhoto(candidate.fileItem, PhotoOptimizationConfig())
            if (result.success) {
                totalSavedBytes += result.savedBytes
                preferencesRepository.recordPhotoOptimized(result.savedBytes)
            }
        }

        return Result.success(
            workDataOf(
                KEY_TOTAL_PHOTOS to candidates.size,
                KEY_SAVED_BYTES to totalSavedBytes
            )
        )
    }
}
