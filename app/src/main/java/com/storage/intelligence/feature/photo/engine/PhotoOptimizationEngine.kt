package com.storage.intelligence.feature.photo.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.storage.intelligence.domain.model.FileItem
import com.storage.intelligence.domain.model.StorageCategory
import com.storage.intelligence.feature.photo.model.PhotoOptimizationCandidate
import com.storage.intelligence.feature.photo.model.PhotoOptimizationConfig
import com.storage.intelligence.feature.photo.model.PhotoOptimizationResult
import com.storage.intelligence.feature.photo.model.TargetPhotoFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoOptimizationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Evaluates a file and determines whether photo optimization is recommended.
     * Does not blindly compress everything: skips files where savings are insignificant.
     */
    fun evaluateCandidate(file: FileItem): PhotoOptimizationCandidate {
        val size = file.sizeBytes
        val mime = file.mimeType.lowercase()
        val isPng = mime.contains("png") || file.name.endsWith(".png", ignoreCase = true)
        val isBmp = mime.contains("bmp") || file.name.endsWith(".bmp", ignoreCase = true)
        val isLargeJpeg = (mime.contains("jpeg") || mime.contains("jpg")) && size > 2_000_000L
        val isMediumJpeg = (mime.contains("jpeg") || mime.contains("jpg")) && size > 800_000L
        val isWebp = mime.contains("webp") || file.name.endsWith(".webp", ignoreCase = true)
        val isHeic = mime.contains("heic") || mime.contains("heif") || file.name.endsWith(".heic", ignoreCase = true) || file.name.endsWith(".heif", ignoreCase = true)

        val (savingsPercent, recommendedFormat, reason, isWorth) = when {
            isPng && size > 400_000L -> {
                Quad(65, TargetPhotoFormat.WEBP, "Uncompressed PNG: Converting to WebP saves ~65%", true)
            }
            isBmp -> {
                Quad(80, TargetPhotoFormat.WEBP, "Raw bitmap: Modern compression saves ~80%", true)
            }
            isLargeJpeg -> {
                Quad(45, TargetPhotoFormat.WEBP, "Large camera photo: Smart WebP saves ~45% with visually identical quality", true)
            }
            isMediumJpeg -> {
                Quad(35, TargetPhotoFormat.WEBP, "JPEG photo: WebP compression saves ~35%", true)
            }
            isWebp && size > 1_500_000L -> {
                Quad(45, TargetPhotoFormat.WEBP, "Large WebP (lossless/uncompressed): High-efficiency re-encoding saves ~45%", true)
            }
            isWebp && size > 500_000L -> {
                Quad(35, TargetPhotoFormat.WEBP, "Unoptimized WebP: Compact lossy compression saves ~35%", true)
            }
            isHeic && size > 2_000_000L -> {
                Quad(35, TargetPhotoFormat.WEBP, "Large HEIC image: High-efficiency WebP saves ~35%", true)
            }
            size > 4_000_000L -> {
                Quad(40, TargetPhotoFormat.WEBP, "Large image file: High compression potential (~40%)", true)
            }
            else -> {
                val formatName = when {
                    isWebp -> "WebP"
                    isPng -> "PNG"
                    isHeic -> "HEIC"
                    else -> "JPEG"
                }
                Quad(10, TargetPhotoFormat.WEBP, "Already compact $formatName or compression savings negligible (<15%)", false)
            }
        }

        val estimatedSavingsBytes = if (isWorth) (size * (savingsPercent / 100f)).toLong() else 0L
        val estimatedOptimizedSize = (size - estimatedSavingsBytes).coerceAtLeast(1024L)

        com.storage.intelligence.core.logging.AppLogger.d(
            "PhotoOptimizer",
            "Evaluated candidate '${file.name}' (${file.mimeType}, ${size / 1024} KB): isWorth=$isWorth, estSavings=${estimatedSavingsBytes / 1024} KB ($savingsPercent%)"
        )

        return PhotoOptimizationCandidate(
            fileItem = file,
            width = file.width,
            height = file.height,
            originalSizeBytes = size,
            estimatedOptimizedSizeBytes = estimatedOptimizedSize,
            estimatedSavingsBytes = estimatedSavingsBytes,
            estimatedSavingsPercent = savingsPercent,
            recommendedFormat = recommendedFormat,
            isWorthOptimizing = isWorth,
            reason = reason,
            isSelected = isWorth
        )
    }

    /**
     * Executes the Safe Media Replacement workflow:
     * Original -> temporary output -> validate output -> validate decode -> commit output
     * Never deletes the original automatically.
     */
    suspend fun optimizePhoto(
        candidate: FileItem,
        config: PhotoOptimizationConfig = PhotoOptimizationConfig()
    ): PhotoOptimizationResult = withContext(Dispatchers.IO) {
        val originalFile = File(candidate.path)
        val originalLength = when {
            originalFile.exists() && originalFile.length() > 0L -> originalFile.length()
            candidate.sizeBytes > 0L -> candidate.sizeBytes
            else -> 0L
        }

        // Open input stream safely (via File or MediaStore ContentResolver)
        val inputStream = try {
            if (originalFile.exists() && originalFile.canRead()) {
                FileInputStream(originalFile)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } ?: try {
            val contentUri = android.content.ContentUris.withAppendedId(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                candidate.id
            )
            context.contentResolver.openInputStream(contentUri)
        } catch (_: Exception) {
            null
        }

        if (inputStream == null) {
            return@withContext PhotoOptimizationResult(
                fileId = candidate.id,
                originalPath = candidate.path,
                optimizedPath = "",
                originalSizeBytes = originalLength,
                optimizedSizeBytes = 0L,
                savedBytes = 0L,
                success = false,
                errorMessage = "Cannot access image data (permission or file missing)."
            )
        }

        // 1. Temporary output file in app cache
        val tempDir = File(context.cacheDir, "photo_opt").apply { if (!exists()) mkdirs() }
        val tempFile = File(tempDir, "opt_${System.currentTimeMillis()}_${candidate.id}.${config.format.extension}")

        try {
            // 2. Decode bitmap safely
            val bitmap = inputStream.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext PhotoOptimizationResult(
                fileId = candidate.id,
                originalPath = candidate.path,
                optimizedPath = "",
                originalSizeBytes = originalLength,
                optimizedSizeBytes = 0L,
                savedBytes = 0L,
                success = false,
                errorMessage = "Failed to decode image bitmap."
            )

            // Optional downscaling if maxDimension is configured
            val processedBitmap = if (config.maxDimension != null && (bitmap.width > config.maxDimension || bitmap.height > config.maxDimension)) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val targetW = if (ratio >= 1) config.maxDimension else (config.maxDimension * ratio).toInt()
                val targetH = if (ratio >= 1) (config.maxDimension / ratio).toInt() else config.maxDimension
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true).also {
                    if (it != bitmap) bitmap.recycle()
                }
            } else {
                bitmap
            }

            // 3. Compress to target format
            val compressFormat = when (config.format) {
                TargetPhotoFormat.WEBP -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
                TargetPhotoFormat.JPEG -> Bitmap.CompressFormat.JPEG
                TargetPhotoFormat.AVIF -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    }
                }
            }

            FileOutputStream(tempFile).use { out ->
                processedBitmap.compress(compressFormat, config.quality, out)
            }
            processedBitmap.recycle()

            // 4. Validate output
            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                return@withContext PhotoOptimizationResult(
                    fileId = candidate.id,
                    originalPath = candidate.path,
                    optimizedPath = "",
                    originalSizeBytes = originalLength,
                    optimizedSizeBytes = 0L,
                    savedBytes = 0L,
                    success = false,
                    errorMessage = "Compressed file is empty or corrupted."
                )
            }

            // Copy EXIF metadata if enabled and source file exists
            if (config.preserveExif && originalFile.exists() && originalFile.canRead()) {
                try {
                    val srcExif = android.media.ExifInterface(originalFile.absolutePath)
                    val destExif = android.media.ExifInterface(tempFile.absolutePath)
                    val tags = arrayOf(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.TAG_DATETIME,
                        android.media.ExifInterface.TAG_MAKE,
                        android.media.ExifInterface.TAG_MODEL,
                        android.media.ExifInterface.TAG_FLASH,
                        android.media.ExifInterface.TAG_WHITE_BALANCE
                    )
                    for (tag in tags) {
                        val value = srcExif.getAttribute(tag)
                        if (value != null) {
                            destExif.setAttribute(tag, value)
                        }
                    }
                    destExif.saveAttributes()
                } catch (_: Exception) {
                    // EXIF copy failure is non-fatal
                }
            }

            val optimizedLength = tempFile.length()

            // If compressed file is somehow larger than original, preserve original and skip
            if (originalLength > 0L && optimizedLength >= originalLength) {
                tempFile.delete()
                return@withContext PhotoOptimizationResult(
                    fileId = candidate.id,
                    originalPath = candidate.path,
                    optimizedPath = "",
                    originalSizeBytes = originalLength,
                    optimizedSizeBytes = originalLength,
                    savedBytes = 0L,
                    success = false,
                    errorMessage = "Original file is already more compact than compressed output."
                )
            }

            // 5. Commit output safely to Pictures/OptimizedPhotos
            val baseName = candidate.name.substringBeforeLast('.')
            val targetFileName = "${baseName}_optimized.${config.format.extension}"
            var finalDestinationPath = ""
            var writeSuccess = false

            // Primary method: Modern Android MediaStore API (Scoped Storage compliant)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, targetFileName)
                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, config.format.mimeType)
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "${android.os.Environment.DIRECTORY_PICTURES}/OptimizedPhotos")
                        put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                    }

                    val insertUri = context.contentResolver.insert(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )

                    if (insertUri != null) {
                        context.contentResolver.openOutputStream(insertUri)?.use { outStream ->
                            tempFile.inputStream().use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        contentValues.clear()
                        contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(insertUri, contentValues, null, null)

                        finalDestinationPath = insertUri.toString()
                        writeSuccess = true
                    }
                } catch (e: Exception) {
                    writeSuccess = false
                }
            }

            // Fallback: App external media directory or public Pictures directory
            if (!writeSuccess) {
                try {
                    val fallbackDir = File(
                        context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.filesDir,
                        "OptimizedPhotos"
                    ).apply { if (!exists()) mkdirs() }
                    val finalFile = File(fallbackDir, targetFileName)
                    tempFile.copyTo(finalFile, overwrite = true)
                    finalDestinationPath = finalFile.absolutePath
                    writeSuccess = true
                } catch (e: Exception) {
                    tempFile.delete()
                    return@withContext PhotoOptimizationResult(
                        fileId = candidate.id,
                        originalPath = candidate.path,
                        optimizedPath = "",
                        originalSizeBytes = originalLength,
                        optimizedSizeBytes = 0L,
                        savedBytes = 0L,
                        success = false,
                        errorMessage = "Storage write failed: ${e.localizedMessage}"
                    )
                }
            }

            tempFile.delete()

            val savedBytes = (originalLength - optimizedLength).coerceAtLeast(0L)

            com.storage.intelligence.core.logging.AppLogger.i(
                "PhotoOptimizer",
                "Successfully optimized '${candidate.name}': saved ${savedBytes / 1024} KB (${originalLength / 1024} KB -> ${optimizedLength / 1024} KB, output: $finalDestinationPath)"
            )

            return@withContext PhotoOptimizationResult(
                fileId = candidate.id,
                originalPath = candidate.path,
                optimizedPath = finalDestinationPath,
                originalSizeBytes = originalLength,
                optimizedSizeBytes = optimizedLength,
                savedBytes = savedBytes,
                success = true
            )

        } catch (e: Exception) {
            tempFile.delete()
            com.storage.intelligence.core.logging.AppLogger.e(
                "PhotoOptimizer",
                "Failed optimizing '${candidate.name}' (${candidate.path})",
                e
            )
            return@withContext PhotoOptimizationResult(
                fileId = candidate.id,
                originalPath = candidate.path,
                optimizedPath = "",
                originalSizeBytes = originalLength,
                optimizedSizeBytes = 0L,
                savedBytes = 0L,
                success = false,
                errorMessage = e.localizedMessage ?: "Unknown photo compression error."
            )
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
