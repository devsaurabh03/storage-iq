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

        // Ensure zero background processing when the app is closed:
        // Explicitly cancel all WorkManager periodic workers and tasks so the device does not run background scans or get warm.
        cancelAllBackgroundTasks()
    }

    private fun cancelAllBackgroundTasks() {
        try {
            val workManager = androidx.work.WorkManager.getInstance(this)
            workManager.cancelUniqueWork("periodic_storage_scan")
            workManager.cancelUniqueWork("periodic_storage_snapshot")
            workManager.cancelAllWork()
            com.storage.intelligence.core.logging.AppLogger.i("App", "All background WorkManager tasks explicitly cancelled. Zero background processing.")
        } catch (e: Exception) {
            com.storage.intelligence.core.logging.AppLogger.e("App", "Failed to cancel background workers: ${e.message}", e)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
