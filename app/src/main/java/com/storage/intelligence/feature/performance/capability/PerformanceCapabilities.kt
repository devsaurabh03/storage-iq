package com.storage.intelligence.feature.performance.capability

import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceCapabilities @Inject constructor() {

    fun canReadThermalStatus(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    fun canReadMemoryInfo(): Boolean {
        return true
    }

    fun canReadCpuInfo(): Boolean {
        return true
    }

    fun canReadCpuFrequency(): Boolean {
        return try {
            val freqFile = java.io.File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
            freqFile.exists() && freqFile.canRead()
        } catch (_: Exception) {
            false
        }
    }

    fun canReadCpuTopology(): Boolean {
        return Runtime.getRuntime().availableProcessors() > 0
    }

    fun canReadAppMemory(): Boolean {
        return true
    }

    fun canReadAppCpuActivity(): Boolean {
        // Android O (API 26+) restricted /proc access for per-app CPU usage
        return false
    }

    fun canOpenAppInfo(): Boolean {
        return true
    }

    fun canOpenBatterySettings(): Boolean {
        return true
    }

    fun canOpenStorageSettings(): Boolean {
        return true
    }
}
