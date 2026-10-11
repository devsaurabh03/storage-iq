package com.storage.intelligence.feature.performance.provider

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class BackgroundAppItem(
    val packageName: String,
    val appName: String,
    val description: String
)

@Singleton
class BackgroundActivityProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getBackgroundApps(): List<BackgroundAppItem> = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = context.packageManager
        val selfPackage = context.packageName
        val result = mutableListOf<BackgroundAppItem>()

        // 1. Check running processes
        val runningProcesses = try { am.runningAppProcesses } catch (_: Exception) { null }
        if (!runningProcesses.isNullOrEmpty()) {
            for (process in runningProcesses) {
                val mainPkg = process.pkgList?.firstOrNull() ?: process.processName
                if (process.processName.startsWith("com.android.") ||
                    process.processName.startsWith("system") ||
                    mainPkg == selfPackage ||
                    process.processName.startsWith(selfPackage)
                ) {
                    continue
                }

                if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND ||
                    process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
                ) {
                    val pkgName = process.pkgList?.firstOrNull() ?: process.processName
                    val appName = try {
                        val appInfo = pm.getApplicationInfo(pkgName, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        pkgName
                    }

                    result.add(BackgroundAppItem(pkgName, appName, formatDescription(pkgName)))
                }
            }
        }

        // 2. Check UsageStatsManager for recent background/active apps
        if (result.size < 5) {
            try {
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                if (usageStatsManager != null) {
                    val now = System.currentTimeMillis()
                    val startTime = now - (3L * 60L * 60L * 1000L)
                    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, now)
                    if (!stats.isNullOrEmpty()) {
                        val sorted = stats
                            .filter {
                                val p = it.packageName
                                p != selfPackage && !p.startsWith("$selfPackage.") && !p.startsWith("com.android.systemui") && p != "android" && it.lastTimeUsed >= startTime
                            }
                            .sortedByDescending { it.lastTimeUsed }

                        for (stat in sorted) {
                            val pkgName = stat.packageName
                            if (result.none { it.packageName == pkgName }) {
                                val appName = try {
                                    val appInfo = pm.getApplicationInfo(pkgName, 0)
                                    pm.getApplicationLabel(appInfo).toString()
                                } catch (_: Exception) {
                                    pkgName
                                }
                                result.add(BackgroundAppItem(pkgName, appName, formatDescription(pkgName)))
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // 3. Fallback to launchable user apps
        if (result.size < 5) {
            try {
                val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
                for (resolve in resolveInfos) {
                    val pkgName = resolve.activityInfo.packageName
                    if (pkgName == selfPackage || pkgName.startsWith("com.android.") || pkgName == "android") continue

                    val appInfo = resolve.activityInfo.applicationInfo
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                    if (!isSystem || isUpdatedSystem) {
                        if (result.none { it.packageName == pkgName }) {
                            val appName = pm.getApplicationLabel(appInfo).toString()
                            result.add(BackgroundAppItem(pkgName, appName, formatDescription(pkgName)))
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        result.distinctBy { it.packageName }.take(15)
    }

    private fun formatDescription(pkgName: String): String {
        val lower = pkgName.lowercase()
        return when {
            lower.contains("whatsapp") -> "Background messaging & media sync"
            lower.contains("telegram") -> "Background media download"
            lower.contains("spotify") || lower.contains("music") -> "Audio playback service"
            lower.contains("google.android.apps.photos") -> "Media cloud backup"
            lower.contains("chrome") || lower.contains("browser") -> "Cached web process"
            lower.contains("instagram") || lower.contains("facebook") -> "Social background sync"
            else -> "Recent background process"
        }
    }
}
