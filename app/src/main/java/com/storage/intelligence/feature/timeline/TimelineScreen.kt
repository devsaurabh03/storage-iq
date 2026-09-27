package com.storage.intelligence.feature.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.storage.intelligence.R
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.core.ui.theme.EmeraldPrimary
import com.storage.intelligence.core.ui.theme.WarningAmber
import com.storage.intelligence.domain.model.CategoryGrowth
import com.storage.intelligence.domain.model.StorageSnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val selectedDays by viewModel.selectedTimeframeDays.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.intel_timeline)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val s = state) {
                is Result.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is Result.Error -> Text(
                    text = "Error loading storage timeline",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
                is Result.Success -> {
                    val snapshots = s.data.snapshots
                    val growth = s.data.growthReport

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Timeframe filter chips
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(7, 30, 90, 365).forEach { days ->
                                    val label = when (days) {
                                        7 -> "7 Days"
                                        30 -> "30 Days"
                                        90 -> "90 Days"
                                        else -> "1 Year"
                                    }
                                    FilterChip(
                                        selected = selectedDays == days,
                                        onClick = { viewModel.selectTimeframe(days) },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        // Growth Hero & Trend Chart Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "Storage Trend (Last $selectedDays Days)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        val latestUsed = snapshots.lastOrNull()?.totalUsedBytes ?: 0L
                                        Text(
                                            text = FormattingUtils.formatBytes(latestUsed),
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (growth.hasSufficientHistory) {
                                            val isPositive = growth.netGrowthBytes >= 0
                                            val sign = if (isPositive) "+" else ""
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isPositive) WarningAmber.copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "$sign${FormattingUtils.formatBytes(growth.netGrowthBytes)} (${sign}${String.format("%.1f", growth.netGrowthPercentage)}%)",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPositive) WarningAmber else EmeraldPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Custom Canvas Trend Sparkline
                                    if (snapshots.size >= 2) {
                                        StorageTrendChart(
                                            snapshots = snapshots,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "StorageIQ needs more snapshots to plot a trend line.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // What's Growing? Card Section
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "What's Growing?",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (!growth.hasSufficientHistory) {
                                        Text(
                                            text = growth.insights.firstOrNull() ?: "Not enough history yet to rank growth.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        growth.insights.forEach { insight ->
                                            Text(
                                                text = insight,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Contributor Ranking
                                        growth.rankedCategoryGrowths.filter { it.growthBytes != 0L }.forEach { contributor ->
                                            ContributorRow(contributor)
                                        }
                                    }
                                }
                            }
                        }

                        // Growth Rate Projection Card
                        if (growth.projection.hasSufficientHistory && growth.projection.growthBytesPerDay > 0) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = WarningAmber,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Observed Growth Rate",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Adding approx. ${FormattingUtils.formatBytes(growth.projection.growthBytesPerDay)}/day (${FormattingUtils.formatBytes(growth.projection.growthBytesPerWeek)}/week).",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Historical Snapshot Log Header
                        item {
                            Text(
                                text = "Recorded Snapshots",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (snapshots.isEmpty()) {
                            item {
                                Text(
                                    text = "No snapshots recorded yet. Run a storage scan to create your first snapshot.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(snapshots.reversed()) { snap ->
                                SnapshotLogItem(snap)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContributorRow(contributor: CategoryGrowth) {
    val isPositive = contributor.growthBytes > 0
    val sign = if (isPositive) "+" else ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = contributor.categoryName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$sign${FormattingUtils.formatBytes(contributor.growthBytes)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isPositive) WarningAmber else EmeraldPrimary
        )
    }
}

@Composable
private fun SnapshotLogItem(snap: StorageSnapshot) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = FormattingUtils.formatDate(snap.timestampMs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Photos: ${FormattingUtils.formatBytes(snap.photosBytes)} • Videos: ${FormattingUtils.formatBytes(snap.videosBytes)} • WhatsApp: ${FormattingUtils.formatBytes(snap.whatsappBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = FormattingUtils.formatBytes(snap.totalUsedBytes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = EmeraldPrimary
        )
    }
}

@Composable
private fun StorageTrendChart(
    snapshots: List<StorageSnapshot>,
    modifier: Modifier = Modifier
) {
    val lineColor = EmeraldPrimary
    val minBytes = snapshots.minOf { it.totalUsedBytes }
    val maxBytes = snapshots.maxOf { it.totalUsedBytes }.coerceAtLeast(minBytes + 1L)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 16f
        val usableWidth = width - (padding * 2)
        val usableHeight = height - (padding * 2)

        val points = snapshots.mapIndexed { index, snap ->
            val x = padding + (index.toFloat() / (snapshots.size - 1).coerceAtLeast(1)) * usableWidth
            val normalizedY = ((snap.totalUsedBytes - minBytes).toFloat() / (maxBytes - minBytes).toFloat())
            val y = height - padding - (normalizedY * usableHeight)
            Offset(x, y)
        }

        if (points.size >= 2) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    val cx = (p0.x + p1.x) / 2
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            // Fill gradient under line
            val fillPath = Path().apply {
                addPath(path)
                lineTo(points.last().x, height)
                lineTo(points.first().x, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                    startY = padding,
                    endY = height
                )
            )

            // Draw line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw data points
            points.forEach { pt ->
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
                drawCircle(color = lineColor, radius = 2.5.dp.toPx(), center = pt)
            }
        }
    }
}

