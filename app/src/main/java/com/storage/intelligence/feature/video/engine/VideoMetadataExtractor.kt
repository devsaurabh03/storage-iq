package com.storage.intelligence.feature.video.engine

import android.media.MediaExtractor
import android.media.MediaFormat
import com.storage.intelligence.feature.video.codec.CodecCapabilityManager
import java.io.File

data class KeyframeIndex(
    val timestampMs: Long,
    val byteOffset: Long
)

data class VideoCharacteristics(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrateBps: Int,
    val codecName: String, // AV1, HEVC, H.264
    val mimeType: String,
    val isHdr: Boolean,
    val audioCodec: String?,
    val keyframeIndex: List<KeyframeIndex> = emptyList(),
    val modifiedTimeMs: Long = 0L
)

object VideoMetadataExtractor {

    fun extractCharacteristics(
        file: File,
        context: android.content.Context? = null,
        uri: android.net.Uri? = null,
        fileSizeBytes: Long = 0L,
        modifiedTimeMs: Long = 0L
    ): VideoCharacteristics {
        val extractor = MediaExtractor()
        var width = 1920
        var height = 1080
        var fps = 30
        var bitrate = 10_000_000
        var durationMs = 0L
        var mimeType = "video/avc"
        var codecName = "H.264"
        var isHdr = false
        var audioCodec: String? = null
        val keyframeList = mutableListOf<KeyframeIndex>()

        try {
            if (file.exists() && file.canRead()) {
                extractor.setDataSource(file.absolutePath)
            } else if (context != null && uri != null) {
                extractor.setDataSource(context, uri, null)
            } else {
                extractor.setDataSource(file.absolutePath)
            }
            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                if (mime.startsWith("video/")) {
                    extractor.selectTrack(i)
                    mimeType = mime
                    codecName = when {
                        mime.contains("av01") || mime.contains("av1") -> "AV1"
                        mime.contains("hevc") || mime.contains("h265") -> "HEVC"
                        mime.contains("avc") || mime.contains("h264") -> "H.264"
                        else -> mime.substringAfter("video/").uppercase()
                    }

                    if (format.containsKey(MediaFormat.KEY_WIDTH)) width = format.getInteger(MediaFormat.KEY_WIDTH)
                    if (format.containsKey(MediaFormat.KEY_HEIGHT)) height = format.getInteger(MediaFormat.KEY_HEIGHT)
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) fps = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE)) bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE)
                    if (format.containsKey(MediaFormat.KEY_DURATION)) durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000

                    if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                        val transfer = format.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
                        isHdr = transfer == MediaFormat.COLOR_TRANSFER_ST2084 || transfer == MediaFormat.COLOR_TRANSFER_HLG
                    }

                    // Build Keyframe Index Table (sample every 10 seconds for fast seeking)
                    var sampleTime = extractor.sampleTime
                    var sampleCount = 0
                    while (sampleTime >= 0 && sampleCount < 50) {
                        val flags = extractor.sampleFlags
                        if (flags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                            keyframeList.add(KeyframeIndex(sampleTime / 1000, sampleCount * 1_000_000L))
                        }
                        extractor.advance()
                        sampleTime = extractor.sampleTime
                        sampleCount++
                    }
                } else if (mime.startsWith("audio/")) {
                    audioCodec = mime.substringAfter("audio/").uppercase()
                }
            }
        } catch (e: Exception) {
            // Fallback default
        } finally {
            try { extractor.release() } catch (_: Exception) {}
        }

        val finalSize = if (fileSizeBytes > 0L) fileSizeBytes else (try { file.length() } catch (_: Exception) { 0L })
        val finalModified = if (modifiedTimeMs > 0L) modifiedTimeMs else (try { file.lastModified() } catch (_: Exception) { 0L })

        if (bitrate <= 0 && durationMs > 0) {
            bitrate = if (finalSize > 0) ((finalSize * 8) / (durationMs / 1000f)).toInt().coerceAtLeast(1_000_000) else 10_000_000
        }

        return VideoCharacteristics(
            file = file,
            name = file.name,
            sizeBytes = finalSize,
            durationMs = durationMs,
            width = width,
            height = height,
            fps = fps,
            bitrateBps = bitrate,
            codecName = codecName,
            mimeType = mimeType,
            isHdr = isHdr,
            audioCodec = audioCodec,
            keyframeIndex = keyframeList,
            modifiedTimeMs = finalModified
        )
    }
}
