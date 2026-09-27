package com.storage.intelligence.feature.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.storage.intelligence.R
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.core.ui.components.DuplicateCompareDialog
import com.storage.intelligence.core.ui.components.FileCard
import com.storage.intelligence.core.ui.components.SafeDeleteDialog
import com.storage.intelligence.core.ui.theme.EmeraldPrimary
import com.storage.intelligence.core.ui.theme.WarningAmber
import com.storage.intelligence.domain.model.FileItem
import com.storage.intelligence.domain.model.MessagingMediaType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingMediaScreen(
    viewModel: MessagingMediaViewModel,
    onNavigateBack: () -> Unit
) {
    val mediaState by viewModel.mediaFiles.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val selectedApp by viewModel.selectedApp.collectAsState()
    val selectedSubcategory by viewModel.selectedSubcategory.collectAsState()
    val messagingReport by viewModel.messagingReport.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val permissionManager = remember(context) { com.storage.intelligence.core.permissions.PermissionManager(context.applicationContext) }
    var hadAllFilesAccess by remember { mutableStateOf(permissionManager.hasAllFilesAccess()) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val hasAccess = permissionManager.hasAllFilesAccess()
                if (hasAccess && (!hadAllFilesAccess || (mediaState as? Result.Success)?.data.isNullOrEmpty())) {
                    viewModel.rescanStorage()
                }
                hadAllFilesAccess = hasAccess
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val currentAppStorage = messagingReport.apps.firstOrNull { it.appName == selectedApp }

    var verifyTargetFile by remember { mutableStateOf<FileItem?>(null) }
    var duplicateCopiesForTarget by remember { mutableStateOf<List<FileItem>>(emptyList()) }

    if (verifyTargetFile != null) {
        DuplicateCompareDialog(
            targetFile = verifyTargetFile!!,
            duplicateCopies = duplicateCopiesForTarget,
            onDismiss = { verifyTargetFile = null },
            onConfirmDeleteCopies = { copiesToDelete ->
                copiesToDelete.forEach { copy ->
                    viewModel.toggleSelection(copy.id, true)
                }
                showDeleteDialog = true
                verifyTargetFile = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Messaging Storage & Duplicates", fontWeight = FontWeight.Bold) },
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
                    val files = (mediaState as? Result.Success)?.data ?: emptyList()
                    val selectedItems = files.filter { selectedFiles.contains(it.id) }
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
                                text = "${selectedItems.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = FormattingUtils.formatBytes(totalBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldPrimary
                            )
                        }

                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Delete")
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
            if (isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = EmeraldPrimary
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = EmeraldPrimary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scanning $selectedApp & media directories...",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }

            // UPPER WINDOW HERO CARD: TOTAL DUPLICATE CONTENT OCCUPIED SPACE
            val allFiles = (mediaState as? Result.Success)?.data ?: emptyList()
            val duplicateFiles = allFiles.filter { it.isDuplicate }
            val totalDuplicateOccupiedBytes = duplicateFiles.sumOf { it.sizeBytes }

            if (duplicateFiles.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                text = "DUPLICATE CONTENT OCCUPIED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                            Text(
                                text = FormattingUtils.formatBytes(totalDuplicateOccupiedBytes),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${duplicateFiles.size} duplicate copies found in $selectedApp media",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                duplicateFiles.forEach { viewModel.toggleSelection(it.id, true) }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Select All", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // WhatsApp vs Telegram Tabs
            TabRow(
                selectedTabIndex = if (selectedApp == "WhatsApp") 0 else 1,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = selectedApp == "WhatsApp",
                    onClick = { viewModel.setSelectedApp("WhatsApp") },
                    text = { Text(text = stringResource(R.string.category_whatsapp)) }
                )
                Tab(
                    selected = selectedApp == "Telegram",
                    onClick = { viewModel.setSelectedApp("Telegram") },
                    text = { Text(text = stringResource(R.string.category_telegram)) }
                )
            }

            // Subcategory Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSubcategory == null,
                        onClick = { viewModel.setSelectedSubcategory(null) },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                items(listOf(MessagingMediaType.VIDEOS, MessagingMediaType.IMAGES, MessagingMediaType.DOCUMENTS, MessagingMediaType.AUDIO)) { type ->
                    val sub = currentAppStorage?.subcategories?.firstOrNull { it.type == type }
                    val sizeLabel = if (sub != null && sub.totalSizeBytes > 0) " (${FormattingUtils.formatBytes(sub.totalSizeBytes)})" else ""
                    FilterChip(
                        selected = selectedSubcategory == type,
                        onClick = { viewModel.setSelectedSubcategory(type) },
                        label = { Text("${type.displayName}$sizeLabel") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val state = mediaState) {
                    is Result.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is Result.Error -> Text(
                        text = "Error loading messaging media",
                        modifier = Modifier.align(Alignment.Center)
                    )
                    is Result.Success -> {
                        val rawFiles = state.data
                        // SORTING RULE: Duplicate content at the VERY TOP of the list!
                        val sortedFiles = remember(rawFiles) {
                            rawFiles.sortedWith(
                                compareByDescending<FileItem> { it.isDuplicate }
                                    .thenByDescending { it.sizeBytes }
                            )
                        }

                        if (sortedFiles.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No accessible $selectedApp media found.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "WhatsApp & Telegram media require explicit directory permission on Android to scan all media files.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val pm = remember(context) { com.storage.intelligence.core.permissions.PermissionManager(context.applicationContext) }
                                Button(
                                    onClick = { pm.requestAllFilesAccess(context) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Icon(imageVector = Icons.Default.Info, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Grant Full Storage Access Permission")
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sortedFiles) { fileItem ->
                                    val isChecked = selectedFiles.contains(fileItem.id)
                                    FileCard(
                                        fileItem = fileItem.copy(isSelectedForDeletion = isChecked),
                                        onSelectionChanged = { checked ->
                                            viewModel.toggleSelection(fileItem.id, checked)
                                        },
                                        onVerifyClick = if (fileItem.isDuplicate) {
                                            {
                                                viewModel.getDuplicateCopiesForFile(fileItem) { copies ->
                                                    duplicateCopiesForTarget = copies
                                                    verifyTargetFile = fileItem
                                                }
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        val files = (mediaState as? Result.Success)?.data ?: emptyList()
        val selectedItems = files.filter { selectedFiles.contains(it.id) }
        SafeDeleteDialog(
            selectedCount = selectedItems.size,
            totalSizeBytes = selectedItems.sumOf { it.sizeBytes },
            onConfirmDelete = {
                showDeleteDialog = false
                viewModel.deleteSelected(files)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
