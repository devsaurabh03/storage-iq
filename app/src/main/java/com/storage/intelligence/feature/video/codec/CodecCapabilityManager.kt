package com.storage.intelligence.feature.video.codec

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build

data class CodecCapabilities(
    val mimeType: String,
    val isHardwareDecoderAvailable: Boolean,
    val isHardwareEncoderAvailable: Boolean,
    val maxSupportedWidth: Int,
    val maxSupportedHeight: Int,
    val maxSupportedFps: Int,
    val supports4k60: Boolean,
    val supportsHdr: Boolean
)

data class DeviceCodecProfile(
    val deviceName: String,
    val androidVersion: String,
    val av1Decoder: CodecCapabilities,
    val av1Encoder: CodecCapabilities,
    val hevcDecoder: CodecCapabilities,
    val hevcEncoder: CodecCapabilities,
    val h264Decoder: CodecCapabilities,
    val h264Encoder: CodecCapabilities,
    val preferredCodecForSavings: String
)

object CodecCapabilityManager {

    const val MIME_AV1 = "video/av01"
    const val MIME_HEVC = "video/hevc"
    const val MIME_H264 = "video/avc"

    fun detectCapabilities(): DeviceCodecProfile {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val infos = codecList.codecInfos

        val av1Dec = inspectCodec(infos, MIME_AV1, isEncoder = false)
        val av1Enc = inspectCodec(infos, MIME_AV1, isEncoder = true)
        val hevcDec = inspectCodec(infos, MIME_HEVC, isEncoder = false)
        val hevcEnc = inspectCodec(infos, MIME_HEVC, isEncoder = true)
        val h264Dec = inspectCodec(infos, MIME_H264, isEncoder = false)
        val h264Enc = inspectCodec(infos, MIME_H264, isEncoder = true)

        val preferredCodec = when {
            av1Enc.isHardwareEncoderAvailable && av1Dec.isHardwareDecoderAvailable -> MIME_AV1
            hevcEnc.isHardwareEncoderAvailable && hevcDec.isHardwareDecoderAvailable -> MIME_HEVC
            else -> MIME_H264
        }

        return DeviceCodecProfile(
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            av1Decoder = av1Dec,
            av1Encoder = av1Enc,
            hevcDecoder = hevcDec,
            hevcEncoder = hevcEnc,
            h264Decoder = h264Dec,
            h264Encoder = h264Enc,
            preferredCodecForSavings = preferredCodec
        )
    }

    private fun inspectCodec(infos: Array<MediaCodecInfo>, mimeType: String, isEncoder: Boolean): CodecCapabilities {
        val matchingCodecs = infos.filter { info ->
            info.isEncoder == isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
        }

        if (matchingCodecs.isEmpty()) {
            return CodecCapabilities(
                mimeType = mimeType,
                isHardwareDecoderAvailable = false,
                isHardwareEncoderAvailable = false,
                maxSupportedWidth = 1920,
                maxSupportedHeight = 1080,
                maxSupportedFps = 30,
                supports4k60 = false,
                supportsHdr = false
            )
        }

        val hwCodec = matchingCodecs.firstOrNull { isHardwareAccelerated(it) } ?: matchingCodecs.first()
        val isHardware = isHardwareAccelerated(hwCodec)

        var maxWidth = 1920
        var maxHeight = 1080
        var maxFps = 30
        var supports4k60 = false
        var supportsHdr = false

        try {
            val caps = hwCodec.getCapabilitiesForType(mimeType)
            val videoCaps = caps?.videoCapabilities
            if (videoCaps != null) {
                maxWidth = videoCaps.supportedWidths.upper
                maxHeight = videoCaps.supportedHeights.upper
                maxFps = videoCaps.supportedFrameRates.upper.toInt()
                supports4k60 = maxWidth >= 3840 && maxHeight >= 2160 && maxFps >= 60
            }
            if (caps?.colorFormats != null) {
                supportsHdr = caps.colorFormats.any {
                    it == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible ||
                            it == MediaCodecInfo.CodecCapabilities.COLOR_Format32bitABGR8888
                }
            }
        } catch (e: Exception) {
            // Fallback default capabilities
        }

        return CodecCapabilities(
            mimeType = mimeType,
            isHardwareDecoderAvailable = !isEncoder && isHardware,
            isHardwareEncoderAvailable = isEncoder && isHardware,
            maxSupportedWidth = maxWidth,
            maxSupportedHeight = maxHeight,
            maxSupportedFps = maxFps,
            supports4k60 = supports4k60,
            supportsHdr = supportsHdr
        )
    }

    private fun isHardwareAccelerated(info: MediaCodecInfo): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return info.isHardwareAccelerated
        }
        val name = info.name.lowercase()
        return !name.startsWith("omx.google.") &&
                !name.startsWith("c2.android.") &&
                !name.contains("sw") &&
                !name.contains("software")
    }
}
