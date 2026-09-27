package com.storage.intelligence.feature.me

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.datastore.UserPreferencesRepository
import com.storage.intelligence.feature.performance.model.CpuArchitecture
import com.storage.intelligence.feature.performance.model.CpuIdentity
import com.storage.intelligence.feature.performance.model.CpuVendor
import com.storage.intelligence.feature.performance.provider.BatteryInfoProvider
import com.storage.intelligence.feature.performance.provider.CpuInfoProvider
import com.storage.intelligence.feature.performance.provider.RamInfoProvider
import com.storage.intelligence.feature.performance.provider.ThermalInfoProvider
import com.storage.intelligence.feature.video.codec.CodecCapabilities
import com.storage.intelligence.feature.video.codec.CodecCapabilityManager
import com.storage.intelligence.feature.video.codec.DeviceCodecProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MeUiState(
    val cpuIdentity: CpuIdentity,
    val codecProfile: DeviceCodecProfile,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val thermalStateName: String,
    val optimizedVideosCount: Int,
    val savedVideoBytes: Long,
    val optimizedPhotosCount: Int,
    val savedPhotoBytes: Long,
    val onlyWhileCharging: Boolean,
    val pauseOnLowBattery: Boolean,
    val pauseOnThermalStress: Boolean,
    val optInTelemetry: Boolean
)

private data class ActivityStats(
    val optimizedVideosCount: Int,
    val savedVideoBytes: Long,
    val optimizedPhotosCount: Int,
    val savedPhotoBytes: Long
)

private data class PolicySettings(
    val onlyWhileCharging: Boolean,
    val pauseOnLowBattery: Boolean,
    val pauseOnThermalStress: Boolean,
    val optInTelemetry: Boolean
)

private val defaultCapabilities = CodecCapabilities(
    mimeType = "",
    isHardwareDecoderAvailable = true,
    isHardwareEncoderAvailable = false,
    maxSupportedWidth = 1920,
    maxSupportedHeight = 1080,
    maxSupportedFps = 60,
    supports4k60 = false,
    supportsHdr = false
)

private val defaultCodecProfile = DeviceCodecProfile(
    deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
    androidVersion = "Android ${Build.VERSION.RELEASE}",
    av1Decoder = defaultCapabilities.copy(mimeType = CodecCapabilityManager.MIME_AV1),
    av1Encoder = defaultCapabilities.copy(mimeType = CodecCapabilityManager.MIME_AV1),
    hevcDecoder = defaultCapabilities.copy(mimeType = CodecCapabilityManager.MIME_HEVC),
    hevcEncoder = defaultCapabilities.copy(mimeType = CodecCapabilityManager.MIME_HEVC),
    h264Decoder = defaultCapabilities.copy(mimeType = CodecCapabilityManager.MIME_H264),
    h264Encoder = defaultCapabilities.copy(mimeType = CodecCapabilityManager.MIME_H264),
    preferredCodecForSavings = "HEVC"
)

private val defaultCpu = CpuIdentity(
    vendor = CpuVendor.OTHER,
    socName = Build.HARDWARE.ifEmpty { "ARM64 Processor" },
    architecture = CpuArchitecture.ARMv8_A,
    abi = "arm64-v8a",
    is64Bit = true,
    logicalCoreCount = Runtime.getRuntime().availableProcessors(),
    topologyType = "Multi-Core",
    clusters = emptyList(),
    supportedFeatures = emptyList()
)

@HiltViewModel
class MeViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val cpuInfoProvider: CpuInfoProvider,
    private val ramInfoProvider: RamInfoProvider,
    private val batteryInfoProvider: BatteryInfoProvider,
    private val thermalInfoProvider: ThermalInfoProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MeUiState(
            cpuIdentity = defaultCpu,
            codecProfile = defaultCodecProfile,
            totalRamBytes = 8L * 1024 * 1024 * 1024,
            availableRamBytes = 4L * 1024 * 1024 * 1024,
            batteryLevel = 100,
            isCharging = false,
            thermalStateName = "Normal",
            optimizedVideosCount = 0,
            savedVideoBytes = 0L,
            optimizedPhotosCount = 0,
            savedPhotoBytes = 0L,
            onlyWhileCharging = false,
            pauseOnLowBattery = true,
            pauseOnThermalStress = true,
            optInTelemetry = false
        )
    )
    val uiState: StateFlow<MeUiState> = _uiState.asStateFlow()

    init {
        loadHardwareInfo()
        observePreferences()
    }

    private fun loadHardwareInfo() {
        viewModelScope.launch {
            val detectedCodecs = try {
                CodecCapabilityManager.detectCapabilities()
            } catch (_: Throwable) {
                defaultCodecProfile
            }
            val cpu = try { cpuInfoProvider.getCpuIdentity() } catch (_: Throwable) { defaultCpu }
            val ram = try { ramInfoProvider.getRamInfo() } catch (_: Throwable) { null }
            val battery = try { batteryInfoProvider.getBatteryInfo() } catch (_: Throwable) { null }
            val thermal = try { thermalInfoProvider.getThermalInfo() } catch (_: Throwable) { null }

            _uiState.update { current ->
                current.copy(
                    cpuIdentity = cpu,
                    codecProfile = detectedCodecs,
                    totalRamBytes = ram?.totalBytes ?: current.totalRamBytes,
                    availableRamBytes = ram?.availableBytes ?: current.availableRamBytes,
                    batteryLevel = battery?.levelPercent ?: current.batteryLevel,
                    isCharging = battery?.isCharging ?: current.isCharging,
                    thermalStateName = thermal?.state?.displayName ?: current.thermalStateName
                )
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            val activityStatsFlow = combine(
                preferencesRepository.optimizedVideosCount,
                preferencesRepository.savedVideoBytes,
                preferencesRepository.optimizedPhotosCount,
                preferencesRepository.savedPhotoBytes
            ) { optVideos, savedVideo, optPhotos, savedPhoto ->
                ActivityStats(optVideos, savedVideo, optPhotos, savedPhoto)
            }

            val policySettingsFlow = combine(
                preferencesRepository.onlyWhileCharging,
                preferencesRepository.pauseOnLowBattery,
                preferencesRepository.pauseOnThermalStress,
                preferencesRepository.optInTelemetry
            ) { onlyCharging, pauseLowBat, pauseThermal, telemetry ->
                PolicySettings(onlyCharging, pauseLowBat, pauseThermal, telemetry)
            }

            combine(activityStatsFlow, policySettingsFlow) { stats, policies ->
                Pair(stats, policies)
            }.collect { (stats, policies) ->
                _uiState.update { current ->
                    current.copy(
                        optimizedVideosCount = stats.optimizedVideosCount,
                        savedVideoBytes = stats.savedVideoBytes,
                        optimizedPhotosCount = stats.optimizedPhotosCount,
                        savedPhotoBytes = stats.savedPhotoBytes,
                        onlyWhileCharging = policies.onlyWhileCharging,
                        pauseOnLowBattery = policies.pauseOnLowBattery,
                        pauseOnThermalStress = policies.pauseOnThermalStress,
                        optInTelemetry = policies.optInTelemetry
                    )
                }
            }
        }
    }

    fun setOnlyWhileCharging(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setOnlyWhileCharging(enabled) }
    }

    fun setPauseOnLowBattery(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setPauseOnLowBattery(enabled) }
    }

    fun setPauseOnThermalStress(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setPauseOnThermalStress(enabled) }
    }

    fun setOptInTelemetry(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setOptInTelemetry(enabled) }
    }
}

