package com.storage.intelligence.feature.photo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.core.ui.components.WorldFactsLoadingView
import com.storage.intelligence.core.ui.theme.EmeraldPrimary
import com.storage.intelligence.feature.photo.model.PhotoOptimizationCandidate
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoOptimizationScreen(
    viewModel: PhotoOptimizationViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var resultSummaryText by remember { mutableStateOf<String?>(null) }

    val worthOptimizingList = uiState.candidates.filter { it.isWorthOptimizing }
    val allSelected = worthOptimizingList.isNotEmpty() && worthOptimizingList.all { it.isSelected }
    val selectedCount = uiState.candidates.count { it.isSelected }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedCount > 0) "$selectedCount Selected" else "Photo Optimization",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (worthOptimizingList.isNotEmpty()) {
                        TextButton(onClick = { viewModel.selectAll(!allSelected) }) {
                            Text(
                                if (allSelected) "Deselect" else "Select All (${worthOptimizingList.size})",
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedCount > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$selectedCount photos selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Est. savings: ${FormattingUtils.formatBytes(uiState.totalEstimatedSavingsBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { showConfirmDialog = true },
                            enabled = !uiState.isOptimizing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Optimize")
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
            var activeFilter by remember { mutableStateOf(0) } // 0: Needs Optimization, 1: Already Optimal, 2: All

            if (uiState.isEvaluating) {
                WorldFactsLoadingView(
                    statusTitle = "Analyzing Photo Compression Potential…",
                    statusSubtitle = "Evaluating megapixels, metadata, and visual fidelity",
                    accentColor = EmeraldPrimary
                )
            } else if (uiState.totalScannedCount == 0 || uiState.candidates.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Photos Found on Device",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No image files were detected on device storage. Ensure media/storage permissions are granted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadPhotoCandidates() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Re-scan Photos")
                    }
                }
            } else if (uiState.unoptimizedCount == 0 && activeFilter == 0) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "All ${uiState.totalScannedCount} Photos Already Optimal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Every photo scanned on this device is already compressed (WebP/HEIC) or compact (<250 KB). None require conversion.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { activeFilter = 2 },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View Scanned Photos (${uiState.totalScannedCount})")
                    }
                }
            } else {
                val displayedCandidates = remember(uiState.candidates, activeFilter) {
                    when (activeFilter) {
                        0 -> uiState.candidates.filter { it.isWorthOptimizing }
                        1 -> uiState.candidates.filter { !it.isWorthOptimizing }
                        else -> uiState.candidates
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Photo Optimization Overview",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Scanned ${uiState.totalScannedCount} device photos. Converts uncompressed PNGs and high-bitrate photos to modern WebP while preserving full visual resolution and EXIF metadata.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Unoptimized", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${uiState.unoptimizedCount} photos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Already Optimal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${uiState.alreadyOptimalCount} photos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Potential Savings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(FormattingUtils.formatBytes(uiState.totalEstimatedSavingsBytes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Filter Chips Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = activeFilter == 0,
                                onClick = { activeFilter = 0 },
                                label = { Text("To Optimize (${uiState.unoptimizedCount})") }
                            )
                            FilterChip(
                                selected = activeFilter == 1,
                                onClick = { activeFilter = 1 },
                                label = { Text("Optimal (${uiState.alreadyOptimalCount})") }
                            )
                            FilterChip(
                                selected = activeFilter == 2,
                                onClick = { activeFilter = 2 },
                                label = { Text("All (${uiState.totalScannedCount})") }
                            )
                        }
                    }

                    items(displayedCandidates) { candidate ->
                        PhotoCandidateRow(
                            candidate = candidate,
                            onToggleSelection = { isSelected ->
                                viewModel.toggleCandidateSelection(candidate.fileItem.id, isSelected)
                            }
                        )
                    }
                }
            }

            // Safe Media Replacement Confirmation Dialog
            if (showConfirmDialog) {
                var deleteOriginalsChoice by remember { mutableStateOf(true) }

                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Safe Photo Optimization")
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "StorageIQ will generate validated modern WebP photos, ensuring complete visual fidelity before committing.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "What would you like to do with the originals?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { deleteOriginalsChoice = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = deleteOriginalsChoice, onClick = { deleteOriginalsChoice = true })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Safely replace originals", fontWeight = FontWeight.SemiBold)
                                    Text("Reclaim maximum space (recommended)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { deleteOriginalsChoice = false }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = !deleteOriginalsChoice, onClick = { deleteOriginalsChoice = false })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Keep both original & optimized", fontWeight = FontWeight.SemiBold)
                                    Text("Safe side-by-side comparison", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmDialog = false
                                viewModel.optimizeSelected(deleteOriginalsChoice) { saved, count, errorMsg ->
                                    resultSummaryText = if (count > 0) {
                                        "$count photos optimized! ${FormattingUtils.formatBytes(saved)} recovered."
                                    } else {
                                        "Optimization couldn't convert photos: ${errorMsg ?: "No storage savings possible."}"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Start Optimization")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // In-progress modal
            if (uiState.isOptimizing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .align(Alignment.Center),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    WorldFactsLoadingView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        statusTitle = "Optimizing Photo ${uiState.currentOptimizingIndex} of ${uiState.totalToOptimize}…",
                        statusSubtitle = if (uiState.totalSavedSoFarBytes > 0) {
                            "Saved: ${FormattingUtils.formatBytes(uiState.totalSavedSoFarBytes)} • Validating image fidelity"
                        } else {
                            "Validating visual fidelity and re-encoding"
                        },
                        accentColor = EmeraldPrimary
                    )
                }
            }

            resultSummaryText?.let { summary ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { resultSummaryText = null }) {
                            Text("Dismiss", color = EmeraldPrimary)
                        }
                    }
                ) {
                    Text(summary)
                }
            }
        }
    }
}

@Composable
private fun PhotoCandidateRow(
    candidate: PhotoOptimizationCandidate,
    onToggleSelection: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val file = File(candidate.fileItem.path)
                if (file.exists()) {
                    AsyncImage(
                        model = candidate.fileItem.path,
                        contentDescription = candidate.fileItem.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = EmeraldPrimary)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.fileItem.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = FormattingUtils.formatBytes(candidate.originalSizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " → ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormattingUtils.formatBytes(candidate.estimatedOptimizedSizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " (-${candidate.estimatedSavingsPercent}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = candidate.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (candidate.isWorthOptimizing) {
                Checkbox(
                    checked = candidate.isSelected,
                    onCheckedChange = onToggleSelection,
                    colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Optimal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
