package com.storage.intelligence.feature.ram

import com.storage.intelligence.feature.ram.engine.MemoryRecommendationEngine
import com.storage.intelligence.feature.ram.model.AppProcessSignal
import com.storage.intelligence.feature.ram.model.MemoryPressure
import com.storage.intelligence.feature.ram.model.ProcessState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MemoryRecommendationEngineTest {

    private lateinit var engine: MemoryRecommendationEngine

    @Before
    fun setUp() {
        engine = MemoryRecommendationEngine()
    }

    @Test
    fun `generateRecommendation for LOW pressure returns healthy guidance`() {
        val recommendation = engine.generateRecommendation(MemoryPressure.LOW, emptyList())
        assertEquals(MemoryPressure.LOW, recommendation.pressure)
        assertEquals("Memory looks healthy", recommendation.title)
    }

    @Test
    fun `generateRecommendation for MODERATE pressure reassures user`() {
        val signals = listOf(
            AppProcessSignal("com.example.app", "Example", ProcessState.CACHED, 400, 1234)
        )
        val recommendation = engine.generateRecommendation(MemoryPressure.MODERATE, signals)
        assertEquals(MemoryPressure.MODERATE, recommendation.pressure)
        assertEquals("Moderate memory pressure", recommendation.title)
    }

    @Test
    fun `generateRecommendation for HIGH pressure guides review`() {
        val signals = listOf(
            AppProcessSignal("com.example.music", "Music Player", ProcessState.FOREGROUND_SERVICE, 125, 5678)
        )
        val recommendation = engine.generateRecommendation(MemoryPressure.HIGH, signals)
        assertEquals(MemoryPressure.HIGH, recommendation.pressure)
        assertEquals("High memory pressure", recommendation.title)
    }
}
