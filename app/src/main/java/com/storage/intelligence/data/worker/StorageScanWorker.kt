package com.storage.intelligence.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class StorageScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        com.storage.intelligence.core.logging.AppLogger.i(
            "StorageScanWorker",
            "Background scan worker invoked but bypassed. Zero background processing allowed."
        )
        return Result.success()
    }
}
