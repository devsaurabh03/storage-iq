package com.storage.intelligence.feature.video.ui.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storage.intelligence.feature.video.codec.CodecCapabilityManager
import com.storage.intelligence.feature.video.codec.DeviceCodecProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceLabScreen(
    deviceProfile: DeviceCodecProfile,
    onRunBenchmark: (String) -> Unit,
    onRunMemoryLeakTest: () -> Unit,
    onViewCodecComparison: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Performance Lab",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header Explanation Banner
            item {
                Column {
                    Text(
                        text = "Measure how efficiently this device plays your videos.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dynamic Device Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "This Device Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileItem("Device Model", deviceProfile.deviceName)
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileItem("Android Version", deviceProfile.androidVersion)
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileItem("Preferred Codec", deviceProfile.preferredCodecForSavings)
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        
                        CodecStatusItem("AV1 Decoder", deviceProfile.av1Decoder.isHardwareDecoderAvailable, deviceProfile.av1Decoder.isHardwareDecoderAvailable)
                        CodecStatusItem("AV1 Encoder", deviceProfile.av1Encoder.isHardwareEncoderAvailable, deviceProfile.av1Encoder.isHardwareEncoderAvailable)
                        CodecStatusItem("HEVC Decoder", deviceProfile.hevcDecoder.isHardwareDecoderAvailable, deviceProfile.hevcDecoder.isHardwareDecoderAvailable)
                        CodecStatusItem("H.264 Decoder", deviceProfile.h264Decoder.isHardwareDecoderAvailable, deviceProfile.h264Decoder.isHardwareDecoderAvailable)
                        CodecStatusItem("4K / 60 FPS AV1", deviceProfile.av1Decoder.supports4k60, true)
                    }
                }
            }

            // Benchmark Test Suite
            item {
                Text(
                    text = "Benchmark Test Suite",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Standard Test Assets",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        BenchmarkOptionRow("1080p AV1 30 FPS", "Fast baseline decoder test") { onRunBenchmark("1080p_av1_30") }
                        BenchmarkOptionRow("1080p AV1 60 FPS", "Smooth motion playback test") { onRunBenchmark("1080p_av1_60") }
                        BenchmarkOptionRow("4K AV1 30 FPS", "High resolution hardware stress test") { onRunBenchmark("4k_av1_30") }
                        BenchmarkOptionRow("4K AV1 60 FPS", "Maximum pipeline throughput test") { onRunBenchmark("4k_av1_60") }
                        BenchmarkOptionRow("4K HDR AV1", "Wide color gamut dynamic test") { onRunBenchmark("4k_hdr_av1") }
                    }
                }
            }

            // Codec Comparison & Memory Stability Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onViewCodecComparison,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(
                            text = "AV1 vs HEVC vs H.264",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onRunMemoryLeakTest,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Text(
                            text = "20× Memory Leak Test",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CodecStatusItem(label: String, supported: Boolean, hardware: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (supported) if (hardware) "Hardware ✓" else "Software ✓" else "Unsupported ✗",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (supported) (if (hardware) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary) else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun BenchmarkOptionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(10.dp)) {
            Text(text = "Run")
        }
    }
}
