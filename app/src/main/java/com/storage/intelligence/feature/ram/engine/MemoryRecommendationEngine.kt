package com.storage.intelligence.feature.ram.engine

import com.storage.intelligence.feature.ram.model.AppProcessSignal
import com.storage.intelligence.feature.ram.model.MemoryPressure
import com.storage.intelligence.feature.ram.model.MemoryRecommendation
import com.storage.intelligence.feature.ram.model.ProcessState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates evidence-based memory recommendations based on actual memory pressure
 * and observable background activity signals.
 */
@Singleton
class MemoryRecommendationEngine @Inject constructor() {

    fun generateRecommendation(
        pressure: MemoryPressure,
        backgroundSignals: List<AppProcessSignal>
    ): MemoryRecommendation {
        val activeFgServices = backgroundSignals.count { it.processState == ProcessState.FOREGROUND_SERVICE }
        val activeServices = backgroundSignals.count { it.processState == ProcessState.SERVICE }

        return when (pressure) {
            MemoryPressure.LOW -> MemoryRecommendation(
                title = "Memory looks healthy",
                description = "Memory looks healthy. Android is managing cached memory efficiently. No action needed.",
                pressure = MemoryPressure.LOW
            )
            MemoryPressure.MODERATE -> MemoryRecommendation(
                title = "Moderate memory pressure",
                description = if (activeFgServices > 0) {
                    "$activeFgServices active foreground service(s) detected. Android can reclaim cached memory automatically when needed."
                } else {
                    "Some background activity is present. Android can reclaim cached memory automatically."
                },
                pressure = MemoryPressure.MODERATE
            )
            MemoryPressure.HIGH -> MemoryRecommendation(
                title = "High memory pressure",
                description = "Memory pressure is high ($activeServices active background services). Review background activity if your device feels slow.",
                pressure = MemoryPressure.HIGH
            )
            MemoryPressure.CRITICAL -> MemoryRecommendation(
                title = "Memory pressure is high",
                description = "Android reports low memory conditions. Review background apps and system settings.",
                pressure = MemoryPressure.CRITICAL
            )
        }
    }
}
