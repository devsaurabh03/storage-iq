package com.storage.intelligence.feature.dashboard

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.core.ui.components.TrustCenterSheet
import com.storage.intelligence.core.ui.components.WorldFactsLoadingView
import com.storage.intelligence.core.ui.theme.*
import com.storage.intelligence.domain.model.StorageCategory
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToReview: () -> Unit,
    onNavigateToCategory: (StorageCategory) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToTimeline: () -> Unit = {},
    onNavigateToOtherStorage: () -> Unit = {},
    onNavigateToMessaging: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToPerformance: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cpuSnapshot by viewModel.cpuSnapshot.collectAsState()
    val telemetry by viewModel.systemTelemetry.collectAsState()
    var showTrustCenterSheet by remember { mutableStateOf(false) }

    if (showTrustCenterSheet) {
        TrustCenterSheet(onDismiss = { showTrustCenterSheet = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceOled)
    ) {
        // Ambient VisionOS Glow Orbs
        AmbientBackgroundGlows()

        when (val state = uiState) {
            is Result.Loading -> {
                WorldFactsLoadingView(
                    statusTitle = "Analyzing Storage Partitions…",
                    statusSubtitle = "Reading filesystem metrics and storage allocations",
                    accentColor = PrimaryCyan
                )
            }

            is Result.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = ErrorRose,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Unable to analyze storage partition",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextOnSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.rescanStorage() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.4f))
                    ) {
                        Text("Retry Analysis", color = PrimaryCyan)
                    }
                }
            }

            is Result.Success -> {
                val overview = state.data.overview
                val growth = state.data.growthReport
                val isScanning = state.data.isScanning
                val scanText = state.data.scanProgressText
                val recoverableBytes = overview.recoverableSummary.totalRecoverableBytes

                val videosCategory = overview.categoryBreakdowns.find { it.category == StorageCategory.VIDEOS }
                val photosCategory = overview.categoryBreakdowns.find { it.category == StorageCategory.PHOTOS }
                val downloadsCategory = overview.categoryBreakdowns.find { it.category == StorageCategory.DOWNLOADS }
                val messagingCategory = overview.categoryBreakdowns.find {
                    it.category == StorageCategory.WHATSAPP || it.category == StorageCategory.TELEGRAM
                }

                val duplicateTotalBytes = overview.recoverableSummary.duplicateVideosBytes + overview.recoverableSummary.duplicatePhotosBytes
                val messagingBytes = overview.messagingSummary?.totalMessagingBytes ?: (messagingCategory?.totalSizeBytes ?: 0L)

                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 18.dp)
                ) {
                    // Safe top spacing
                    Spacer(modifier = Modifier.statusBarsPadding())
                    Spacer(modifier = Modifier.height(10.dp))

                    // Minimal Header (VisionOS Glass) with 100% Real Live Battery & Thermal Telemetry
                    VisionOsHeader(
                        batteryPercent = telemetry.batteryPercent,
                        tempCelsius = telemetry.tempCelsius,
                        isCharging = telemetry.isCharging,
                        onPerformanceClick = onNavigateToPerformance,
                        onRescanClick = { viewModel.rescanStorage() },
                        onTrustCenterClick = { showTrustCenterSheet = true },
                        onSearchClick = onNavigateToSearch,
                        onSettingsClick = onNavigateToSettings
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Incremental Scanning Banner
                    AnimatedVisibility(visible = isScanning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(PrimaryCyan.copy(alpha = 0.08f))
                                .border(1.dp, PrimaryCyan.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = PrimaryCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = scanText ?: "Updating storage partition index…",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryCyanLight
                                )
                            }
                        }
                    }

                    // Fluid Hardware Telemetry Strip (100% Real OS Metrics: CPU, RAM, Temp, Partition)
                    HardwareTelemetryStrip(
                        cpuPercent = (cpuSnapshot?.utilization ?: 0f).toInt(),
                        ramUsedBytes = telemetry.ramUsedBytes,
                        ramTotalBytes = telemetry.ramTotalBytes,
                        tempCelsius = telemetry.tempCelsius,
                        usedStoragePercent = overview.usedPercentage,
                        onClick = onNavigateToPerformance
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Hero Card: Ethereal Fluid Light Monolith (100% Real Device Storage Analysis)
                    EtherealHeroCard(
                        usedBytes = overview.usedDeviceSizeBytes,
                        totalBytes = overview.totalDeviceSizeBytes,
                        usedPercentage = overview.usedPercentage,
                        recoverableBytes = recoverableBytes,
                        onQuickCleanClick = onNavigateToReview
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── OFFICIAL STORAGEIQ RAM INTELLIGENCE TILE ──────────────────────────
                    com.storage.intelligence.feature.ram.ui.RamDashboardTile(
                        ramUsedBytes = telemetry.ramUsedBytes,
                        ramTotalBytes = telemetry.ramTotalBytes,
                        pressure = telemetry.ramPressure,
                        isLoading = telemetry.ramTotalBytes <= 0L,
                        onClick = onNavigateToPerformance
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section Header: Flagship Autonomous Engines
                    SectionHeader(
                        title = "Flagship Autonomous Engines",
                        subtitle = "Next-gen deep optimization with zero data loss guarantee",
                        badgeText = "2 Ready"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Card 1: Media Optimizer (AV1 Transcode & Photos — 100% Real Media Analysis)
                    MediaOptimizerCard(
                        videoCount = videosCategory?.fileCount ?: 0,
                        videoBytes = videosCategory?.totalSizeBytes ?: 0L,
                        photoCount = photosCategory?.fileCount ?: 0,
                        photoBytes = photosCategory?.totalSizeBytes ?: 0L,
                        onLaunchMediaStudio = { onNavigateToCategory(StorageCategory.VIDEOS) },
                        onPhotoClick = { onNavigateToCategory(StorageCategory.PHOTOS) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Card 2: Duplicate File Detection (Cryptographic Shield — 100% Real Duplicate Scan Analysis)
                    DuplicateDetectionCard(
                        duplicateVideoBytes = overview.recoverableSummary.duplicateVideosBytes,
                        duplicatePhotoBytes = overview.recoverableSummary.duplicatePhotosBytes,
                        totalPurgeableBytes = duplicateTotalBytes,
                        onReviewDeduplicate = onNavigateToReview
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section Header: Secondary Maintenance Purges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Secondary Maintenance Purges",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextOnSurface.copy(alpha = 0.85f)
                            )
                        }
                        Text(
                            text = "3 Modules",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Maintenance Purge Card (100% Real Scanned Values)
                    SecondaryMaintenanceCard(
                        staleBytes = overview.recoverableSummary.otherRecoverableBytes,
                        downloadsBytes = downloadsCategory?.totalSizeBytes ?: overview.recoverableSummary.oldDownloadsBytes,
                        bufferBytes = messagingBytes,
                        onStaleClick = onNavigateToTimeline,
                        onDownloadsClick = { onNavigateToCategory(StorageCategory.DOWNLOADS) },
                        onBuffersClick = onNavigateToMessaging
                    )

                    // Storage Growth Report (Real growth calculation)
                    if (growth != null && growth.rankedCategoryGrowths.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        StorageGrowthBanner(
                            growthText = "${growth.rankedCategoryGrowths.first().categoryName} had highest recent growth (${FormattingUtils.formatBytes(growth.netGrowthBytes)}).",
                            onClick = onNavigateToTimeline
                        )
                    }

                    // Extra space so the bottom dock never overlaps content
                    Spacer(modifier = Modifier.height(84.dp))
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VisionOS Ambient Glowing Orbs
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BoxScope.AmbientBackgroundGlows() {
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientGlowTransition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    // Top Center Cyan Orb
    Canvas(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = (-80).dp)
            .size(360.dp)
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AmbientCyan.copy(alpha = glowAlpha), Color.Transparent),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.width / 2
            )
        )
    }

    // Mid-Right Indigo Orb
    Canvas(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 90.dp, y = 320.dp)
            .size(280.dp)
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AmbientIndigo.copy(alpha = glowAlpha * 0.9f), Color.Transparent),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.width / 2
            )
        )
    }

    // Bottom-Left Emerald Orb
    Canvas(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .offset(x = (-70).dp, y = (-120).dp)
            .size(250.dp)
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AmbientEmerald.copy(alpha = glowAlpha * 0.8f), Color.Transparent),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.width / 2
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VisionOS Minimal Top Header with Real Battery & Thermal Telemetry
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VisionOsHeader(
    batteryPercent: Int,
    tempCelsius: Double,
    isCharging: Boolean,
    onPerformanceClick: () -> Unit,
    onRescanClick: () -> Unit,
    onTrustCenterClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF04060A).copy(alpha = 0.75f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onTrustCenterClick)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Storage IQ",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextOnSurface,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34D399))
            )
        }

        // Action Pill + Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Live Real-Time Battery & Thermal Telemetry Pill
            if (batteryPercent > 0) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable(onClick = onPerformanceClick)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (isCharging) Color(0xFF34D399) else PrimaryCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$batteryPercent%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    if (tempCelsius > 0) {
                        Text(
                            text = " · ",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.25f)
                        )
                        Text(
                            text = String.format(Locale.US, "%.0f°", tempCelsius),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // Rescan
            IconButton(
                onClick = onRescanClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rescan",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Search
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Profile / Settings
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hardware Telemetry Strip (100% Real Device Analysis)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HardwareTelemetryStrip(
    cpuPercent: Int,
    ramUsedBytes: Long,
    ramTotalBytes: Long,
    tempCelsius: Double,
    usedStoragePercent: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.02f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
            .clickable(onClick = onClick)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TelemetryChip(color = PrimaryCyan, label = "CPU", value = "$cpuPercent%")
        if (ramUsedBytes > 0) {
            Text(text = "•", color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 8.dp))
            TelemetryChip(color = SecondaryEmerald, label = "RAM", value = FormattingUtils.formatBytes(ramUsedBytes))
        }
        if (tempCelsius > 0) {
            Text(text = "•", color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 8.dp))
            TelemetryChip(color = WarningAmber, label = "", value = String.format(Locale.US, "%.1f°C", tempCelsius))
        }
        Text(text = "•", color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 8.dp))
        TelemetryChip(color = PrimaryCyan, label = "STORAGE", value = "$usedStoragePercent% Used")
    }
}

