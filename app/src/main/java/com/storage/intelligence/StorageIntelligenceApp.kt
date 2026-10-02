package com.storage.intelligence

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StorageIntelligenceApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        com.storage.intelligence.core.logging.AppLogger.init(this)
        com.storage.intelligence.core.logging.AppLogger.i("App", "StorageIQ Application launched (v${BuildConfig.VERSION_NAME})")

        // Global uncaught exception handler to capture all unhandled errors and crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            com.storage.intelligence.core.logging.AppLogger.e(
                "CRASH",
                "FATAL: Uncaught exception in thread '${thread.name}': ${throwable.localizedMessage}",
                throwable
            )
            defaultHandler?.uncaughtException(thread, throwable)
        }

        scheduleBackgroundWorkers()
    }

    private fun scheduleBackgroundWorkers() {
        try {
            val workManager = androidx.work.WorkManager.getInstance(this)

            // Periodic snapshot worker: every 12 hours to track storage growth timeline
            val snapshotConstraints = androidx.work.Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val snapshotRequest = androidx.work.PeriodicWorkRequestBuilder<com.storage.intelligence.data.worker.PeriodicSnapshotWorker>(
                12, java.util.concurrent.TimeUnit.HOURS
            ).setConstraints(snapshotConstraints).build()

            workManager.enqueueUniquePeriodicWork(
                "periodic_storage_snapshot",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                snapshotRequest
            )

            // Storage scan worker: daily background scan
            val scanConstraints = androidx.work.Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val scanRequest = androidx.work.PeriodicWorkRequestBuilder<com.storage.intelligence.data.worker.StorageScanWorker>(
                24, java.util.concurrent.TimeUnit.HOURS
            ).setConstraints(scanConstraints).build()

            workManager.enqueueUniquePeriodicWork(
                "periodic_storage_scan",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                scanRequest
            )

            com.storage.intelligence.core.logging.AppLogger.d("App", "Background maintenance workers successfully enqueued.")
        } catch (e: Exception) {
            com.storage.intelligence.core.logging.AppLogger.e("App", "Failed to enqueue background maintenance workers: ${e.message}", e)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
