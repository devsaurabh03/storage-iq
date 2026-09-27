package com.storage.intelligence.feature.performance.analyzer

import com.storage.intelligence.feature.performance.model.CpuActivity
import com.storage.intelligence.feature.performance.model.CpuIdentity
import com.storage.intelligence.feature.performance.model.ThermalInfo
import com.storage.intelligence.feature.performance.model.ThermalState
import javax.inject.Inject
import javax.inject.Singleton

enum class CpuBehaviorFinding {
    NORMAL_CPU_BEHAVIOR,
    HIGH_CPU_ACTIVITY,
    THERMAL_THROTTLING,
    FREQUENCY_REDUCTION,
    SUSTAINED_LOAD,
    UNUSUAL_CPU_ACTIVITY,
    INSUFFICIENT_DATA
}

data class CpuAnalysisReport(
    val finding: CpuBehaviorFinding,
    val summary: String,
    val evidence: List<String>,
    val scalingActive: Boolean
)

@Singleton
class CpuBehaviorEngine @Inject constructor() {

    fun analyzeCpuBehavior(
        identity: CpuIdentity,
        activity: CpuActivity,
        thermalInfo: ThermalInfo
    ): CpuAnalysisReport {
        val evidence = mutableListOf<String>()
        evidence.add("Target SoC: ${identity.socName} (${identity.vendor.displayName})")
        evidence.add("Logical Cores: ${identity.logicalCoreCount}")
        evidence.add("Current Load: ${activity.currentLoadPercent}%")

        val finding = when {
            thermalInfo.isThrottlingDetected -> {
                evidence.add("Thermal state: ${thermalInfo.state.displayName}")
                evidence.add("CPU frequency scaling throttled to control temperature")
                CpuBehaviorFinding.THERMAL_THROTTLING
            }
            activity.currentLoadPercent > 80 -> {
                evidence.add("High sustained compute load detected across cores")
                CpuBehaviorFinding.HIGH_CPU_ACTIVITY
            }
            thermalInfo.state >= ThermalState.WARM -> {
                evidence.add("Thermal state elevated to ${thermalInfo.state.displayName}")
                CpuBehaviorFinding.FREQUENCY_REDUCTION
            }
            activity.trend == "INCREASING" && activity.currentLoadPercent > 60 -> {
                evidence.add("CPU workload increasing rapidly")
                CpuBehaviorFinding.SUSTAINED_LOAD
            }
            else -> {
                evidence.add("CPU workload matches expected frequency scaling curve")
                CpuBehaviorFinding.NORMAL_CPU_BEHAVIOR
            }
        }

        val summary = when (finding) {
            CpuBehaviorFinding.NORMAL_CPU_BEHAVIOR -> "CPU operates within normal parameters for ${identity.socName}."
            CpuBehaviorFinding.HIGH_CPU_ACTIVITY -> "Elevated CPU load (${activity.currentLoadPercent}%). Active tasks are consuming CPU capacity."
            CpuBehaviorFinding.THERMAL_THROTTLING -> "Thermal management is reducing CPU frequencies to control temperature."
            CpuBehaviorFinding.FREQUENCY_REDUCTION -> "Device temperature is elevated. CPU frequency scaling is active."
            CpuBehaviorFinding.SUSTAINED_LOAD -> "Sustained compute activity observed on ${identity.logicalCoreCount} cores."
            CpuBehaviorFinding.UNUSUAL_CPU_ACTIVITY -> "Unusual background compute pattern detected."
            CpuBehaviorFinding.INSUFFICIENT_DATA -> "Collecting performance telemetry data..."
        }

        return CpuAnalysisReport(
            finding = finding,
            summary = summary,
            evidence = evidence,
            scalingActive = true
        )
    }
}
