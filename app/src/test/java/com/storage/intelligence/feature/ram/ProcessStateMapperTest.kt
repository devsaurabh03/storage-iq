package com.storage.intelligence.feature.ram

import android.app.ActivityManager
import com.storage.intelligence.feature.ram.mapper.ProcessStateMapper
import com.storage.intelligence.feature.ram.model.ProcessState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProcessStateMapperTest {

    private lateinit var mapper: ProcessStateMapper

    @Before
    fun setUp() {
        mapper = ProcessStateMapper()
    }

    @Test
    fun `mapImportanceToState maps FOREGROUND correctly`() {
        val state = mapper.mapImportanceToState(ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
        assertEquals(ProcessState.FOREGROUND, state)
    }

    @Test
    fun `mapImportanceToState maps FOREGROUND_SERVICE correctly`() {
        val state = mapper.mapImportanceToState(ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE)
        assertEquals(ProcessState.FOREGROUND_SERVICE, state)
    }

    @Test
    fun `mapImportanceToState maps SERVICE correctly`() {
        val state = mapper.mapImportanceToState(ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE)
        assertEquals(ProcessState.SERVICE, state)
    }

    @Test
    fun `mapImportanceToState maps CACHED correctly`() {
        val state = mapper.mapImportanceToState(ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED)
        assertEquals(ProcessState.CACHED, state)
    }

    @Test
    fun `mapImportanceToState maps PERCEPTIBLE correctly`() {
        val state = mapper.mapImportanceToState(ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE)
        assertEquals(ProcessState.PERCEPTIBLE, state)
    }
}
