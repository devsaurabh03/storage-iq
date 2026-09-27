package com.storage.intelligence.feature.video.engine

import com.storage.intelligence.feature.video.codec.CodecCapabilityManager
import com.storage.intelligence.feature.video.codec.DeviceCodecProfile

enum class OptimizationMode(val displayName: String, val description: String) {
    QUICK("Quick Mode", "Fastest encoding with lowest battery impact"),
    BALANCED("Balanced Mode", "Optimal balance of video quality, compression ratio, and speed"),
    MAXIMUM_SAVINGS("Maximum Savings", "Maximum storage reduction using AV1/HEVC hardware acceleration")
}

data class OptimizationPlan(
    val source: VideoCharacteristics,
    val mode: OptimizationMode,
    val targetCodecName: String, // AV1, HEVC, H.264
    val targetMimeType: String,
    val targetWidth: Int,
    val targetHeight: Int,
    val targetFps: Int,
    val targetBitrateBps: Int,
    val estimatedOutputSizeBytes: Long,
    val estimatedSavingsBytes: Long,
    val isHardwareAccelerated: Boolean,
    val estimatedProcessingTimeSec: Int
)

object VideoOptimizationEngine {

    fun generatePlan(
        source: VideoCharacteristics,
        deviceProfile: DeviceCodecProfile,
        mode: OptimizationMode = OptimizationMode.BALANCED
    ): OptimizationPlan {
        // Codec selection decision logic
        val (targetCodecName, targetMimeType, isHw) = when (mode) {
            OptimizationMode.MAXIMUM_SAVINGS -> {
                if (deviceProfile.av1Encoder.isHardwareEncoderAvailable && deviceProfile.av1Decoder.isHardwareDecoderAvailable) {
                    Triple("AV1", CodecCapabilityManager.MIME_AV1, true)
                } else if (deviceProfile.hevcEncoder.isHardwareEncoderAvailable && deviceProfile.hevcDecoder.isHardwareDecoderAvailable) {
                    Triple("HEVC", CodecCapabilityManager.MIME_HEVC, true)
                } else {
                    Triple("H.264", CodecCapabilityManager.MIME_H264, deviceProfile.h264Encoder.isHardwareEncoderAvailable)
                }
            }
            OptimizationMode.BALANCED -> {
                if (deviceProfile.hevcEncoder.isHardwareEncoderAvailable && deviceProfile.hevcDecoder.isHardwareDecoderAvailable) {
                    Triple("HEVC", CodecCapabilityManager.MIME_HEVC, true)
                } else if (deviceProfile.av1Encoder.isHardwareEncoderAvailable && deviceProfile.av1Decoder.isHardwareDecoderAvailable) {
                    Triple("AV1", CodecCapabilityManager.MIME_AV1, true)
                } else {
                    Triple("H.264", CodecCapabilityManager.MIME_H264, true)
                }
            }
            OptimizationMode.QUICK -> {
                Triple("H.264", CodecCapabilityManager.MIME_H264, deviceProfile.h264Encoder.isHardwareEncoderAvailable)
            }
        }

        // Resolution & FPS scaling logic
        val (targetWidth, targetHeight) = when {
            mode == OptimizationMode.MAXIMUM_SAVINGS && source.width >= 3840 -> Pair(1920, 1080)
            mode == OptimizationMode.BALANCED && source.width >= 3840 -> Pair(2560, 1440)
            else -> Pair(source.width, source.height)
        }

        val targetFps = when {
            mode == OptimizationMode.MAXIMUM_SAVINGS && source.fps >= 60 -> 30
            else -> source.fps.coerceIn(24, 60)
        }

        // Codec efficiency multiplier
        val codecEfficiency = when (targetCodecName) {
            "AV1" -> 0.40f // ~60% size reduction
            "HEVC" -> 0.50f // ~50% size reduction
            else -> 0.70f // ~30% size reduction
        }

        val resMultiplier = (targetWidth.toFloat() * targetHeight.toFloat()) / (source.width.toFloat() * source.height.toFloat()).coerceAtLeast(1f)
        val fpsMultiplier = targetFps.toFloat() / source.fps.toFloat().coerceAtLeast(1f)

        val targetBitrateBps = (source.bitrateBps * codecEfficiency * resMultiplier * fpsMultiplier)
            .toInt()
            .coerceIn(1_500_000, 15_000_000)

        val estimatedOutputSizeBytes = if (source.durationMs > 0) {
            ((targetBitrateBps.toDouble() / 8.0) * (source.durationMs / 1000.0)).toLong()
        } else {
            (source.sizeBytes * codecEfficiency * resMultiplier).toLong()
        }.coerceAtMost((source.sizeBytes * 0.85).toLong())

        val estimatedSavingsBytes = (source.sizeBytes - estimatedOutputSizeBytes).coerceAtLeast(0L)
        val processingTimeSec = ((source.durationMs / 1000) * if (isHw) 0.15f else 0.5f).toInt().coerceAtLeast(5)

        return OptimizationPlan(
            source = source,
            mode = mode,
            targetCodecName = targetCodecName,
            targetMimeType = targetMimeType,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            targetFps = targetFps,
            targetBitrateBps = targetBitrateBps,
            estimatedOutputSizeBytes = estimatedOutputSizeBytes,
            estimatedSavingsBytes = estimatedSavingsBytes,
            isHardwareAccelerated = isHw,
            estimatedProcessingTimeSec = processingTimeSec
        )
    }
}
