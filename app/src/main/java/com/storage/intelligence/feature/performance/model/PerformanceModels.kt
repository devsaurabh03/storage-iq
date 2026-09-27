package com.storage.intelligence.feature.performance.model

enum class CpuVendor(val displayName: String) {
    QUALCOMM("Qualcomm"),
    MEDIATEK("MediaTek"),
    SAMSUNG_EXYNOS("Samsung Exynos"),
    GOOGLE_TENSOR("Google Tensor"),
    UNISOC("UNISOC"),
    INTEL("Intel"),
    AMD("AMD"),
    OTHER("Generic / Unknown")
}

enum class CpuArchitecture(val displayName: String) {
    ARMv7("ARMv7 (32-bit)"),
    ARMv8_A("ARMv8-A (64-bit)"),
    ARMv9_A("ARMv9-A (64-bit)"),
    X86_64("x86_64"),
    UNKNOWN("Unknown Architecture")
}

data class CpuCluster(
    val clusterId: Int,
    val coreCount: Int,
    val minFreqKHz: Long,
    val maxFreqKHz: Long,
    val curFreqKHz: Long
)

data class CpuIdentity(
    val vendor: CpuVendor,
    val socName: String,
    val architecture: CpuArchitecture,
    val abi: String,
    val is64Bit: Boolean,
    val logicalCoreCount: Int,
    val topologyType: String, // e.g., "Heterogeneous (Big.LITTLE / Tri-Cluster)"
    val clusters: List<CpuCluster>,
    val supportedFeatures: List<String>
)

data class CpuActivity(
    val currentLoadPercent: Int,
    val average60sPercent: Int,
    val peak60sPercent: Int,
    val elevatedActivityDurationSeconds: Long,
    val trend: String // "INCREASING", "STABLE", "DECREASING"
)

enum class RamPressure(val displayName: String) {
    LOW("Low Pressure"),
    MODERATE("Moderate Pressure"),
    HIGH("High Pressure")
}

data class RamInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
    val thresholdBytes: Long,
    val isLowMemory: Boolean,
    val pressure: RamPressure
)

data class AppMemoryInfo(
    val packageName: String,
    val appName: String,
    val usedBytes: Long,
    val statusString: String // "Measured", "Estimated", "Unavailable"
)

enum class ThermalState(val displayName: String) {
    NORMAL("Normal"),
    WARM("Warm"),
    HOT("Hot"),
    VERY_HOT("Very Hot"),
    CRITICAL("Critical")
}

enum class Confidence {
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN
}

data class ThermalInfo(
    val state: ThermalState,
    val batteryTempCelsius: Double,
    val isThrottlingDetected: Boolean,
    val confidence: Confidence
)

data class BatteryInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
    val status: String,
    val tempCelsius: Double
)

data class PerformanceSnapshot(
    val timestampMs: Long,
    val ramInfo: RamInfo,
    val cpuActivity: CpuActivity,
    val thermalInfo: ThermalInfo,
    val batteryInfo: BatteryInfo,
    val storageUsedBytes: Long,
    val storageTotalBytes: Long,
    val activeAppCount: Int
)

enum class FindingType {
    HIGH_CPU_ACTIVITY,
    HIGH_RAM_PRESSURE,
    THERMAL_WARNING,
    THERMAL_THROTTLING,
    HIGH_STORAGE_PRESSURE,
    BACKGROUND_ACTIVITY,
    CPU_FREQUENCY_REDUCTION,
    PERFORMANCE_DEGRADATION,
    NO_SIGNIFICANT_ISSUE
}

enum class Severity {
    INFO,
    WARNING,
    CRITICAL
}

data class PerformanceFinding(
    val id: String,
    val type: FindingType,
    val severity: Severity,
    val confidence: Confidence,
    val title: String,
    val description: String,
    val evidence: List<String>,
    val actionText: String? = null,
    val actionRoute: String? = null
)

data class CpuHealthTestResult(
    val durationSeconds: Int,
    val singleThreadScore: Int,
    val multiThreadScore: Int,
    val frequencyStabilityPercent: Int,
    val thermalResponseState: ThermalState,
    val performanceConsistencyPercent: Int,
    val isThrottledDuringTest: Boolean,
    val comparisonWithBaselinePercent: Int
)
