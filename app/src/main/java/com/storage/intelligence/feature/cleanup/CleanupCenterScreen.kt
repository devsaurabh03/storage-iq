package com.storage.intelligence.feature.cleanup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.core.ui.components.EmptyStateCard
import com.storage.intelligence.core.ui.components.RecoveryItemBreakdown
import com.storage.intelligence.core.ui.components.SectionHeader
import com.storage.intelligence.core.ui.components.StorageRecoveryHeroCard
import com.storage.intelligence.core.ui.theme.*
import com.storage.intelligence.domain.model.StorageCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupCenterScreen(
    viewModel: CleanupViewModel,
    onNavigateToDuplicates: () -> Unit,
    onNavigateToSimilar: () -> Unit,
    onNavigateToLargeFiles: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToScreenshots: () -> Unit,
    onNavigateToMessaging: () -> Unit,
    onNavigateToStaleOptimize: () -> Unit = {},
    onNavigateToVideoOptimization: () -> Unit = {},
    onNavigateToPhotoOptimization: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showOtherStorageExplanation by remember { mutableStateOf(false) }

    if (showOtherStorageExplanation) {
        AlertDialog(
            onDismissRequest = { showOtherStorageExplanation = false },
            title = { Text("What is 'Other Storage'?") },
            text = {
                Column {
                    Text("Android groups unclassified system and application files under 'Other'.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Temporary & cache files (~2.4 GB)")
                    Text("• App-created database indexes (~1.8 GB)")
                    Text("• App-specific binary assets (~2.9 GB)")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "We measure everything allowed by Android security APIs without guessing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showOtherStorageExplanation = false }) {
                    Text("Understood")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Storage & Optimization",
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
            when (val state = uiState) {
                is Result.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = EmeraldPrimary)
                is Result.Error -> Text("Error loading storage map", modifier = Modifier.align(Alignment.Center))
                is Result.Success -> {
                    val overview = state.data
                    val recoverable = overview.recoverableSummary
                    val totalRecoverableBytes = recoverable.duplicateVideosBytes +
                            recoverable.duplicatePhotosBytes +
                            recoverable.oldDownloadsBytes +
                            recoverable.screenshotsBytes

                    val breakdownItems = mutableListOf<RecoveryItemBreakdown>()
                    if (recoverable.duplicateVideosBytes + recoverable.duplicatePhotosBytes > 0) {
                        breakdownItems.add(
                            RecoveryItemBreakdown("Duplicate Media", recoverable.duplicateVideosBytes + recoverable.duplicatePhotosBytes, CategoryVideoColor)
                        )
                    }
                    if (recoverable.oldDownloadsBytes > 0) {
                        breakdownItems.add(
                            RecoveryItemBreakdown("Old Downloads", recoverable.oldDownloadsBytes, CategoryDownloadColor)
                        )
                    }
                    if (recoverable.screenshotsBytes > 0) {
                        breakdownItems.add(
                            RecoveryItemBreakdown("Screenshots", recoverable.screenshotsBytes, CategoryScreenshotColor)
                        )
                    }

                    val videosCat = overview.categoryBreakdowns.find { it.category == StorageCategory.VIDEOS }
                    val photosCat = overview.categoryBreakdowns.find { it.category == StorageCategory.PHOTOS }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. HERO RECOVERY CARD (Only shown if recoverable storage > 0)
                        if (totalRecoverableBytes > 0) {
                            StorageRecoveryHeroCard(
                                totalRecoverableBytes = totalRecoverableBytes,
                                items = breakdownItems,
                                onReviewClick = onNavigateToDuplicates
                            )
                        } else {
                            EmptyStateCard(
                                title = "Storage is well organized",
                                description = "No redundant duplicates or accumulated old downloads found."
                            )
                        }

                        // 2. DATA-DRIVEN RECOVERY RECOMMENDATIONS (Strictly rendered only if > 0 bytes)
                        val duplicateMediaBytes = recoverable.duplicateVideosBytes + recoverable.duplicatePhotosBytes
                        val hasMediaRecommendations = (videosCat != null && videosCat.totalSizeBytes > 0) ||
                                (photosCat != null && photosCat.totalSizeBytes > 0) ||
                                duplicateMediaBytes > 0 ||
                                recoverable.oldDownloadsBytes > 0 ||
                                recoverable.screenshotsBytes > 0

                        if (hasMediaRecommendations) {
                            SectionHeader(title = "Opportunities & Actions")

                            // Exact Duplicate Media
                            if (duplicateMediaBytes > 0) {
                                StorageRecommendationCard(
                                    title = "Exact Duplicate Media",
                                    subtitle = "Remove byte-matched redundant copies across device",
                                    sizeBytes = duplicateMediaBytes,
                                    icon = Icons.Default.ContentCopy,
                                    iconColor = CategoryVideoColor,
                                    onClick = onNavigateToDuplicates
                                )
                            }

                            // Smart Video Optimization (AV1 / HEVC)
                            if (videosCat != null && videosCat.totalSizeBytes > 0) {
                                StorageRecommendationCard(
                                    title = "Smart Video Optimization",
                                    subtitle = "Compress large videos with hardware AV1/HEVC (~60% space reduction)",
                                    sizeBytes = videosCat.totalSizeBytes,
                                    actionLabel = "Optimize videos →",
                                    icon = Icons.Default.Movie,
                                    iconColor = EmeraldPrimary,
                                    onClick = onNavigateToVideoOptimization
                                )
                            }

                            // Smart Photo Optimization (WebP)
                            if (photosCat != null && photosCat.totalSizeBytes > 0) {
                                StorageRecommendationCard(
                                    title = "Smart Photo Optimization",
                                    subtitle = "Convert raw and uncompressed photos to modern WebP",
                                    sizeBytes = photosCat.totalSizeBytes,
                                    actionLabel = "Optimize photos →",
                                    icon = Icons.Default.PhotoSizeSelectActual,
                                    iconColor = CategoryPhotoColor,
                                    onClick = onNavigateToPhotoOptimization
                                )
                            }

                            // Old Downloads
                            if (recoverable.oldDownloadsBytes > 0) {
                                StorageRecommendationCard(
                                    title = "Old Downloads",
                                    subtitle = "Clean unused PDFs, APKs and downloads older than 90 days",
                                    sizeBytes = recoverable.oldDownloadsBytes,
                                    icon = Icons.Default.Download,
                                    iconColor = CategoryDownloadColor,
                                    onClick = onNavigateToDownloads
                                )
                            }

                            // Screenshots
                            if (recoverable.screenshotsBytes > 0) {
                                StorageRecommendationCard(
                                    title = "Screenshots & Captures",
                                    subtitle = "Review and prune accumulated screenshots",
                                    sizeBytes = recoverable.screenshotsBytes,
                                    icon = Icons.Default.Screenshot,
                                    iconColor = CategoryScreenshotColor,
                                    onClick = onNavigateToScreenshots
                                )
                            }

                            // Stale & Unopened Files
                            StorageRecommendationCard(
                                title = "Stale & Unopened Files",
                                subtitle = "Inspect old unopened files and documents",
                                sizeBytes = 0L,
                                actionLabel = "Inspect →",
                                icon = Icons.Default.Compress,
                                iconColor = EmeraldPrimary,
                                onClick = onNavigateToStaleOptimize
                            )

                            // Social Media & Messaging Duplicates (WhatsApp / Telegram)
                            val waCat = overview.categoryBreakdowns.find { it.category == StorageCategory.WHATSAPP }
                            val tgCat = overview.categoryBreakdowns.find { it.category == StorageCategory.TELEGRAM }
                            val messagingBytes = (waCat?.totalSizeBytes ?: 0L) + (tgCat?.totalSizeBytes ?: 0L)

                            StorageRecommendationCard(
                                title = "Social Media & Messaging Duplicates",
                                subtitle = "Detect forwarded duplicate media & cleanup WhatsApp & Telegram storage",
                                sizeBytes = messagingBytes,
                                actionLabel = "Review messaging duplicates →",
                                icon = Icons.Default.Chat,
                                iconColor = EmeraldPrimary,
                                onClick = onNavigateToMessaging
                            )

                            // Visually Similar Photos
                            StorageRecommendationCard(
                                title = "Visually Similar Photos",
                                subtitle = "Review burst shots and near-identical photos",
                                sizeBytes = 0L,
                                actionLabel = "Review clusters →",
                                icon = Icons.Default.Collections,
                                iconColor = CategoryPhotoColor,
                                onClick = onNavigateToSimilar
                            )
                        }

                        // 3. STORAGE EXPLANATION SECTION: WHY DOES 'OTHER' EXIST?
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showOtherStorageExplanation = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Why is 'Other' storage unclassified?",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Learn how Android system privacy partitions app cache vs user media.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageRecommendationCard(
    title: String,
    subtitle: String,
    sizeBytes: Long,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    actionLabel: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (sizeBytes > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${FormattingUtils.formatBytes(sizeBytes)} recoverable",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (actionLabel != null) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
