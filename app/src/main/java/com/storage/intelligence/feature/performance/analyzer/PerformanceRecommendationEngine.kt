package com.storage.intelligence.feature.performance.analyzer

import com.storage.intelligence.feature.performance.model.PerformanceFinding
import com.storage.intelligence.feature.performance.model.PerformanceSnapshot
import com.storage.intelligence.feature.performance.model.RamPressure
import com.storage.intelligence.feature.performance.model.ThermalState
import javax.inject.Inject
import javax.inject.Singleton

data class RecommendationItem(
    val title: String,
    val description: String,
    val actionText: String,
    val actionRoute: String
)

@Singleton
class PerformanceRecommendationEngine @Inject constructor() {

    fun generateRecommendations(
        snapshot: PerformanceSnapshot,
        findings: List<PerformanceFinding>
    ): List<RecommendationItem> {
        val recommendations = mutableListOf<RecommendationItem>()

        val storageUsedPercent = if (snapshot.storageTotalBytes > 0) {
            ((snapshot.storageUsedBytes.toDouble() / snapshot.storageTotalBytes) * 100).toInt()
        } else 0

        // Storage recommendation
        if (storageUsedPercent >= 90) {
            val potentialGb = 14.5
            recommendations.add(
                RecommendationItem(
                    title = "Storage Almost Full ($storageUsedPercent%)",
                    description = "Freeing space reduces swap file thrashing and improves app launch performance.",
                    actionText = "Free Up Space ($potentialGb GB)",
                    actionRoute = "cleanup"
                )
            )
        }

        // Thermal recommendation
        if (snapshot.thermalInfo.state >= ThermalState.WARM || snapshot.thermalInfo.isThrottlingDetected) {
            recommendations.add(
                RecommendationItem(
                    title = "Device Running Warm (${snapshot.batteryInfo.tempCelsius}°C)",
                    description = "CPU frequency scaling is reduced to maintain safe thermal limits. Allow device to cool.",
                    actionText = "View CPU Details",
                    actionRoute = "cpu_intelligence"
                )
            )
        }

        // RAM / App recommendation
        if (snapshot.ramInfo.pressure == RamPressure.HIGH || snapshot.activeAppCount >= 4) {
            recommendations.add(
                RecommendationItem(
                    title = "${snapshot.activeAppCount} Active Apps Consuming RAM",
                    description = "Review apps with active background services or high process memory footprint.",
                    actionText = "Review Active Apps",
                    actionRoute = "apps"
                )
            )
        }

        return recommendations
    }
}
