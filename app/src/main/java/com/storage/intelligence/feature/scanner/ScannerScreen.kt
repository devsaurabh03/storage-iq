package com.storage.intelligence.feature.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.storage.intelligence.R
import com.storage.intelligence.core.ui.components.WorldFactsLoadingView
import com.storage.intelligence.core.ui.theme.EmeraldPrimary

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onScanComplete: () -> Unit
) {
    val progress by viewModel.scanProgress.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startScan()
    }

    LaunchedEffect(progress.isComplete) {
        if (progress.isComplete) {
            onScanComplete()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val progressText = if (progress.totalCount > 0) {
                stringResource(R.string.scanner_progress_format, progress.scannedCount, progress.totalCount)
            } else {
                progress.currentPhase
            }

            WorldFactsLoadingView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                statusTitle = stringResource(R.string.scanner_title),
                statusSubtitle = progressText,
                accentColor = EmeraldPrimary
            )

            // Scanner phase checkmarks card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PhaseRow(title = stringResource(R.string.scanner_phase_photos), isDone = progress.scannedCount > 0)
                    PhaseRow(title = stringResource(R.string.scanner_phase_videos), isDone = progress.scannedCount > 100)
                    PhaseRow(title = stringResource(R.string.scanner_phase_downloads), isDone = progress.scannedCount > 200)
                    PhaseRow(title = stringResource(R.string.scanner_phase_duplicates), isDone = progress.isComplete)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.scanner_bg_notice),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PhaseRow(title: String, isDone: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (isDone) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
