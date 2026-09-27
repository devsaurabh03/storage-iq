package com.storage.intelligence.feature.performance.provider

import android.app.ActivityManager
import android.content.Context
import com.storage.intelligence.feature.performance.model.AppMemoryInfo
import com.storage.intelligence.feature.performance.model.RamInfo
import com.storage.intelligence.feature.performance.model.RamPressure
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RamInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getRamInfo(): RamInfo = withContext(Dispatchers.IO) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalBytes = memoryInfo.totalMem
        val availableBytes = memoryInfo.availMem
        val usedBytes = totalBytes - availableBytes
        val thresholdBytes = memoryInfo.threshold

        val usedPercent = (usedBytes.toDouble() / totalBytes * 100).toInt()

        val pressure = when {
            memoryInfo.lowMemory || usedPercent >= 85 -> RamPressure.HIGH
            usedPercent >= 70 -> RamPressure.MODERATE
            else -> RamPressure.LOW
        }

        RamInfo(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            availableBytes = availableBytes,
            thresholdBytes = thresholdBytes,
            isLowMemory = memoryInfo.lowMemory,
            pressure = pressure
        )
    }

    suspend fun getActiveAppMemoryList(): List<AppMemoryInfo> = withContext(Dispatchers.IO) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return@withContext emptyList()
        val pm = context.packageManager

        val pids = runningProcesses.map { it.pid }.toIntArray()
        val processMemoryInfos = activityManager.getProcessMemoryInfo(pids)

        val appMemoryList = mutableListOf<AppMemoryInfo>()

        for (i in runningProcesses.indices) {
            val process = runningProcesses[i]
            val memInfo = processMemoryInfos.getOrNull(i)

            val pssKb = memInfo?.totalPss ?: 0
            val pssBytes = pssKb * 1024L

            if (pssBytes > 10 * 1024 * 1024L) { // Only show apps > 10 MB
                val packageName = process.pkgList.firstOrNull() ?: process.processName
                val appName = try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    packageName
                }

                val status = if (memInfo != null && pssKb > 0) "Measured" else "Estimated"

                appMemoryList.add(
                    AppMemoryInfo(
                        packageName = packageName,
                        appName = appName,
                        usedBytes = pssBytes,
                        statusString = status
                    )
                )
            }
        }

        appMemoryList.sortedByDescending { it.usedBytes }.take(25)
    }
}
