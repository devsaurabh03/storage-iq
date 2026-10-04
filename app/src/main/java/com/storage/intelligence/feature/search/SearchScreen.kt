package com.storage.intelligence.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.storage.intelligence.R
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.core.common.Result
import com.storage.intelligence.core.ui.components.FileCard
import com.storage.intelligence.core.ui.components.SafeDeleteDialog
import com.storage.intelligence.core.ui.components.WorldFactsLoadingView
import com.storage.intelligence.core.ui.theme.EmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit
) {
    val resultsState by viewModel.searchResults.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = filter.query,
                        onValueChange = { viewModel.updateQuery(it) },
                        placeholder = { Text(text = stringResource(R.string.search_placeholder)) },
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (filter.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateQuery("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
                    val files = (resultsState as? Result.Success)?.data ?: emptyList()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = resultsState) {
                is Result.Loading -> {
                    WorldFactsLoadingView(
                        statusTitle = "Searching Storage Index…",
                        statusSubtitle = "Scanning matching filenames, extensions, and paths",
                        accentColor = EmeraldPrimary
                    )
                }
                is Result.Error -> Text("Error searching files", modifier = Modifier.align(Alignment.Center))
                is Result.Success -> {
                    val files = state.data
                    if (files.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_no_results),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(files) { fileItem ->
                                val isChecked = selectedFiles.contains(fileItem.id)
                                FileCard(
                                    fileItem = fileItem.copy(isSelectedForDeletion = isChecked),
                                    onSelectionChanged = { checked ->
                                        viewModel.toggleSelection(fileItem.id, checked)
                                    }
                                )
                            }
                        }
                    }

                    if (showDeleteDialog) {
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
            }
        }
    }
}
