package com.storage.intelligence.feature.photo.model

import com.storage.intelligence.domain.model.FileItem

enum class TargetPhotoFormat(val displayName: String, val extension: String, val mimeType: String) {
    WEBP("WebP (Modern & Compact)", "webp", "image/webp"),
    JPEG("Optimized JPEG", "jpg", "image/jpeg"),
    AVIF("AVIF (Highest Efficiency)", "avif", "image/avif")
}

data class PhotoOptimizationConfig(
    val format: TargetPhotoFormat = TargetPhotoFormat.WEBP,
    val quality: Int = 82, // 80-85 is visually lossless
    val maxDimension: Int? = null, // null preserves original resolution
    val preserveExif: Boolean = true
)

data class PhotoOptimizationCandidate(
    val fileItem: FileItem,
    val width: Int,
    val height: Int,
    val originalSizeBytes: Long,
    val estimatedOptimizedSizeBytes: Long,
    val estimatedSavingsBytes: Long,
    val estimatedSavingsPercent: Int,
    val recommendedFormat: TargetPhotoFormat,
    val isWorthOptimizing: Boolean,
    val reason: String,
    val isSelected: Boolean = true
)

data class PhotoOptimizationResult(
    val fileId: Long,
    val originalPath: String,
    val optimizedPath: String,
    val originalSizeBytes: Long,
    val optimizedSizeBytes: Long,
    val savedBytes: Long,
    val success: Boolean,
    val errorMessage: String? = null
)
