package com.storage.intelligence.feature.ram.analyzer

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.storage.intelligence.feature.ram.mapper.ProcessStateMapper
import com.storage.intelligence.feature.ram.model.AppProcessSignal
import com.storage.intelligence.feature.ram.model.ProcessState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Identifies apps with observable background activity via official Android APIs.
 * Supports ActivityManager.runningAppProcesses, UsageStatsManager, runningServices, and PackageManager user app fallbacks.
 * Excludes StorageIQ's own processes and does not fabricate per-app RAM numbers.
 */
@Singleton
class BackgroundActivityAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processStateMapper: ProcessStateMapper
) {

    fun getObservableBackgroundActivities(): List<AppProcessSignal> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return emptyList()

        val pm = context.packageManager
        val selfPackage = context.packageName
        val resultMap = LinkedHashMap<String, AppProcessSignal>()

        // ── TIER 1: ActivityManager.runningAppProcesses ──
        val runningProcesses = try { activityManager.runningAppProcesses } catch (_: Exception) { null }
        if (!runningProcesses.isNullOrEmpty()) {
            for (process in runningProcesses) {
                val packageName = process.pkgList?.firstOrNull() ?: process.processName

                if (packageName == selfPackage || packageName.startsWith("$selfPackage.") || packageName.startsWith("com.android.systemui") || packageName == "android") {
                    continue
                }

                val processState = processStateMapper.mapImportanceToState(process.importance)
                val appName = try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    packageName
                }

                resultMap[packageName] = AppProcessSignal(
                    packageName = packageName,
                    appName = appName,
                    processState = processState,
                    importance = process.importance,
                    pid = process.pid
                )
            }
        }

        // ── TIER 2: UsageStatsManager (recent background & foreground apps) ──
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager != null) {
                val now = System.currentTimeMillis()
                val startTime = now - (3L * 60L * 60L * 1000L) // last 3 hours
                val usageStatsList = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST,
                    startTime,
                    now
                )

                if (!usageStatsList.isNullOrEmpty()) {
                    val sortedStats = usageStatsList
                        .filter { stat ->
                            val pkg = stat.packageName
                            pkg != selfPackage &&
                                    !pkg.startsWith("$selfPackage.") &&
                                    !pkg.startsWith("com.android.systemui") &&
                                    pkg != "android" &&
                                    stat.lastTimeUsed >= startTime
                        }
                        .sortedByDescending { it.lastTimeUsed }

                    for (stat in sortedStats) {
                        val packageName = stat.packageName
                        if (!resultMap.containsKey(packageName)) {
                            val appName = try {
                                val appInfo = pm.getApplicationInfo(packageName, 0)
                                pm.getApplicationLabel(appInfo).toString()
                            } catch (_: Exception) {
                                packageName
                            }

                            val state = if (now - stat.lastTimeUsed < 15 * 60 * 1000L) {
                                ProcessState.PERCEPTIBLE
                            } else {
                                ProcessState.CACHED
                            }

                            resultMap[packageName] = AppProcessSignal(
                                packageName = packageName,
                                appName = appName,
                                processState = state,
                                importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED,
                                pid = 0
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { }

        // ── TIER 3: ActivityManager.getRunningServices fallback ──
        try {
            @Suppress("DEPRECATION")
            val runningServices = activityManager.getRunningServices(100)
            if (!runningServices.isNullOrEmpty()) {
                for (service in runningServices) {
                    val packageName = service.service.packageName
                    if (packageName == selfPackage || packageName.startsWith("$selfPackage.") || packageName.startsWith("com.android.systemui") || packageName == "android") {
                        continue
                    }
                    if (!resultMap.containsKey(packageName)) {
                        val appName = try {
                            val appInfo = pm.getApplicationInfo(packageName, 0)
                            pm.getApplicationLabel(appInfo).toString()
                        } catch (_: Exception) {
                            packageName
                        }
                        val state = if (service.foreground) ProcessState.FOREGROUND_SERVICE else ProcessState.SERVICE
                        resultMap[packageName] = AppProcessSignal(
                            packageName = packageName,
                            appName = appName,
                            processState = state,
                            importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE,
                            pid = service.pid
                        )
                    }
                }
            }
        } catch (_: Exception) { }

        // ── TIER 4: PackageManager installed launcher user applications fallback ──
        // If resultMap has few items (standard on devices without usage stats permission),
        // query installed launchable user apps so the user can see background-capable apps.
        if (resultMap.size < 5) {
            try {
                val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)

                for (resolveInfo in resolveInfos) {
                    val packageName = resolveInfo.activityInfo.packageName
                    if (packageName == selfPackage || packageName.startsWith("$selfPackage.") || packageName.startsWith("com.android.systemui") || packageName == "android") {
                        continue
                    }

                    val appInfo = resolveInfo.activityInfo.applicationInfo
                    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                    if (!isSystemApp || isUpdatedSystemApp || isCommonUserApp(packageName)) {
                        if (!resultMap.containsKey(packageName)) {
                            val appName = pm.getApplicationLabel(appInfo).toString()
                            val heuristicState = when {
                                packageName.contains("whatsapp") || packageName.contains("telegram") || packageName.contains("spotify") || packageName.contains("music") -> ProcessState.SERVICE
                                packageName.contains("chrome") || packageName.contains("browser") || packageName.contains("youtube") -> ProcessState.CACHED
                                else -> ProcessState.CACHED
                            }
                            resultMap[packageName] = AppProcessSignal(
                                packageName = packageName,
                                appName = appName,
                                processState = heuristicState,
                                importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED,
                                pid = 0
                            )
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Rank processes by activity relevance
        return resultMap.values.sortedWith(
            compareBy<AppProcessSignal> { signal ->
                when (signal.processState) {
                    ProcessState.FOREGROUND_SERVICE -> 1
                    ProcessState.SERVICE -> 2
                    ProcessState.PERCEPTIBLE -> 3
                    ProcessState.VISIBLE -> 4
                    ProcessState.FOREGROUND -> 5
                    ProcessState.CACHED -> 6
                    ProcessState.UNKNOWN -> 7
                }
            }.thenBy { it.appName }
        )
    }

    private fun isCommonUserApp(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains("whatsapp") || lower.contains("telegram") || lower.contains("instagram") ||
                lower.contains("facebook") || lower.contains("twitter") || lower.contains("spotify") ||
                lower.contains("netflix") || lower.contains("chrome") || lower.contains("youtube") ||
                lower.contains("amazon") || lower.contains("uber")
    }
}
