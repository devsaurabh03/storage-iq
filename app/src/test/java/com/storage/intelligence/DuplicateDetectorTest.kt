package com.storage.intelligence

import com.storage.intelligence.domain.model.DuplicateGroup
import com.storage.intelligence.domain.model.FileItem
import com.storage.intelligence.domain.model.StorageCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DuplicateDetectorTest {

    @Test
    fun `duplicate grouping preserves at least one original copy`() {
        val files = listOf(
            FileItem(1L, "/path/video1.mp4", "video1.mp4", 1000L, "video/mp4", 100L, StorageCategory.VIDEOS, sha256Hash = "hash123"),
            FileItem(2L, "/path/video2.mp4", "video2.mp4", 1000L, "video/mp4", 200L, StorageCategory.VIDEOS, sha256Hash = "hash123"),
            FileItem(3L, "/path/video3.mp4", "video3.mp4", 1000L, "video/mp4", 150L, StorageCategory.VIDEOS, sha256Hash = "hash123")
        )

        val groups = files.groupBy { it.sha256Hash!! }
            .filter { it.value.size > 1 }
            .map { (hash, items) ->
                val sorted = items.sortedByDescending { it.modifiedTimeMs }
                val preserved = sorted.first()
                val duplicates = sorted.drop(1)

                DuplicateGroup(
                    groupId = hash,
                    sha256Hash = hash,
                    totalSizeBytes = items.sumOf { it.sizeBytes },
                    recoverableSizeBytes = duplicates.sumOf { it.sizeBytes },
                    files = sorted.map { it.copy(isSelectedForDeletion = it.id != preserved.id) },
                    preservedFileId = preserved.id
                )
            }

        assertEquals(1, groups.size)
        val group = groups.first()
        assertEquals(3000L, group.totalSizeBytes)
        assertEquals(2000L, group.recoverableSizeBytes)
        assertEquals(2L, group.preservedFileId) // Newest file (modifiedTimeMs 200) preserved
        assertNotNull(group.files.find { it.id == 2L && !it.isSelectedForDeletion })
    }
}
