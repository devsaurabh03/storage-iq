package com.storage.intelligence.feature.optimization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.core.ui.components.EmptyStateCard
import com.storage.intelligence.core.ui.components.SafeDeleteDialog
import com.storage.intelligence.core.ui.components.SectionHeader
import com.storage.intelligence.core.ui.preview.FilePreviewController
import com.storage.intelligence.core.ui.preview.FilePreviewDialog
import com.storage.intelligence.core.ui.theme.EmeraldPrimary
import com.storage.intelligence.core.ui.theme.WarningAmber
import com.storage.intelligence.domain.model.FileItem
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaleOptimizationScreen(
    viewModel: StaleOptimizationViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToArchive: () -> Unit
) {
    val context = LocalContext.current
    val staleState by viewModel.staleFiles.collectAsState()
    val selectedTimeframeDays by viewModel.selectedTimeframeDays.collectAsState()
    val selectedFileIds by viewModel.selectedFileIds.collectAsState()
    val isCompressing by viewModel.isCompressing.collectAsState()
    val optimizedFiles by viewModel.optimizedFiles.collectAsState()

    var fileToDelete by remember { mutableStateOf<FileItem?>(null) }
    var optimizationCompleteSnackbar by remember { mutableStateOf<Long?>(null) }

    val previewController = remember { FilePreviewController() }
    val previewState by previewController.previewState.collectAsState()

    if (previewState.isVisible && previewState.fileItem != null) {
        FilePreviewDialog(
            fileItem = previewState.fileItem!!,
            previewType = previewState.previewType,
            onDismiss = { previewController.dismiss() },
            onOpenExternal = {
                previewController.openWithExternalApp(context, previewState.fileItem!!)
            },
            onActionKeep = null,
            onActionDelete = {
                fileToDelete = previewState.fileItem
            }
        )
    }

    if (fileToDelete != null) {
        SafeDeleteDialog(
            selectedCount = 1,
            totalSizeBytes = fileToDelete!!.sizeBytes,
            onConfirmDelete = {
                val target = fileToDelete!!
                fileToDelete = null
                viewModel.deleteFile(target) {}
            },
            onDismiss = { fileToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stale & Unopened Files", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (optimizedFiles.isNotEmpty()) {
                        IconButton(onClick = onNavigateToArchive) {
                            BadgedBox(
                                badge = { Badge { Text("${optimizedFiles.size}") } }
                            ) {
                                Icon(imageVector = Icons.Default.Archive, contentDescription = "Archive", tint = EmeraldPrimary)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (selectedFileIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp
                ) {
                    val files = (staleState as? Result.Success)?.data ?: emptyList()
                    val selectedItems = files.filter { selectedFileIds.contains(it.id) }
                    val totalOriginalBytes = selectedItems.sumOf { it.sizeBytes }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedItems.size} files selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = FormattingUtils.formatBytes(totalOriginalBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.optimizeSelected(context, files) { saved ->
                                    optimizationCompleteSnackbar = saved
                                }
                            },
                            enabled = !isCompressing,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isCompressing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Optimizing...", color = Color.Black)
                            } else {
                                Icon(imageVector = Icons.Default.Compress, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Optimize", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = staleState) {
                is Result.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = EmeraldPrimary)
                }
                is Result.Error -> {
                    Text("Error loading stale files", modifier = Modifier.align(Alignment.Center))
                }
                is Result.Success -> {
                    val files = state.data
                    val totalBytes = files.sumOf { it.sizeBytes }

                    if (files.isEmpty()) {
                        EmptyStateCard(
                            title = "No stale files found",
                            description = "No unopened files older than $selectedTimeframeDays days were found.",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        )
                    } else {
                        // Data-driven category breakdown: only include categories with > 0 items!
                        val mediaFiles = files.filter { it.category.name == "VIDEOS" || it.category.name == "PHOTOS" || it.category.name == "AUDIO" }
                        val documentFiles = files.filter { it.category.name == "DOCUMENTS" }
                        val downloadFiles = files.filter { it.isDownload }
                        val otherFiles = files.filter {
                            !mediaFiles.contains(it) && !documentFiles.contains(it) && !downloadFiles.contains(it)
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Timeframe Selection Bar & Hero Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Stale Files",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${files.size} files • ${FormattingUtils.formatBytes(totalBytes)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = WarningAmber,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            TextButton(
                                                onClick = {
                                                    if (selectedFileIds.size == files.size) {
                                                        viewModel.selectAll(emptyList())
                                                    } else {
                                                        viewModel.selectAll(files)
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = if (selectedFileIds.size == files.size) "Deselect" else "Select All",
                                                    color = EmeraldPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Timeframe Filter Chips
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(30 to "30d+", 90 to "90d+", 180 to "6mo+", 365 to "1yr+").forEach { (days, label) ->
                                                FilterChip(
                                                    selected = selectedTimeframeDays == days,
                                                    onClick = { viewModel.selectTimeframe(days) },
                                                    label = { Text(label, fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Media Section (if > 0 items)
                            if (mediaFiles.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Media",
                                        badgeText = "${mediaFiles.size} • ${FormattingUtils.formatBytes(mediaFiles.sumOf { it.sizeBytes })}"
                                    )
                                }
                                items(mediaFiles, key = { it.id }) { item ->
                                    StaleItemCard(
                                        fileItem = item,
                                        isSelected = selectedFileIds.contains(item.id),
                                        onToggleSelect = { viewModel.toggleFileSelection(item.id, it) },
                                        onPreview = { previewController.previewFile(item) },
                                        onOpenWith = { previewController.openWithExternalApp(context, item) },
                                        onDelete = { fileToDelete = item }
                                    )
                                }
                            }

                            // 3. Documents Section (if > 0 items)
                            if (documentFiles.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Documents",
                                        badgeText = "${documentFiles.size} • ${FormattingUtils.formatBytes(documentFiles.sumOf { it.sizeBytes })}"
                                    )
                                }
                                items(documentFiles, key = { it.id }) { item ->
                                    StaleItemCard(
                                        fileItem = item,
                                        isSelected = selectedFileIds.contains(item.id),
                                        onToggleSelect = { viewModel.toggleFileSelection(item.id, it) },
                                        onPreview = { previewController.previewFile(item) },
                                        onOpenWith = { previewController.openWithExternalApp(context, item) },
                                        onDelete = { fileToDelete = item }
                                    )
                                }
                            }

                            // 4. Downloads Section (if > 0 items)
                            if (downloadFiles.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Downloads",
                                        badgeText = "${downloadFiles.size} • ${FormattingUtils.formatBytes(downloadFiles.sumOf { it.sizeBytes })}"
                                    )
                                }
                                items(downloadFiles, key = { it.id }) { item ->
                                    StaleItemCard(
                                        fileItem = item,
                                        isSelected = selectedFileIds.contains(item.id),
                                        onToggleSelect = { viewModel.toggleFileSelection(item.id, it) },
                                        onPreview = { previewController.previewFile(item) },
                                        onOpenWith = { previewController.openWithExternalApp(context, item) },
                                        onDelete = { fileToDelete = item }
                                    )
                                }
                            }

                            // 5. Other Files Section (if > 0 items)
                            if (otherFiles.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Other Files",
                                        badgeText = "${otherFiles.size} • ${FormattingUtils.formatBytes(otherFiles.sumOf { it.sizeBytes })}"
                                    )
                                }
                                items(otherFiles, key = { it.id }) { item ->
                                    StaleItemCard(
                                        fileItem = item,
                                        isSelected = selectedFileIds.contains(item.id),
                                        onToggleSelect = { viewModel.toggleFileSelection(item.id, it) },
                                        onPreview = { previewController.previewFile(item) },
                                        onOpenWith = { previewController.openWithExternalApp(context, item) },
                                        onDelete = { fileToDelete = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            optimizationCompleteSnackbar?.let { savedBytes ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = onNavigateToArchive) {
                            Text("View Archive", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    Text("Optimization complete! Saved ${FormattingUtils.formatBytes(savedBytes)}.")
                }
            }
        }
    }
}

@Composable
private fun StaleItemCard(
    fileItem: FileItem,
    isSelected: Boolean,
    onToggleSelect: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onOpenWith: () -> Unit,
    onDelete: () -> Unit
) {
    val daysInactive = remember(fileItem.modifiedTimeMs) {
        val diff = System.currentTimeMillis() - fileItem.modifiedTimeMs
        TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(1)
    }

    val relativeAge = when {
        daysInactive >= 365 -> "${daysInactive / 365} year(s) ago"
        daysInactive >= 30 -> "${daysInactive / 30} month(s) ago"
        else -> "$daysInactive day(s) ago"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail / Icon with tap-to-preview
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray)
                        .clickable { onPreview() },
                    contentAlignment = Alignment.Center
                ) {
                    if (fileItem.category.name == "PHOTOS" || fileItem.category.name == "VIDEOS") {
                        AsyncImage(
                            model = File(fileItem.path),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileItem.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${FormattingUtils.formatBytes(fileItem.sizeBytes)} • Last modified: $relativeAge",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onToggleSelect
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: [ Open ] [ Preview ] [ Delete ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenWith,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open", fontSize = 12.sp)
                }

                Button(
                    onClick = onPreview,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Preview", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
