package com.storage.intelligence

import android.content.Context
import com.storage.intelligence.domain.model.FileItem
import com.storage.intelligence.domain.model.StorageCategory
import com.storage.intelligence.feature.photo.engine.PhotoOptimizationEngine
import com.storage.intelligence.feature.photo.model.TargetPhotoFormat
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PhotoOptimizationEngineTest {

    private val mockContext = mockk<Context>(relaxed = true)
    private lateinit var engine: PhotoOptimizationEngine

    @Before
    fun setup() {
        engine = PhotoOptimizationEngine(mockContext)
    }

    @Test
    fun `evaluateCandidate marks large PNG as worth optimizing with WebP target`() {
        val file = FileItem(
            id = 101L,
            path = "/storage/emulated/0/DCIM/screenshot_large.png",
            name = "screenshot_large.png",
            sizeBytes = 5_000_000L, // 5 MB PNG
            mimeType = "image/png",
            modifiedTimeMs = System.currentTimeMillis(),
            category = StorageCategory.PHOTOS,
            width = 1440,
            height = 3120
        )

        val candidate = engine.evaluateCandidate(file)

        assertTrue(candidate.isWorthOptimizing)
        assertEquals(TargetPhotoFormat.WEBP, candidate.recommendedFormat)
        assertTrue(candidate.estimatedSavingsBytes > 2_000_000L)
        assertEquals(65, candidate.estimatedSavingsPercent)
    }

    @Test
    fun `evaluateCandidate skips already compact photos to prevent blind conversion`() {
        val smallFile = FileItem(
            id = 102L,
            path = "/storage/emulated/0/DCIM/thumbnail.jpg",
            name = "thumbnail.jpg",
            sizeBytes = 150_000L, // 150 KB JPEG
            mimeType = "image/jpeg",
            modifiedTimeMs = System.currentTimeMillis(),
            category = StorageCategory.PHOTOS,
            width = 640,
            height = 480
        )

        val candidate = engine.evaluateCandidate(smallFile)

        assertFalse(candidate.isWorthOptimizing)
        assertEquals(0L, candidate.estimatedSavingsBytes)
    }
}
