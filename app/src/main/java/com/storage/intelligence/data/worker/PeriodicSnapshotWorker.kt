package com.storage.intelligence.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.storage.intelligence.domain.repository.TimelineRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PeriodicSnapshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val timelineRepository: TimelineRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            timelineRepository.recordCurrentSnapshot()
            com.storage.intelligence.core.logging.AppLogger.d("PeriodicSnapshotWorker", "Successfully recorded periodic storage snapshot.")
            Result.success()
        } catch (e: Exception) {
            com.storage.intelligence.core.logging.AppLogger.e("PeriodicSnapshotWorker", "Failed to record periodic snapshot: ${e.message}", e)
            Result.failure()
        }
    }
}
