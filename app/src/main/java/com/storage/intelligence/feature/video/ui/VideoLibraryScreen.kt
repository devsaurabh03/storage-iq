package com.storage.intelligence.feature.video.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.storage.intelligence.core.ui.components.VideoItemCard
import com.storage.intelligence.core.ui.theme.EmeraldPrimary
import com.storage.intelligence.feature.video.engine.VideoCharacteristics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections

enum class VideoSortFilter(val displayName: String) {
    RECOMMENDED("Recommended"),
    LARGEST("Largest"),
    RECENT("Recent")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLibraryScreen(
    videos: List<VideoCharacteristics>,
    onVideoClick: (VideoCharacteristics) -> Unit,
    onPlayClick: ((VideoCharacteristics) -> Unit)? = null,
    onOptimizeClick: ((VideoCharacteristics) -> Unit)? = null,
    onBatchOptimizeClick: ((List<VideoCharacteristics>) -> Unit)? = null,
    onNavigateToPhotoOptimization: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(VideoSortFilter.RECOMMENDED) }
    var selectedVideoPaths by remember { mutableStateOf(setOf<String>()) }
    val listState = rememberLazyListState()

    // Scroll to top when active filter changes
    LaunchedEffect(activeFilter) {
        try {
            listState.scrollToItem(0)
        } catch (_: Exception) {}
    }

    val sortedVideos = remember(videos, activeFilter) {
        try {
            val distinctVideos = videos.distinctBy { it.file.absolutePath }
            when (activeFilter) {
                VideoSortFilter.LARGEST -> distinctVideos.sortedByDescending { it.sizeBytes }
                VideoSortFilter.RECENT -> distinctVideos.sortedByDescending { video ->
                    if (video.modifiedTimeMs > 0L) {
                        video.modifiedTimeMs
                    } else {
                        try {
                            video.file.lastModified()
                        } catch (_: Exception) {
                            0L
                        }
                    }
                }
                VideoSortFilter.RECOMMENDED -> distinctVideos.sortedWith(
                    compareByDescending<VideoCharacteristics> { it.width >= 3840 }
                        .thenByDescending { it.sizeBytes }
                )
            }
        } catch (e: Exception) {
            com.storage.intelligence.core.logging.AppLogger.e("VideoLibraryScreen", "Sort failed: ${e.message}", e)
            videos.distinctBy { it.file.absolutePath }
        }
    }

    val allSelected = sortedVideos.isNotEmpty() && selectedVideoPaths.size == sortedVideos.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedVideoPaths.isNotEmpty()) "${selectedVideoPaths.size} Selected" else "Video Library (${videos.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (sortedVideos.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                selectedVideoPaths = if (allSelected) {
                                    emptySet()
                                } else {
                                    sortedVideos.map { it.file.absolutePath }.toSet()
                                }
                            }
                        ) {
                            Text(
                                text = if (allSelected) "Deselect" else "Select All",
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (onNavigateToPhotoOptimization != null) {
                        IconButton(onClick = onNavigateToPhotoOptimization) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = "Photo Optimizer",
                                tint = EmeraldPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (selectedVideoPaths.isNotEmpty()) {
                val selectedVideos = remember(selectedVideoPaths, sortedVideos) {
                    sortedVideos.filter { selectedVideoPaths.contains(it.file.absolutePath) }
                }
                val totalEstSavings = remember(selectedVideos) {
                    selectedVideos.sumOf { v ->
                        val isModern = v.codecName.equals("AV1", ignoreCase = true) ||
                                       v.codecName.equals("HEVC", ignoreCase = true) ||
                                       v.codecName.equals("H.265", ignoreCase = true)
                        when {
                            v.width >= 3840 -> (v.sizeBytes * 0.60).toLong()
                            !isModern -> (v.sizeBytes * 0.45).toLong()
                            else -> (v.sizeBytes * 0.20).toLong()
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedVideos.size} videos selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Est. savings: ~${com.storage.intelligence.core.common.FormattingUtils.formatBytes(totalEstSavings)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                onBatchOptimizeClick?.invoke(selectedVideos)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Optimize (${selectedVideos.size})", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Sort Filter Chips & Photo shortcut
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(VideoSortFilter.RECOMMENDED, VideoSortFilter.LARGEST, VideoSortFilter.RECENT).forEach { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = {
                                if (activeFilter != filter) {
                                    activeFilter = filter
                                }
                            },
                            label = { Text(filter.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (onNavigateToPhotoOptimization != null) {
                    TextButton(
                        onClick = onNavigateToPhotoOptimization,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Collections, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Photos", color = EmeraldPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (sortedVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No videos found in storage library.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = sortedVideos,
                        key = { it.file.absolutePath }
                    ) { video ->
                        val isSelected = selectedVideoPaths.contains(video.file.absolutePath)
                        val isModern = video.codecName.equals("AV1", ignoreCase = true) ||
                                       video.codecName.equals("HEVC", ignoreCase = true) ||
                                       video.codecName.equals("H.265", ignoreCase = true)
                        val estSavings = when {
                            video.width >= 3840 -> (video.sizeBytes * 0.60).toLong()
                            !isModern -> (video.sizeBytes * 0.45).toLong()
                            else -> 0L
                        }
                        VideoItemCard(
                            video = video,
                            estimatedSavingsBytes = estSavings,
                            isSelected = isSelected,
                            onSelectionToggle = { checked ->
                                selectedVideoPaths = if (checked) {
                                    selectedVideoPaths + video.file.absolutePath
                                } else {
                                    selectedVideoPaths - video.file.absolutePath
                                }
                            },
                            onClick = {
                                if (selectedVideoPaths.isNotEmpty()) {
                                    selectedVideoPaths = if (isSelected) {
                                        selectedVideoPaths - video.file.absolutePath
                                    } else {
                                        selectedVideoPaths + video.file.absolutePath
                                    }
                                } else {
                                    onVideoClick(video)
                                }
                            },
                            onPlayClick = { (onPlayClick ?: onVideoClick)(video) },
                            onOptimizeClick = { (onOptimizeClick ?: onVideoClick)(video) }
                        )
                    }
                }
            }
        }
    }
}
