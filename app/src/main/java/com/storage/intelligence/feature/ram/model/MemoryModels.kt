package com.storage.intelligence.feature.ram.model

/**
 * System memory pressure classification levels.
 */
enum class MemoryPressure(val displayName: String) {
    LOW("Healthy"),
    MODERATE("Moderate memory pressure"),
    HIGH("High memory pressure"),
    CRITICAL("Memory pressure is high")
}

/**
 * Android process importance states mapped into a safe StorageIQ abstraction.
 */
enum class ProcessState(val displayName: String) {
    FOREGROUND("Foreground App"),
    FOREGROUND_SERVICE("Foreground Service Active"),
    VISIBLE("Visible Activity"),
    PERCEPTIBLE("Perceptible Activity"),
    SERVICE("Background Service"),
    CACHED("Cached Process"),
    UNKNOWN("Background Activity Detected")
}

/**
 * Snapshot of Android system memory from ActivityManager.MemoryInfo.
 */
data class SystemMemorySnapshot(
    val totalBytes: Long,
    val availableBytes: Long,
    val thresholdBytes: Long,
    val lowMemory: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0L)
    val usedPercent: Int get() = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0
}

/**
 * Signal representing an observable background process.
 * Per-app RAM numbers are intentionally not fabricated.
 */
data class AppProcessSignal(
    val packageName: String,
    val appName: String,
    val processState: ProcessState,
    val importance: Int,
    val pid: Int
)

/**
 * Historical memory pressure observation recorded locally.
 */
data class MemoryObservation(
    val id: Long = 0,
    val timestamp: Long,
    val totalBytes: Long,
    val availableBytes: Long,
    val thresholdBytes: Long,
    val lowMemory: Boolean,
    val pressure: MemoryPressure
)

/**
 * Evidence-based memory recommendation.
 */
data class MemoryRecommendation(
    val title: String,
    val description: String,
    val pressure: MemoryPressure
)
