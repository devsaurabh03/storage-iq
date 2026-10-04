package com.storage.intelligence.feature.performance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.core.ui.components.WorldFactsLoadingView
import com.storage.intelligence.core.ui.theme.EmeraldPrimary
import com.storage.intelligence.core.ui.theme.WarningAmber
import com.storage.intelligence.feature.performance.model.RamPressure
import com.storage.intelligence.feature.performance.model.ThermalState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceDashboardScreen(
    viewModel: PerformanceViewModel,
    onNavigateToCpuDetails: () -> Unit,
    onNavigateToSlowDiagnosis: () -> Unit,
    onNavigateToHotDiagnosis: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Performance Intelligence",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                WorldFactsLoadingView(
                    statusTitle = "Sampling System Telemetry…",
                    statusSubtitle = "Reading CPU utilization, memory pressure, and thermal levels",
                    accentColor = EmeraldPrimary
                )
            } else {
                val snapshot = uiState.snapshot
                val identity = uiState.cpuIdentity

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // HUMAN TOP-LEVEL PERFORMANCE HERO CARD
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "PERFORMANCE STATUS",
                                        style = MaterialTheme.typography.labelSmall,
                                        letterSpacing = 1.1.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (snapshot != null && snapshot.thermalInfo.isThrottlingDetected) "Phone is Running Warm" else "Running Normally",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${identity?.socName ?: "System SoC"} • ${identity?.architecture?.displayName ?: "ARM64"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // DIAGNOSTIC ACTIONS: "WHY IS MY PHONE SLOW / HOT?"
                    Text(
                        text = "DIAGNOSTICS & CHECKS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(onClick = onNavigateToSlowDiagnosis),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Why is my phone slow?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Run diagnostic check",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(onClick = onNavigateToHotDiagnosis),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Thermostat,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6D00),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Why is my phone hot?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Check thermal sources",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // REASSURING RAM EXPLANATION CARD (TRUST BUILDING)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Android manages RAM automatically to keep apps responsive. High RAM usage is normal and keeps recently used apps ready.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // LIVE TELEMETRY METRIC CARDS
                    if (snapshot != null) {
                        Text(
                            text = "SYSTEM METRICS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // RAM Card
                            MetricCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToRoute(com.storage.intelligence.feature.navigation.Routes.RAM_ANALYSIS) },
                                title = "RAM Memory",
                                value = FormattingUtils.formatBytes(snapshot.ramInfo.usedBytes),
                                subtitle = "of ${FormattingUtils.formatBytes(snapshot.ramInfo.totalBytes)} total",
                                status = snapshot.ramInfo.pressure.displayName,
                                statusColor = when (snapshot.ramInfo.pressure) {
                                    RamPressure.HIGH -> Color(0xFFE53935)
                                    RamPressure.MODERATE -> WarningAmber
                                    else -> EmeraldPrimary
                                }
                            )

                            // CPU Card
                            MetricCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClick = onNavigateToCpuDetails),
                                title = "CPU Activity",
                                value = "${snapshot.cpuActivity.currentLoadPercent}%",
                                subtitle = "${identity?.logicalCoreCount ?: 8} Cores",
                                status = snapshot.cpuActivity.trend,
                                statusColor = if (snapshot.cpuActivity.currentLoadPercent > 70) Color(0xFFE53935) else EmeraldPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Thermal Card
                            MetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Temperature",
                                value = "${snapshot.batteryInfo.tempCelsius}°C",
                                subtitle = snapshot.thermalInfo.state.displayName,
                                status = if (snapshot.thermalInfo.isThrottlingDetected) "Throttling" else "Normal",
                                statusColor = when (snapshot.thermalInfo.state) {
                                    ThermalState.VERY_HOT, ThermalState.CRITICAL -> Color(0xFFE53935)
                                    ThermalState.HOT, ThermalState.WARM -> WarningAmber
                                    else -> EmeraldPrimary
                                }
                            )

                            // Battery Card
                            MetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Battery Level",
                                value = "${snapshot.batteryInfo.levelPercent}%",
                                subtitle = snapshot.batteryInfo.status,
                                status = if (snapshot.batteryInfo.isCharging) "Charging" else "Discharging",
                                statusColor = EmeraldPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // CPU INTELLIGENCE DEEP DIVE CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onNavigateToCpuDetails),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "CPU Intelligence & Benchmark",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Topology, core cluster scaling, benchmark health test",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    status: String,
    statusColor: Color
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(text = status, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}
