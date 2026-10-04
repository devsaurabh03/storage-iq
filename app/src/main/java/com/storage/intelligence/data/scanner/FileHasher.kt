package com.storage.intelligence.data.scanner

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileHasher @Inject constructor(
    @ApplicationContext private val context: Context? = null
) {

    fun computeSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        inputStream.use { stream ->
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun computeSha256(file: File, mediaStoreId: Long? = null): String? {
        if (file.exists() && file.canRead()) {
            try {
                return computeSha256(file.inputStream())
            } catch (_: Exception) { }
        }
        // Scoped storage fallback: read via MediaStore ContentResolver if direct File I/O is restricted
        if (context != null && mediaStoreId != null && mediaStoreId > 0) {
            try {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Files.getContentUri("external"),
                    mediaStoreId
                )
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    return computeSha256(stream)
                }
            } catch (_: Exception) { }
        }
        return null
    }

    /**
     * Computes a 64-bit Difference Hash (dHash) for visual image similarity.
     * Resizes image to 9x8, converts to grayscale, and compares horizontal pixels.
     */
    fun computeDHash(bitmap: Bitmap): Long {
        val resized = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        var hash = 0L
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val leftPixel = resized.getPixel(x, y)
                val rightPixel = resized.getPixel(x + 1, y)

                val leftGray = (Color.red(leftPixel) * 0.299 + Color.green(leftPixel) * 0.587 + Color.blue(leftPixel) * 0.114).toInt()
                val rightGray = (Color.red(rightPixel) * 0.299 + Color.green(rightPixel) * 0.587 + Color.blue(rightPixel) * 0.114).toInt()

                if (leftGray < rightGray) {
                    hash = hash or (1L shl (y * 8 + x))
                }
            }
        }
        if (resized != bitmap) {
            resized.recycle()
        }
        return hash
    }

    fun computeDHash(file: File, mediaStoreId: Long? = null): Long? {
        val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
        if (file.exists() && file.canRead()) {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
                if (bitmap != null) {
                    val hash = computeDHash(bitmap)
                    bitmap.recycle()
                    return hash
                }
            } catch (_: Exception) { }
        }
        if (context != null && mediaStoreId != null && mediaStoreId > 0) {
            try {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Files.getContentUri("external"),
                    mediaStoreId
                )
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = android.graphics.BitmapFactory.decodeStream(stream, null, options)
                    if (bitmap != null) {
                        val hash = computeDHash(bitmap)
                        bitmap.recycle()
                        return hash
                    }
                }
            } catch (_: Exception) { }
        }
        return null
    }

    fun calculateHammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }
}