@Composable
private fun TelemetryChip(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        if (label.isNotEmpty()) {
            Text(
                text = "$label ",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Card: Ethereal Fluid Light Monolith (Real Hardware Storage Partition)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EtherealHeroCard(
    usedBytes: Long,
    totalBytes: Long,
    usedPercentage: Int,
    recoverableBytes: Long,
    onQuickCleanClick: () -> Unit
) {
    val deviceHardware = remember {
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        "$manufacturer $model"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.015f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
            .padding(22.dp)
    ) {
        // Internal glow bleed
        Canvas(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(80.dp)
        ) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryCyan.copy(alpha = 0.15f), Color.Transparent)
                )
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Partition Status (Real Device Hardware Identity)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(PrimaryCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "INTERNAL STORAGE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.45f),
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = deviceHardware,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fluid Arc Gauge
            FluidArcGauge(
                usedBytes = usedBytes,
                totalBytes = totalBytes,
                usedPercentage = usedPercentage,
                size = 210.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Real Reclaimable Savings Pill
            val hasSavings = recoverableBytes > 0
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PrimaryCyan.copy(alpha = 0.08f))
                    .border(1.dp, PrimaryCyan.copy(alpha = 0.25f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasSavings) "${FormattingUtils.formatBytes(recoverableBytes)} Available to Free" else "Partition Fully Optimal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryCyanLight
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // VisionOS Floating Glass Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PrimaryCyan.copy(alpha = 0.22f),
                                PrimaryCyan.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(1.dp, PrimaryCyan.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .clickable(onClick = onQuickCleanClick)
                    .padding(vertical = 14.dp, horizontal = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Clean System",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = (-0.2).sp
                    )
                    if (hasSavings) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.30f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = FormattingUtils.formatBytes(recoverableBytes),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = PrimaryCyanLight.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fluid Arc Progress Gauge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FluidArcGauge(
    usedBytes: Long,
    totalBytes: Long,
    usedPercentage: Int,
    size: Dp
) {
    val fraction = remember(usedBytes, totalBytes) {
        if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
    }
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "FluidGauge"
    )

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Track 1: Subtle white track
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth - 1.dp.toPx(), cap = StrokeCap.Round)
            )

            // Track 2: Secondary subtle segment (linear feel)
            drawArc(
                color = SecondaryEmerald.copy(alpha = 0.18f),
                startAngle = 100f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth - 1.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dynamic Fluid Cyan Arc with gradient
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        SecondaryEmerald,
                        PrimaryCyan,
                        Color(0xFF38BDF8),
                        SecondaryEmerald
                    ),
                    center = Offset(this.size.width / 2, this.size.height / 2)
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center Numerical Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "PARTITION USED",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.45f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = FormattingUtils.formatBytes(usedBytes),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    .padding(horizontal = 9.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(PrimaryCyan)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "$usedPercentage% of ${FormattingUtils.formatBytes(totalBytes)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Header: Balanced, responsive, zero awkward wraps
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    badgeText: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(PrimaryCyan)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextOnSurface,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PrimaryCyan.copy(alpha = 0.08f))
                    .border(1.dp, PrimaryCyan.copy(alpha = 0.25f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = PrimaryCyanLight,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.45f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Flagship 1: Media Optimizer Card (100% Real Live Media Analysis)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MediaOptimizerCard(
    videoCount: Int,
    videoBytes: Long,
    photoCount: Int,
    photoBytes: Long,
    onLaunchMediaStudio: () -> Unit,
    onPhotoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
            .clickable(onClick = onLaunchMediaStudio)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header row with non-shrinking horizontal badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryCyan.copy(alpha = 0.12f))
                            .border(1.dp, PrimaryCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = PrimaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Media Optimizer",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(PrimaryCyan.copy(alpha = 0.10f))
                                    .border(1.dp, PrimaryCyan.copy(alpha = 0.20f), CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Photos & 4K",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = PrimaryCyanLight,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Intelligent curation",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(PrimaryCyan.copy(alpha = 0.10f))
                        .border(1.dp, PrimaryCyan.copy(alpha = 0.25f), CircleShape)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+${FormattingUtils.formatBytes(videoBytes)} Reclaimable",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryCyan,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Dual Previews (4K Video & Burst Photo — Dynamic based on scanned media)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Video Transcode Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onLaunchMediaStudio)
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .border(1.dp, PrimaryCyan.copy(alpha = 0.25f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AV1 / HEVC",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = PrimaryCyan,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Text(
                        text = "$videoCount Videos • ${FormattingUtils.formatBytes(videoBytes)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        color = Color(0xFF6EE7B7),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }

                // Photo Burst Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E1B4B).copy(alpha = 0.7f), Color(0xFF030712))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onPhotoClick)
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(5.dp))
                            .background(WarningAmber.copy(alpha = 0.20f))
                            .border(1.dp, WarningAmber.copy(alpha = 0.35f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AI Best-Shot",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFFFDE68A),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Text(
                        text = "$photoCount Photos • ${FormattingUtils.formatBytes(photoBytes)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }

            // Stats Sub-row (100% Real Live Media Statistics)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$videoCount Videos",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                        Text(
                            text = FormattingUtils.formatBytes(videoBytes),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryCyan,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$photoCount Photos",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                        Text(
                            text = FormattingUtils.formatBytes(photoBytes),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryEmerald,
                            maxLines = 1
                        )
                    }
                }
            }

            // Footer action
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Batch Transcode & Curate",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Launch Media Studio",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Flagship 2: Duplicate File Detection Card (100% Real Live Duplicate Analysis)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DuplicateDetectionCard(
    duplicateVideoBytes: Long,
    duplicatePhotoBytes: Long,
    totalPurgeableBytes: Long,
    onReviewDeduplicate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
            .clickable(onClick = onReviewDeduplicate)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Row with non-shrinking horizontal badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SecondaryEmerald.copy(alpha = 0.12f))
                            .border(1.dp, SecondaryEmerald.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = SecondaryEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Duplicate Detection",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SecondaryEmerald.copy(alpha = 0.10f))
                                    .border(1.dp, SecondaryEmerald.copy(alpha = 0.20f), CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Crypto Shield",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = SecondaryEmerald,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bit-by-bit scanner",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SecondaryEmerald.copy(alpha = 0.10f))
                        .border(1.dp, SecondaryEmerald.copy(alpha = 0.25f), CircleShape)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+${FormattingUtils.formatBytes(totalPurgeableBytes)} Purgeable",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryEmerald,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Comparison Visual Box (Real Live Duplicate Video vs Photo Analysis)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = PrimaryCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SHA-256 Verified",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = PrimaryCyan,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = "Original Protected",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = SecondaryEmerald,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Real Live Scanned Duplicate Categories
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Master Media Copy
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "MASTER COPIES",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryEmerald
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = SecondaryEmerald,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (duplicateVideoBytes > 0) "Duplicate Videos Found" else "No Duplicate Videos",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextOnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = FormattingUtils.formatBytes(duplicateVideoBytes),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Duplicate Clones
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(1.dp, PrimaryCyan.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CLONE MEDIA",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryCyan
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = PrimaryCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (duplicatePhotoBytes > 0) "Duplicate Photos Found" else "No Duplicate Photos",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextOnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = FormattingUtils.formatBytes(duplicatePhotoBytes),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = PrimaryCyanLight.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Zero-Risk Auto-Select",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "100% Cryptographic Match",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Footer
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Safely removes clone copies only",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Review & Deduplicate",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryEmerald
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = SecondaryEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Secondary Maintenance Purge Card (100% Real Scanned Values)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SecondaryMaintenanceCard(
    staleBytes: Long,
    downloadsBytes: Long,
    bufferBytes: Long,
    onStaleClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onBuffersClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
            .padding(8.dp)
    ) {
        Column {
            // Row 1: Stale Files (100% Real Scanned Size)
            MaintenanceRow(
                icon = Icons.Default.History,
                iconTint = WarningAmber,
                iconBg = WarningAmber.copy(alpha = 0.12f),
                title = "Stale Files",
                tag = "> 90d Idle",
                tagColor = WarningAmber,
                description = "Dormant files untouched in 3 months",
                value = if (staleBytes > 0) "+${FormattingUtils.formatBytes(staleBytes)}" else "0 B",
                onClick = onStaleClick
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.04f))

            // Row 2: Downloads & APKs (100% Real Scanned Size)
            MaintenanceRow(
                icon = Icons.Default.Download,
                iconTint = Color(0xFF38BDF8),
                iconBg = Color(0xFF38BDF8).copy(alpha = 0.12f),
                title = "Downloads & APKs",
                tag = "Archives",
                tagColor = Color(0xFF38BDF8),
                description = "Obsolete installation archives & APKs",
                value = if (downloadsBytes > 0) "+${FormattingUtils.formatBytes(downloadsBytes)}" else "0 B",
                onClick = onDownloadsClick
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.04f))

            // Row 3: System & App Buffers (100% Real Scanned Size)
            MaintenanceRow(
                icon = Icons.Default.CleaningServices,
                iconTint = SecondaryEmerald,
                iconBg = SecondaryEmerald.copy(alpha = 0.12f),
                title = "Social & App Buffers",
                tag = "Safe Purge",
                tagColor = SecondaryEmerald,
                description = "Volatile runtime junk & messaging caches",
                value = if (bufferBytes > 0) "+${FormattingUtils.formatBytes(bufferBytes)}" else "0 B",
                onClick = onBuffersClick
            )
        }
    }
}

@Composable
private fun MaintenanceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    tag: String,
    tagColor: Color,
    description: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg)
                    .border(1.dp, iconTint.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(tagColor.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = tagColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = iconTint,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Storage Growth Banner
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StorageGrowthBanner(
    growthText: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = PrimaryCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Storage Growth Report",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextOnSurface
                )
                Text(
                    text = growthText,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
