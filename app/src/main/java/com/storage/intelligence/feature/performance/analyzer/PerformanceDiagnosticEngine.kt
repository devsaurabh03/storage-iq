package com.storage.intelligence.feature.performance.analyzer

import com.storage.intelligence.feature.performance.model.*
import javax.inject.Inject
import javax.inject.Singleton

data class DiagnosticResult(
    val title: String,
    val summary: String,
    val findings: List<PerformanceFinding>
)

@Singleton
class PerformanceDiagnosticEngine @Inject constructor() {

    fun diagnoseWhyPhoneIsSlow(
        snapshot: PerformanceSnapshot,
        activeApps: List<String>
    ): DiagnosticResult {
        val findings = mutableListOf<PerformanceFinding>()

        // 1. Check Storage Pressure
        val storageUsedPercent = if (snapshot.storageTotalBytes > 0) {
            ((snapshot.storageUsedBytes.toDouble() / snapshot.storageTotalBytes) * 100).toInt()
        } else 0

        if (storageUsedPercent >= 90) {
            findings.add(
                PerformanceFinding(
                    id = "slow_storage_pressure",
                    type = FindingType.HIGH_STORAGE_PRESSURE,
                    severity = Severity.CRITICAL,
                    confidence = Confidence.HIGH,
                    title = "Critical Storage Pressure ($storageUsedPercent% used)",
                    description = "When internal storage is nearly full, system swap, app cache expansion, and temp file creation slow down.",
                    evidence = listOf(
                        "Storage usage: $storageUsedPercent%",
                        "High file system fragmentation risk"
                    ),
                    actionText = "Free Up Storage",
                    actionRoute = "cleanup"
                )
            )
        }

        // 2. Check Thermal Throttling
        if (snapshot.thermalInfo.isThrottlingDetected || snapshot.thermalInfo.state >= ThermalState.HOT) {
            findings.add(
                PerformanceFinding(
                    id = "slow_thermal_throttling",
                    type = FindingType.THERMAL_THROTTLING,
                    severity = Severity.WARNING,
                    confidence = snapshot.thermalInfo.confidence,
                    title = "Thermal Throttling Active",
                    description = "Your device is warm (${snapshot.thermalInfo.batteryTempCelsius}°C). Android reduces CPU max clock frequency to prevent overheating.",
                    evidence = listOf(
                        "Thermal state: ${snapshot.thermalInfo.state.displayName}",
                        "Battery temperature: ${snapshot.thermalInfo.batteryTempCelsius}°C",
                        "Frequency reduction active"
                    ),
                    actionText = "View CPU Details",
                    actionRoute = "cpu_intelligence"
                )
            )
        }

        // 3. Check CPU Activity
        if (snapshot.cpuActivity.currentLoadPercent >= 75) {
            findings.add(
                PerformanceFinding(
                    id = "slow_high_cpu",
                    type = FindingType.HIGH_CPU_ACTIVITY,
                    severity = Severity.WARNING,
                    confidence = Confidence.HIGH,
                    title = "Elevated CPU Load (${snapshot.cpuActivity.currentLoadPercent}%)",
                    description = "Active processes are consuming significant processor bandwidth, leading to UI touch latency or lag.",
                    evidence = listOf(
                        "Current CPU load: ${snapshot.cpuActivity.currentLoadPercent}%",
                        "Peak load: ${snapshot.cpuActivity.peak60sPercent}%"
                    ),
                    actionText = "Check Active Apps",
                    actionRoute = "apps"
                )
            )
        }

        // 4. Check RAM Pressure
        if (snapshot.ramInfo.pressure == RamPressure.HIGH) {
            findings.add(
                PerformanceFinding(
                    id = "slow_high_ram",
                    type = FindingType.HIGH_RAM_PRESSURE,
                    severity = Severity.WARNING,
                    confidence = Confidence.HIGH,
                    title = "High Memory Pressure",
                    description = "System memory is constrained. Android is caching heavily to avoid app relaunches.",
                    evidence = listOf(
                        "RAM used: ${(snapshot.ramInfo.usedBytes / (1024 * 1024 * 1024.0)).toString().take(4)} GB",
                        "Available RAM: ${(snapshot.ramInfo.availableBytes / (1024 * 1024 * 1024.0)).toString().take(4)} GB"
                    ),
                    actionText = "Review Active Apps",
                    actionRoute = "apps"
                )
            )
        }

        // 5. Check Background Apps
        if (activeApps.size >= 5) {
            findings.add(
                PerformanceFinding(
                    id = "slow_background_apps",
                    type = FindingType.BACKGROUND_ACTIVITY,
                    severity = Severity.INFO,
                    confidence = Confidence.MEDIUM,
                    title = "${activeApps.size} Active Background Apps",
                    description = "Multiple background services (syncing, media buffering, notifications) are active simultaneously.",
                    evidence = listOf(
                        "Active apps: ${activeApps.take(4).joinToString(", ")}..."
                    ),
                    actionText = "App Settings",
                    actionRoute = "apps"
                )
            )
        }

        if (findings.isEmpty()) {
            findings.add(
                PerformanceFinding(
                    id = "slow_no_issue",
                    type = FindingType.NO_SIGNIFICANT_ISSUE,
                    severity = Severity.INFO,
                    confidence = Confidence.HIGH,
                    title = "System Performance is Optimal",
                    description = "CPU load, memory pressure, thermal levels, and storage headroom are all within healthy operating ranges.",
                    evidence = listOf(
                        "CPU Load: ${snapshot.cpuActivity.currentLoadPercent}%",
                        "Thermal State: ${snapshot.thermalInfo.state.displayName}",
                        "Storage Used: $storageUsedPercent%"
                    )
                )
            )
        }

        return DiagnosticResult(
            title = "Why is My Phone Slow?",
            summary = if (findings.any { it.severity == Severity.CRITICAL || it.severity == Severity.WARNING }) {
                "${findings.size} likely contributor(s) identified affecting device responsiveness."
            } else {
                "No major performance bottlenecks detected."
            },
            findings = findings
        )
    }

