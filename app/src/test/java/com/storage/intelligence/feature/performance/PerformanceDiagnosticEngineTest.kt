package com.storage.intelligence.feature.performance

import com.storage.intelligence.feature.performance.analyzer.PerformanceDiagnosticEngine
import com.storage.intelligence.feature.performance.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceDiagnosticEngineTest {

    private val engine = PerformanceDiagnosticEngine()

    @Test
    fun testDiagnoseWhyPhoneIsSlow_CriticalStoragePressure() {
        val snapshot = PerformanceSnapshot(
            timestampMs = System.currentTimeMillis(),
            ramInfo = RamInfo(8000000000L, 5000000000L, 3000000000L, 1000000000L, false, RamPressure.LOW),
            cpuActivity = CpuActivity(20, 20, 30, 0, "STABLE"),
            thermalInfo = ThermalInfo(ThermalState.NORMAL, 30.0, false, Confidence.HIGH),
            batteryInfo = BatteryInfo(80, false, "Discharging", 30.0),
            storageUsedBytes = 120000000000L,
            storageTotalBytes = 128000000000L, // ~93.7% used
            activeAppCount = 2
        )

        val result = engine.diagnoseWhyPhoneIsSlow(snapshot, listOf("Chrome", "WhatsApp"))
        val storageFinding = result.findings.find { it.type == FindingType.HIGH_STORAGE_PRESSURE }

        assertTrue(storageFinding != null)
        assertEquals(Severity.CRITICAL, storageFinding?.severity)
        assertEquals(Confidence.HIGH, storageFinding?.confidence)
    }

    @Test
    fun testDiagnoseWhyPhoneIsHot_ElevatedThermalAndCharging() {
        val snapshot = PerformanceSnapshot(
            timestampMs = System.currentTimeMillis(),
            ramInfo = RamInfo(8000000000L, 5000000000L, 3000000000L, 1000000000L, false, RamPressure.LOW),
            cpuActivity = CpuActivity(85, 80, 90, 100, "INCREASING"),
            thermalInfo = ThermalInfo(ThermalState.HOT, 41.5, true, Confidence.HIGH),
            batteryInfo = BatteryInfo(65, true, "Charging", 41.5),
            storageUsedBytes = 50000000000L,
            storageTotalBytes = 128000000000L,
            activeAppCount = 4
        )

        val result = engine.diagnoseWhyPhoneIsHot(snapshot, listOf("YouTube", "Camera", "Instagram", "WhatsApp"))
        val thermalFinding = result.findings.find { it.type == FindingType.THERMAL_WARNING }
        val cpuFinding = result.findings.find { it.type == FindingType.HIGH_CPU_ACTIVITY }

        assertTrue(thermalFinding != null)
        assertTrue(cpuFinding != null)
        assertEquals(Severity.CRITICAL, thermalFinding?.severity)
    }
}
