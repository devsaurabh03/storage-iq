package com.storage.intelligence

import com.storage.intelligence.core.common.SearchFilter
import com.storage.intelligence.core.common.SizeThreshold
import com.storage.intelligence.domain.model.FileItem
import com.storage.intelligence.domain.model.StorageCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchFilterTest {

    @Test
    fun `search filter matches by size threshold and query`() {
        val files = listOf(
            FileItem(1L, "/p/vid1.mp4", "vacation_video.mp4", 600L * 1024 * 1024, "video/mp4", 1000L, StorageCategory.VIDEOS),
            FileItem(2L, "/p/photo.jpg", "family_photo.jpg", 2L * 1024 * 1024, "image/jpeg", 1000L, StorageCategory.PHOTOS),
            FileItem(3L, "/p/doc.pdf", "manual.pdf", 50L * 1024 * 1024, "application/pdf", 1000L, StorageCategory.OTHER)
        )

        val filter = SearchFilter(
            query = "vacation",
            sizeThreshold = SizeThreshold.OVER_500MB
        )

        val matched = files.filter { item ->
            val matchesQuery = filter.query.isEmpty() || item.name.contains(filter.query, ignoreCase = true)
            val matchesSize = item.sizeBytes >= filter.sizeThreshold.minBytes
            matchesQuery && matchesSize
        }

        assertEquals(1, matched.size)
        assertEquals("vacation_video.mp4", matched.first().name)
    }
}
