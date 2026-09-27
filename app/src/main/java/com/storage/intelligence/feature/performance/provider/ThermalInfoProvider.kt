package com.storage.intelligence.feature.performance.provider

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.storage.intelligence.feature.performance.model.Confidence
import com.storage.intelligence.feature.performance.model.ThermalInfo
import com.storage.intelligence.feature.performance.model.ThermalState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getThermalInfo(cpuLoadPercent: Int = 0): ThermalInfo = withContext(Dispatchers.IO) {
        val batteryTempCelsius = getBatteryTemperature()
        var systemThermalState = ThermalState.NORMAL
        var confidence = Confidence.MEDIUM

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                val status = powerManager.currentThermalStatus
                systemThermalState = mapPowerManagerThermalStatus(status)
                confidence = Confidence.HIGH
            }
        }

        // If system thermal API is unavailable or normal, correlate with battery temp
        if (systemThermalState == ThermalState.NORMAL) {
            systemThermalState = when {
                batteryTempCelsius >= 45.0 -> ThermalState.VERY_HOT
                batteryTempCelsius >= 40.0 -> ThermalState.HOT
                batteryTempCelsius >= 36.5 -> ThermalState.WARM
                else -> ThermalState.NORMAL
            }
        }

        val isThrottlingDetected = (systemThermalState >= ThermalState.HOT) ||
                (systemThermalState == ThermalState.WARM && cpuLoadPercent > 75)

        ThermalInfo(
            state = systemThermalState,
            batteryTempCelsius = batteryTempCelsius,
            isThrottlingDetected = isThrottlingDetected,
            confidence = confidence
        )
    }

    private fun getBatteryTemperature(): Double {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            if (temp > 0) temp / 10.0 else 28.5
        } catch (_: Exception) {
            28.5
        }
    }

    private fun mapPowerManagerThermalStatus(status: Int): ThermalState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalState.NORMAL
        return when (status) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalState.NORMAL
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.WARM
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.WARM
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.HOT
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalState.VERY_HOT
            PowerManager.THERMAL_STATUS_EMERGENCY, PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.CRITICAL
            else -> ThermalState.NORMAL
        }
    }
}
