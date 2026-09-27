package com.storage.intelligence.feature.similar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.core.ui.components.EmptyStateCard
import com.storage.intelligence.core.ui.components.SafeDeleteDialog
import com.storage.intelligence.core.ui.components.SimilarityGroupCard
import com.storage.intelligence.core.ui.preview.FilePreviewController
import com.storage.intelligence.core.ui.preview.FilePreviewDialog
import com.storage.intelligence.core.ui.theme.EmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimilarPhotosScreen(
    viewModel: SimilarPhotosViewModel,
    onNavigateBack: () -> Unit
) {
    val groupsState by viewModel.similarGroups.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val previewController = remember { FilePreviewController() }
    val previewState by previewController.previewState.collectAsState()

    if (previewState.isVisible && previewState.fileItem != null) {
        FilePreviewDialog(
            fileItem = previewState.fileItem!!,
            previewType = previewState.previewType,
            onDismiss = { previewController.dismiss() },
            onOpenExternal = {
                previewController.openWithExternalApp(context, previewState.fileItem!!)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Visually Similar Photos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    val selectedItems = groups.flatMap { g -> g.photos.filter { selectedFiles.contains(it.id) } }
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
                                text = "${selectedItems.size} photos selected",
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
                            Text(text = "Delete ${selectedItems.size} photos")
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
                        Text("Detecting visually similar photos...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is Result.Error -> {
                    Text("Error loading similar photos", modifier = Modifier.align(Alignment.Center))
                }
                is Result.Success -> {
                    val groups = state.data

                    if (groups.isEmpty()) {
                        EmptyStateCard(
                            title = "No similar photos found",
                            description = "Your photo library has no repetitive bursts or duplicate scenes.",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(groups, key = { it.groupId }) { group ->
                                SimilarityGroupCard(
                                    group = group,
                                    selectedPhotoIds = selectedFiles,
                                    onToggleSelect = { photoId, isSelected ->
                                        viewModel.toggleSelection(photoId, isSelected)
                                    },
                                    onPreview = { photo ->
                                        previewController.previewFile(photo)
                                    }
                                )
                            }
                        }
                    }

                    if (showDeleteDialog) {
                        val selectedItems = groups.flatMap { g -> g.photos.filter { selectedFiles.contains(it.id) } }
                        SafeDeleteDialog(
                            selectedCount = selectedItems.size,
                            totalSizeBytes = selectedItems.sumOf { it.sizeBytes },
                            onConfirmDelete = {
                                showDeleteDialog = false
                                viewModel.deleteSelected(groups)
                            },
                            onDismiss = { showDeleteDialog = false }
                        )
                    }
                }
            }
        }
    }
}
