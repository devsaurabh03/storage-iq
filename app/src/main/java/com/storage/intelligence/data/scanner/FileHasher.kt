package com.storage.intelligence.data.scanner

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileHasher @Inject constructor() {

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

    fun computeSha256(file: File): String? {
        if (!file.exists() || !file.canRead()) return null
        return try {
            computeSha256(file.inputStream())
        } catch (e: Exception) {
            null
        }
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

    fun calculateHammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }
}
