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
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(VideoSortFilter.RECOMMENDED) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Video Library (${videos.size} items)",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Sort Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = sortedVideos,
                        key = { it.file.absolutePath }
                    ) { video ->
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
                            onClick = { onVideoClick(video) },
                            onPlayClick = { (onPlayClick ?: onVideoClick)(video) },
                            onOptimizeClick = { (onOptimizeClick ?: onVideoClick)(video) }
                        )
                    }
                }
            }
        }
    }
}
