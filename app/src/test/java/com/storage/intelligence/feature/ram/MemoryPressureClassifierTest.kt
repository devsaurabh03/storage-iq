package com.storage.intelligence.feature.ram

import com.storage.intelligence.feature.ram.classifier.MemoryPressureClassifier
import com.storage.intelligence.feature.ram.model.MemoryPressure
import com.storage.intelligence.feature.ram.model.SystemMemorySnapshot
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MemoryPressureClassifierTest {

    private lateinit var classifier: MemoryPressureClassifier

    @Before
    fun setUp() {
        classifier = MemoryPressureClassifier()
    }

    @Test
    fun `classify returns LOW when RAM usage is below 70 percent and above thresholds`() {
        // 8 GB Total, 4 GB Available (50% used)
        val snapshot = SystemMemorySnapshot(
            totalBytes = 8_000_000_000L,
            availableBytes = 4_000_000_000L,
            thresholdBytes = 500_000_000L,
            lowMemory = false
        )

        val pressure = classifier.classify(snapshot)
        assertEquals(MemoryPressure.LOW, pressure)
    }

    @Test
    fun `classify returns MODERATE when RAM usage is between 70 and 83 percent`() {
        // 8 GB Total, 2 GB Available (75% used)
        val snapshot = SystemMemorySnapshot(
            totalBytes = 8_000_000_000L,
            availableBytes = 2_000_000_000L,
            thresholdBytes = 500_000_000L,
            lowMemory = false
        )

        val pressure = classifier.classify(snapshot)
        assertEquals(MemoryPressure.MODERATE, pressure)
    }

    @Test
    fun classify_returns_HIGH_when_available_bytes_approach_threshold_or_used_percent_is_high() {
        // 8 GB Total, 1.2 GB Available (85% used)
        val snapshot = SystemMemorySnapshot(
            totalBytes = 8_000_000_000L,
            availableBytes = 1_200_000_000L,
            thresholdBytes = 500_000_000L,
            lowMemory = false
        )

        val pressure = classifier.classify(snapshot)
        assertEquals(MemoryPressure.HIGH, pressure)
    }

    @Test
    fun `classify returns CRITICAL when lowMemory flag is true`() {
        val snapshot = SystemMemorySnapshot(
            totalBytes = 4_000_000_000L,
            availableBytes = 1_000_000_000L,
            thresholdBytes = 500_000_000L,
            lowMemory = true
        )

        val pressure = classifier.classify(snapshot)
        assertEquals(MemoryPressure.CRITICAL, pressure)
    }

    @Test
    fun `classify returns CRITICAL when available bytes is below threshold`() {
        val snapshot = SystemMemorySnapshot(
            totalBytes = 4_000_000_000L,
            availableBytes = 300_000_000L,
            thresholdBytes = 500_000_000L,
            lowMemory = false
        )

        val pressure = classifier.classify(snapshot)
        assertEquals(MemoryPressure.CRITICAL, pressure)
    }
}
