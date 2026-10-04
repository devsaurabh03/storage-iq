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
import androidx.compose.ui.draw.clip
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
import com.storage.intelligence.core.ui.components.WorldFactsLoadingView
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SyncAlt
import coil.compose.AsyncImage
import com.storage.intelligence.core.ui.theme.EmeraldPrimary
import com.storage.intelligence.core.ui.theme.WarningAmber
import com.storage.intelligence.domain.model.CorrelationType
import com.storage.intelligence.domain.model.SentReceivedCorrelationGroup
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
    val (sentReceivedSummary, sentReceivedGroups) = viewModel.sentReceivedState.collectAsState().value
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

            if (viewModel.isPartialPermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Partial media access granted. Some WhatsApp media may be missing from this analysis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // UPPER WINDOW HERO CARD: SENT & RECEIVED INTELLIGENCE OR DUPLICATE SPACE
            val allFiles = (mediaState as? Result.Success)?.data ?: emptyList()
            val duplicateFiles = allFiles.filter { it.isDuplicate || it.isForwarded }
            val totalDuplicateOccupiedBytes = duplicateFiles.sumOf { it.sizeBytes }

            if (selectedApp == "WhatsApp" && sentReceivedSummary.totalCorrelatedPairs > 0) {
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
                                .background(EmeraldPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SENT & RECEIVED INTELLIGENCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = FormattingUtils.formatBytes(sentReceivedSummary.reclaimableBytes),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${sentReceivedSummary.totalCorrelatedPairs} media pairs (${sentReceivedSummary.exactMatchCount} exact · ${sentReceivedSummary.similarMatchCount} similar)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Button(
                                onClick = {
                                    viewModel.selectAllSentCopies(sentReceivedGroups)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Select Sent", fontWeight = FontWeight.Bold)
                            }
                            if (selectedSubcategory != MessagingMediaType.SENT_AND_RECEIVED) {
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = { viewModel.setSelectedSubcategory(MessagingMediaType.SENT_AND_RECEIVED) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Review Pairs →", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                                }
                            }
                        }
                    }
                }
            } else if (duplicateFiles.isNotEmpty()) {
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
                                text = "DUPLICATE & CIRCULATED MEDIA",
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
                            val forwardedCount = duplicateFiles.count { it.isForwarded }
                            val exactDupCount = duplicateFiles.count { it.isDuplicate }
                            val manyTimesCount = duplicateFiles.count { it.isForwardedManyTimes }
                            val subtitleText = if (manyTimesCount > 0) {
                                "${duplicateFiles.size} copies ($manyTimesCount circulated, $forwardedCount sent)"
                            } else {
                                "${duplicateFiles.size} copies ($exactDupCount exact duplicates, $forwardedCount sent)"
                            }
                            Text(
                                text = subtitleText,
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
            val availableTypes = if (selectedApp == "WhatsApp") {
                listOf(
                    MessagingMediaType.SENT_AND_RECEIVED,
                    MessagingMediaType.EXACT_MATCHES,
                    MessagingMediaType.SIMILAR_MATCHES,
                    MessagingMediaType.GREETINGS,
                    MessagingMediaType.VIDEOS,
                    MessagingMediaType.IMAGES,
                    MessagingMediaType.DOCUMENTS,
                    MessagingMediaType.AUDIO
                )
            } else {
                listOf(
                    MessagingMediaType.EXACT_MATCHES,
                    MessagingMediaType.GREETINGS,
                    MessagingMediaType.VIDEOS,
                    MessagingMediaType.IMAGES,
                    MessagingMediaType.DOCUMENTS,
                    MessagingMediaType.AUDIO
                )
            }

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

                items(availableTypes) { type ->
                    val sub = currentAppStorage?.subcategories?.firstOrNull { it.type == type }
                    val sizeLabel = if (type == MessagingMediaType.SENT_AND_RECEIVED && sentReceivedSummary.reclaimableBytes > 0) {
                        " (${FormattingUtils.formatBytes(sentReceivedSummary.reclaimableBytes)})"
                    } else if (sub != null && sub.totalSizeBytes > 0) {
                        " (${FormattingUtils.formatBytes(sub.totalSizeBytes)})"
                    } else ""
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
                if (selectedSubcategory == MessagingMediaType.SENT_AND_RECEIVED) {
                    if (sentReceivedGroups.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Sent & Received Matches Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "None of your WhatsApp sent media matches files in your received collections.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Found in Sent & Received",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Select redundant sent copies to safely reclaim space while preserving received originals.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { viewModel.selectAllSentCopies(sentReceivedGroups) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                        ) {
                                            Text("Select Sent", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }

                            items(sentReceivedGroups) { group ->
                                val isChecked = selectedFiles.contains(group.sentFile.id)
                                SentReceivedCorrelationCard(
                                    group = group,
                                    isSentSelected = isChecked,
                                    onSentSelectionChanged = { checked ->
                                        viewModel.toggleSelection(group.sentFile.id, checked)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    when (val state = mediaState) {
                        is Result.Loading -> {
                            WorldFactsLoadingView(
                                statusTitle = "Analyzing Chat Media…",
                                statusSubtitle = "Scanning WhatsApp & Telegram photos, videos, and audio",
                                accentColor = EmeraldPrimary
                            )
                        }
                        is Result.Error -> Text(
                            text = "Error loading messaging media",
                            modifier = Modifier.align(Alignment.Center)
                        )
                        is Result.Success -> {
                            val rawFiles = state.data
                            val sortedFiles = remember(rawFiles) {
                                rawFiles.sortedWith(
                                    compareByDescending<FileItem> { it.isDuplicate || it.isForwarded }
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

@Composable
fun SentReceivedCorrelationCard(
    group: SentReceivedCorrelationGroup,
    isSentSelected: Boolean,
    onSentSelectionChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Correlation Badge & Reclaimable Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (badgeBg, badgeTextColor) = when (group.correlationType) {
                    CorrelationType.STRONG_TEMPORAL -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
                    CorrelationType.EXACT_MATCH -> Pair(Color(0xFFE0F2FE), Color(0xFF0369A1))
                    CorrelationType.VISUAL_SIMILARITY -> Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = group.correlationType.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }

                Text(
                    text = "${FormattingUtils.formatBytes(group.reclaimableBytes)} reclaimable",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = group.correlationType.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Two-column comparison: Received Copy (Keep) vs Sent Copy (Reclaim)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Received Copy (Keep)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = group.receivedFile.path,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Received Copy",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = group.receivedFile.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = FormattingUtils.formatBytes(group.receivedFile.sizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sent Copy (Reclaim)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSentSelectionChanged(!isSentSelected) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSentSelected) WarningAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (isSentSelected) androidx.compose.foundation.BorderStroke(1.5.dp, WarningAmber) else null
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = group.sentFile.path,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sent Copy",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = group.sentFile.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = FormattingUtils.formatBytes(group.sentFile.sizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = isSentSelected,
                            onCheckedChange = onSentSelectionChanged,
                            colors = CheckboxDefaults.colors(checkedColor = WarningAmber)
                        )
                    }
                }
            }
        }
    }
}
