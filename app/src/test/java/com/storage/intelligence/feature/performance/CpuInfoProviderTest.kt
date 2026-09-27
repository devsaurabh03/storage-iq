package com.storage.intelligence.feature.performance

import com.storage.intelligence.feature.performance.model.CpuArchitecture
import com.storage.intelligence.feature.performance.model.CpuVendor
import com.storage.intelligence.feature.performance.provider.CpuInfoProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CpuInfoProviderTest {

    private val provider = CpuInfoProvider()

    @Test
    fun testGetCpuIdentity_ReturnsValidStructure() = runBlocking {
        val identity = provider.getCpuIdentity()
        assertNotNull(identity)
        assertNotNull(identity.vendor)
        assertNotNull(identity.architecture)
        assertNotNull(identity.socName)
        assert(identity.logicalCoreCount > 0)
    }

    @Test
    fun testVendorDetection_QualcommSnapdragon() {
        val method = CpuInfoProvider::class.java.getDeclaredMethod(
            "detectVendor",
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java
        )
        method.isAccessible = true
        val vendor = method.invoke(
            provider,
            "Qualcomm",
            "SM8450",
            "qcom",
            "tarTarget",
            "Hardware : Qualcomm Technologies, Inc SM8450"
        ) as CpuVendor

        assertEquals(CpuVendor.QUALCOMM, vendor)
    }

    @Test
    fun testVendorDetection_MediaTekDimensity() {
        val method = CpuInfoProvider::class.java.getDeclaredMethod(
            "detectVendor",
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java
        )
        method.isAccessible = true
        val vendor = method.invoke(
            provider,
            "MediaTek",
            "MT6895",
            "mt6895",
            "dimensity",
            "Hardware : MT6895"
        ) as CpuVendor

        assertEquals(CpuVendor.MEDIATEK, vendor)
    }

    @Test
    fun testVendorDetection_GoogleTensor() {
        val method = CpuInfoProvider::class.java.getDeclaredMethod(
            "detectVendor",
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java
        )
        method.isAccessible = true
        val vendor = method.invoke(
            provider,
            "Google",
            "GS201",
            "zuma",
            "tensor",
            "Hardware : Tensor G2"
        ) as CpuVendor

        assertEquals(CpuVendor.GOOGLE_TENSOR, vendor)
    }
}
