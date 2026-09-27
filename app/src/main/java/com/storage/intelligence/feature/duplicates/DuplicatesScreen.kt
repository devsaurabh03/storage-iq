package com.storage.intelligence.feature.duplicates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.core.ui.components.DuplicateGroupCard
import com.storage.intelligence.core.ui.components.EmptyStateCard
import com.storage.intelligence.core.ui.components.SafeDeleteDialog
import com.storage.intelligence.core.ui.preview.FilePreviewController
import com.storage.intelligence.core.ui.preview.FilePreviewDialog
import com.storage.intelligence.core.ui.theme.EmeraldPrimary
import com.storage.intelligence.core.ui.theme.WarningAmber
import com.storage.intelligence.domain.model.FileItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    viewModel: DuplicatesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMessaging: (() -> Unit)? = null
) {
    val groupsState by viewModel.duplicateGroups.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    val previewController = remember { FilePreviewController() }
    val previewState by previewController.previewState.collectAsState()
    val context = LocalContext.current

    if (previewState.isVisible && previewState.fileItem != null) {
        FilePreviewDialog(
            fileItem = previewState.fileItem!!,
            previewType = previewState.previewType,
            onDismiss = { previewController.dismiss() },
            onOpenExternal = {
                previewController.openWithExternalApp(context, previewState.fileItem!!)
            },
            onActionKeep = previewState.onActionKeep,
            onActionDelete = previewState.onActionDelete
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Duplicate Files", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.rescanStorage() }, enabled = !isScanning) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rescan")
                    }
                }
            )
        },
        bottomBar = {
            if (selectedFiles.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp
                ) {
                    val groups = (groupsState as? Result.Success)?.data ?: emptyList()
                    val selectedItems = groups.flatMap { g -> g.files.filter { selectedFiles.contains(it.id) } }
                    val totalBytes = selectedItems.sumOf { it.sizeBytes }

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
                                text = FormattingUtils.formatBytes(totalBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Delete ${selectedItems.size} files")
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
            when (val state = groupsState) {
                is Result.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Finding duplicate files...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is Result.Error -> {
                    Text("Error loading duplicates", modifier = Modifier.align(Alignment.Center))
                }
                is Result.Success -> {
                    val groups = state.data
                    val totalRecoverableBytes = groups.sumOf { it.recoverableSizeBytes }
                    val selectedItems = groups.flatMap { g -> g.files.filter { selectedFiles.contains(it.id) } }
                    val isAllDuplicatesSelected = groups.isNotEmpty() && selectedFiles.size >= groups.sumOf { it.files.size - 1 }

                    if (groups.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            EmptyStateCard(
                                title = "No duplicate files found",
                                description = "Your storage has no identical duplicate copies across device folders."
                            )
                            if (onNavigateToMessaging != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onNavigateToMessaging,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Check WhatsApp & Telegram Duplicates")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (onNavigateToMessaging != null) {
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable(onClick = onNavigateToMessaging),
                                        color = EmeraldPrimary.copy(alpha = 0.12f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Social Media & Messaging Duplicates", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                                Text("Find duplicate photos & videos inside WhatsApp and Telegram", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text("Review →", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                        }
                                    }
                                }
                            }

                            // 1. SELECT ALL / SUMMARY BANNER CARD
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(WarningAmber.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                tint = WarningAmber,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${groups.size} groups",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (selectedFiles.isEmpty()) {
                                                    "${FormattingUtils.formatBytes(totalRecoverableBytes)} recoverable"
                                                } else {
                                                    "${selectedItems.size} files selected • ${FormattingUtils.formatBytes(selectedItems.sumOf { it.sizeBytes })}"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (selectedFiles.isEmpty()) WarningAmber else EmeraldPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (isAllDuplicatesSelected) {
                                                    viewModel.deselectAllDuplicates()
                                                } else {
                                                    viewModel.selectAllDuplicates(groups, keepNewest = true)
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isAllDuplicatesSelected) MaterialTheme.colorScheme.surfaceVariant else EmeraldPrimary,
                                                contentColor = if (isAllDuplicatesSelected) MaterialTheme.colorScheme.onSurface else Color.Black
                                            )
                                        ) {
                                            Text(
                                                text = if (isAllDuplicatesSelected) "Deselect" else "Select All",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. DUPLICATE GROUP CARDS
                            items(groups, key = { it.groupId }) { group ->
                                DuplicateGroupCard(
                                    group = group,
                                    selectedFileIds = selectedFiles,
                                    onToggleSelectFile = { fileId, isSelected ->
                                        viewModel.toggleFileSelection(fileId, isSelected)
                                    },
                                    onKeepFile = { fileId ->
                                        viewModel.setGroupPreservedFile(group.groupId, fileId)
                                    },
                                    onSelectAllExceptNewest = {
                                        viewModel.selectGroupAllExceptNewest(group)
                                    },
                                    onSelectAllExceptOldest = {
                                        viewModel.selectGroupAllExceptOldest(group)
                                    },
                                    onPreviewFile = { fileItem ->
                                        previewController.previewFile(
                                            fileItem = fileItem,
                                            onActionKeep = {
                                                viewModel.setGroupPreservedFile(group.groupId, fileItem.id)
                                            },
                                            onActionDelete = {
                                                viewModel.toggleFileSelection(fileItem.id, true)
                                                showDeleteDialog = true
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (showDeleteDialog) {
                        SafeDeleteDialog(
                            selectedCount = selectedItems.size,
                            totalSizeBytes = selectedItems.sumOf { it.sizeBytes },
                            onConfirmDelete = {
                                showDeleteDialog = false
                                viewModel.deleteSelected(groups) {}
                            },
                            onDismiss = { showDeleteDialog = false }
                        )
                    }
                }
            }
        }
    }
}
