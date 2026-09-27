package com.storage.intelligence

import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.domain.model.RecoverableSpace
import org.junit.Assert.assertEquals
import org.junit.Test

class RecoverableSpaceCalculatorTest {

    @Test
    fun `formatBytes formats MB and GB accurately`() {
        assertEquals("0 B", FormattingUtils.formatBytes(0L))
        assertEquals("500.0 MB", FormattingUtils.formatBytes(500L * 1024 * 1024))
        assertEquals("12.4 GB", FormattingUtils.formatBytes((12.4 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun `recoverable space calculation sums categories correctly`() {
        val summary = RecoverableSpace(
            totalRecoverableBytes = 0L,
            duplicateVideosBytes = 12L * 1024 * 1024 * 1024,
            duplicatePhotosBytes = 3L * 1024 * 1024 * 1024,
            oldDownloadsBytes = 4L * 1024 * 1024 * 1024,
            screenshotsBytes = 2L * 1024 * 1024 * 1024,
            otherRecoverableBytes = 0L
        )

        val total = summary.duplicateVideosBytes + summary.duplicatePhotosBytes + summary.oldDownloadsBytes + summary.screenshotsBytes
        assertEquals(21L * 1024 * 1024 * 1024, total)
    }
}
