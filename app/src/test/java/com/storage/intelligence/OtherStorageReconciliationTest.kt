package com.storage.intelligence

import com.storage.intelligence.domain.model.ConfidenceLevel
import com.storage.intelligence.domain.model.ExplainableStorageItem
import com.storage.intelligence.domain.model.OtherStorageBreakdown
import com.storage.intelligence.domain.model.StorageExplanation
import com.storage.intelligence.domain.model.StorageReconciliation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OtherStorageReconciliationTest {

    @Test
    fun `reconciliation correctly computes unaccounted difference`() {
        val systemUsedBytes = 117L * 1024 * 1024 * 1024 // 117 GB
        val accountedBytes = 108L * 1024 * 1024 * 1024 // 108 GB
        val unaccountedBytes = (systemUsedBytes - accountedBytes).coerceAtLeast(0L)

        val reconciliation = StorageReconciliation(
            systemReportedUsedBytes = systemUsedBytes,
            accountedBytes = accountedBytes,
            unaccountedBytes = unaccountedBytes,
            confidence = ConfidenceLevel.EXACT,
            explanations = emptyList()
        )

        assertEquals(9L * 1024 * 1024 * 1024, reconciliation.unaccountedBytes)
        assertEquals(ConfidenceLevel.EXACT, reconciliation.confidence)
    }

    @Test
    fun `reconciliation handles edge case where accounted exceeds system reported`() {
        val systemUsedBytes = 50L * 1024 * 1024
        val accountedBytes = 60L * 1024 * 1024
        val unaccountedBytes = (systemUsedBytes - accountedBytes).coerceAtLeast(0L)

        assertEquals(0L, unaccountedBytes)
    }

    @Test
    fun `other storage breakdown segregates explainable from platform restricted`() {
        val explainable = listOf(
            ExplainableStorageItem("Temporary files", 3L * 1024 * 1024 * 1024, ConfidenceLevel.EXACT, "Public temp"),
            ExplainableStorageItem("Messaging media", 2L * 1024 * 1024 * 1024, ConfidenceLevel.EXACT, "WhatsApp/TG")
        )
        val explainableSum = explainable.sumOf { it.sizeBytes }
        val totalUnaccounted = 10L * 1024 * 1024 * 1024
        val restrictedBytes = (totalUnaccounted - explainableSum).coerceAtLeast(0L)

        val breakdown = OtherStorageBreakdown(
            totalUnaccountedBytes = totalUnaccounted,
            explainableBytes = explainableSum,
            unavailableBytes = restrictedBytes,
            explainableItems = explainable,
            inaccessibleSources = listOf(
                StorageExplanation("Private Sandboxes", "App-private data", null, ConfidenceLevel.HIGH_CONFIDENCE, false)
            )
        )

        assertEquals(5L * 1024 * 1024 * 1024, breakdown.explainableBytes)
        assertEquals(5L * 1024 * 1024 * 1024, breakdown.unavailableBytes)
        assertEquals(1, breakdown.inaccessibleSources.size)
        assertTrue(breakdown.inaccessibleSources.first().confidence == ConfidenceLevel.HIGH_CONFIDENCE)
    }
}
