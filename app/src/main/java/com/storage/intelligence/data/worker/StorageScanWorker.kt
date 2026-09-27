package com.storage.intelligence.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.storage.intelligence.domain.repository.DuplicateRepository
import com.storage.intelligence.domain.repository.StorageRepository
import com.storage.intelligence.domain.repository.TimelineRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collect

@HiltWorker
class StorageScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val storageRepository: StorageRepository,
    private val duplicateRepository: DuplicateRepository,
    private val timelineRepository: TimelineRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            storageRepository.scanStorage().collect()
            duplicateRepository.runExactDuplicateDetection()
            duplicateRepository.runSimilarPhotoDetection()
            timelineRepository.recordCurrentSnapshot()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
