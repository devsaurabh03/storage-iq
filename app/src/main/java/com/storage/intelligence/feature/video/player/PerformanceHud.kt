package com.storage.intelligence.feature.video.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storage.intelligence.core.ui.theme.EmeraldPrimary
import com.storage.intelligence.core.ui.theme.WarningAmber

enum class HudMode { COMPACT, DETAILED }

@Composable
fun PerformanceHud(
    telemetry: LivePlayerTelemetry,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    var mode by remember { mutableStateOf(HudMode.DETAILED) }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { mode = if (mode == HudMode.DETAILED) HudMode.COMPACT else HudMode.DETAILED },
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmeraldPrimary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = telemetry.codecName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE HUD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (onClose != null) {
                    IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (mode == HudMode.COMPACT) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudCompactMetric("FPS", "%.1f".format(telemetry.fps), EmeraldPrimary)
                    HudCompactMetric("CPU", "${telemetry.cpuPercent}%", Color.White)
                    HudCompactMetric("RAM", "${telemetry.ramUsedMb}MB", Color.White)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HudMetricRow("FPS", "%.1f".format(telemetry.fps), if (telemetry.fps >= 55) EmeraldPrimary else WarningAmber)
                    HudMetricRow("CPU", "${telemetry.cpuPercent}%")
                    HudMetricRow("RAM", "${telemetry.ramUsedMb} MB")
                    HudMetricRow("GPU", "${telemetry.gpuPercent}%")
                    HudMetricRow("DROPPED", "${telemetry.droppedFrames}", if (telemetry.droppedFrames > 0) WarningAmber else EmeraldPrimary)
                    HudMetricRow("TEMP", "${"%.1f".format(telemetry.tempCelsius)}°C")
                    if (telemetry.timeToFirstFrameMs > 0) {
                        HudMetricRow("STARTUP", "${telemetry.timeToFirstFrameMs} ms", EmeraldPrimary)
                    }
                    if (telemetry.lastSeekLatencyMs > 0) {
                        HudMetricRow("SEEK LATENCY", "${telemetry.lastSeekLatencyMs} ms", EmeraldPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun HudMetricRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun HudCompactMetric(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    }
}
