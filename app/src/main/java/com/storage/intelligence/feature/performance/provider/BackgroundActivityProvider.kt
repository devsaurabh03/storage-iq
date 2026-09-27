package com.storage.intelligence.feature.performance.provider

import android.app.ActivityManager
import android.content.Context
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
        val runningProcesses = am.runningAppProcesses ?: return@withContext emptyList()
        val pm = context.packageManager

        val result = mutableListOf<BackgroundAppItem>()

        for (process in runningProcesses) {
            // Ignore system / self
            val mainPkg = process.pkgList?.firstOrNull() ?: process.processName
            if (process.processName.startsWith("com.android.") ||
                process.processName.startsWith("system") ||
                mainPkg == context.packageName ||
                process.processName.startsWith(context.packageName)
            ) {
                continue
            }

            if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND ||
                process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
            ) {
                val pkgName = process.pkgList.firstOrNull() ?: process.processName
                val appName = try {
                    val appInfo = pm.getApplicationInfo(pkgName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    pkgName
                }

                val desc = when {
                    pkgName.contains("whatsapp") -> "Background messaging & media sync"
                    pkgName.contains("telegram") -> "Background media download"
                    pkgName.contains("spotify") || pkgName.contains("music") -> "Audio playback service"
                    pkgName.contains("google.android.apps.photos") -> "Media cloud backup"
                    else -> "Recent background process"
                }

                result.add(BackgroundAppItem(pkgName, appName, desc))
            }
        }

        result.distinctBy { it.packageName }.take(10)
    }
}
