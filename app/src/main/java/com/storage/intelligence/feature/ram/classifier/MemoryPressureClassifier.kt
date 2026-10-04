package com.storage.intelligence.feature.ram.classifier

import com.storage.intelligence.feature.ram.model.MemoryPressure
import com.storage.intelligence.feature.ram.model.SystemMemorySnapshot
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Classifies device memory pressure using Android's system memory signals:
 * available bytes, threshold bytes, lowMemory flag, and used memory ratio.
 */
@Singleton
class MemoryPressureClassifier @Inject constructor() {

    /**
     * Classifies memory pressure.
     *
     * Thresholds:
     * - CRITICAL: lowMemory flag is true OR available RAM <= Android threshold OR used RAM >= 92%
     * - HIGH: available RAM <= 1.5 * threshold OR used RAM >= 83%
     * - MODERATE: available RAM <= 2.2 * threshold OR used RAM >= 70%
     * - LOW (Healthy): otherwise
     */
    fun classify(snapshot: SystemMemorySnapshot): MemoryPressure {
        if (snapshot.totalBytes <= 0) return MemoryPressure.LOW

        val usedBytes = (snapshot.totalBytes - snapshot.availableBytes).coerceAtLeast(0L)
        val usedPercent = (usedBytes.toDouble() / snapshot.totalBytes) * 100

        return when {
            snapshot.lowMemory || snapshot.availableBytes <= snapshot.thresholdBytes || usedPercent >= 92.0 -> {
                MemoryPressure.CRITICAL
            }
            snapshot.availableBytes <= (snapshot.thresholdBytes * 1.5).toLong() || usedPercent >= 83.0 -> {
                MemoryPressure.HIGH
            }
            snapshot.availableBytes <= (snapshot.thresholdBytes * 2.2).toLong() || usedPercent >= 70.0 -> {
                MemoryPressure.MODERATE
            }
            else -> {
                MemoryPressure.LOW
            }
        }
    }
}
