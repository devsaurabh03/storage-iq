package com.storage.intelligence.feature.ram.mapper

import android.app.ActivityManager
import com.storage.intelligence.feature.ram.model.ProcessState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps Android process importance constants (ActivityManager.RunningAppProcessInfo)
 * to StorageIQ ProcessState abstraction.
 */
@Singleton
class ProcessStateMapper @Inject constructor() {

    fun mapImportanceToState(importance: Int): ProcessState {
        return when (importance) {
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> ProcessState.FOREGROUND
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE -> ProcessState.FOREGROUND_SERVICE
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING,
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> ProcessState.VISIBLE
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE,
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE -> ProcessState.PERCEPTIBLE
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> ProcessState.SERVICE
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> ProcessState.CACHED
            else -> ProcessState.UNKNOWN
        }
    }
}
