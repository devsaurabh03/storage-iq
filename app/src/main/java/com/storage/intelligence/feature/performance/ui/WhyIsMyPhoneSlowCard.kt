package com.storage.intelligence.feature.performance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storage.intelligence.core.ui.components.PerformanceChart
import com.storage.intelligence.core.ui.theme.EmeraldPrimary
import com.storage.intelligence.core.ui.theme.WarningAmber
import com.storage.intelligence.feature.performance.engine.CpuSnapshot
import com.storage.intelligence.feature.performance.engine.LoadTrend
import com.storage.intelligence.feature.performance.model.ThermalState

@Composable
fun WhyIsMyPhoneSlowCard(
    cpuSnapshot: CpuSnapshot?,
    usedRamPercent: Int = 45,
    onSeeDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLoad = cpuSnapshot?.utilization?.toInt() ?: 20
    val thermalState = cpuSnapshot?.thermalState ?: ThermalState.NORMAL
    val isWarm = thermalState != ThermalState.NORMAL
    val isHighCpu = currentLoad > 65
    val isHealthy = !isWarm && !isHighCpu && usedRamPercent < 85

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isWarm -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                isHighCpu -> WarningAmber.copy(alpha = 0.12f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isWarm -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                    isHighCpu -> WarningAmber.copy(alpha = 0.15f)
                                    else -> EmeraldPrimary.copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isWarm -> Icons.Default.Thermostat
                                isHighCpu -> Icons.Default.Speed
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = when {
                                isWarm -> MaterialTheme.colorScheme.error
                                isHighCpu -> WarningAmber
                                else -> EmeraldPrimary
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isWarm) "Performance issue detected" else "Why is my phone slow?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isHealthy) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Healthy",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body text
            when {
                isWarm -> {
                    Text(
                        text = "Your device is temporarily reducing performance because it is running warm (${thermalState.displayName}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onSeeDetails,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("See Why")
                    }
                }
                isHighCpu -> {
                    Text(
                        text = "CPU activity is currently elevated (${currentLoad}%). ${cpuSnapshot?.loadTrend?.symbol ?: ""} ${cpuSnapshot?.loadTrend?.label ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (cpuSnapshot?.mainWorkload != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Main workload: ${cpuSnapshot.mainWorkload}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = onSeeDetails,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("See Details")
                    }
                }
                else -> {
                    Text(
                        text = "You're not experiencing any significant performance issues right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusPill(label = "CPU", value = "Normal (${currentLoad}%)")
                        StatusPill(label = "Memory", value = "Normal (${usedRamPercent}%)")
                        StatusPill(label = "Thermal", value = thermalState.displayName)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nothing needs attention",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = EmeraldPrimary
                        )
                    }
                }
            }

            // Rolling Real-Time CPU Graph
            if (cpuSnapshot != null && cpuSnapshot.history.size > 2) {
                Spacer(modifier = Modifier.height(16.dp))
                PerformanceChart(
                    values = cpuSnapshot.history,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    lineColor = if (isWarm) MaterialTheme.colorScheme.error else if (isHighCpu) WarningAmber else EmeraldPrimary
                )
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}
