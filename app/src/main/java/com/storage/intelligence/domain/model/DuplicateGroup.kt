package com.storage.intelligence.domain.model

data class DuplicateGroup(
    val groupId: String,
    val sha256Hash: String,
    val totalSizeBytes: Long,
    val recoverableSizeBytes: Long, // Size of duplicates excluding preserved copy
    val files: List<FileItem>,
    val preservedFileId: Long?
)

data class SimilarPhotoGroup(
    val groupId: String,
    val recommendedKeepFileId: Long,
    val reasonForKeepRecommendation: String, // e.g., "Highest resolution (4000x3000)", "Sharpest detail"
    val photos: List<FileItem>
)