    fun diagnoseWhyPhoneIsHot(
        snapshot: PerformanceSnapshot,
        activeApps: List<String>
    ): DiagnosticResult {
        val findings = mutableListOf<PerformanceFinding>()

        // 1. High Thermal State / Battery Temp
        if (snapshot.thermalInfo.state >= ThermalState.WARM || snapshot.batteryInfo.tempCelsius >= 36.5) {
            findings.add(
                PerformanceFinding(
                    id = "hot_temp_elevated",
                    type = FindingType.THERMAL_WARNING,
                    severity = if (snapshot.thermalInfo.state >= ThermalState.HOT) Severity.CRITICAL else Severity.WARNING,
                    confidence = Confidence.HIGH,
                    title = "Elevated Device Temperature (${snapshot.batteryInfo.tempCelsius}°C)",
                    description = "The battery and processor temperature is elevated to ${snapshot.thermalInfo.state.displayName}.",
                    evidence = listOf(
                        "Battery Temperature: ${snapshot.batteryInfo.tempCelsius}°C",
                        "Thermal State: ${snapshot.thermalInfo.state.displayName}"
                    )
                )
            )
        }

        // 2. Charging while under compute load
        if (snapshot.batteryInfo.isCharging) {
            findings.add(
                PerformanceFinding(
                    id = "hot_charging_active",
                    type = FindingType.THERMAL_WARNING,
                    severity = Severity.INFO,
                    confidence = Confidence.HIGH,
                    title = "Device is Charging",
                    description = "Fast charging generates heat inside the battery and power delivery IC. Using intensive apps while charging increases warmth.",
                    evidence = listOf(
                        "Charging Status: ${snapshot.batteryInfo.status}",
                        "Battery Level: ${snapshot.batteryInfo.levelPercent}%"
                    )
                )
            )
        }

        // 3. Sustained CPU compute load
        if (snapshot.cpuActivity.currentLoadPercent >= 65) {
            findings.add(
                PerformanceFinding(
                    id = "hot_high_cpu",
                    type = FindingType.HIGH_CPU_ACTIVITY,
                    severity = Severity.WARNING,
                    confidence = Confidence.HIGH,
                    title = "Heavy CPU Workload (${snapshot.cpuActivity.currentLoadPercent}%)",
                    description = "High CPU frequency operation dissipates power as thermal energy.",
                    evidence = listOf(
                        "CPU Load: ${snapshot.cpuActivity.currentLoadPercent}%"
                    ),
                    actionText = "Check CPU",
                    actionRoute = "cpu_intelligence"
                )
            )
        }

        // 4. Background Sync / Media Processing
        if (activeApps.isNotEmpty()) {
            findings.add(
                PerformanceFinding(
                    id = "hot_background_sync",
                    type = FindingType.BACKGROUND_ACTIVITY,
                    severity = Severity.INFO,
                    confidence = Confidence.MEDIUM,
                    title = "Background Activity Detected",
                    description = "Background synchronization, cloud backups, or media processing generate continuous CPU cycles.",
                    evidence = listOf(
                        "Active apps: ${activeApps.take(3).joinToString(", ")}"
                    )
                )
            )
        }

        if (findings.isEmpty()) {
            findings.add(
                PerformanceFinding(
                    id = "hot_normal",
                    type = FindingType.NO_SIGNIFICANT_ISSUE,
                    severity = Severity.INFO,
                    confidence = Confidence.HIGH,
                    title = "Normal Thermal State",
                    description = "Device temperature is normal (${snapshot.batteryInfo.tempCelsius}°C) with no thermal stress detected.",
                    evidence = listOf("Battery Temp: ${snapshot.batteryInfo.tempCelsius}°C")
                )
            )
        }

        return DiagnosticResult(
            title = "Why is My Phone Hot?",
            summary = "Thermal analysis completed. Review identified heat sources.",
            findings = findings
        )
    }
}
