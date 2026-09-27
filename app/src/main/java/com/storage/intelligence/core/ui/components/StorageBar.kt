package com.storage.intelligence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.storage.intelligence.domain.model.StorageCategoryBreakdown

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StorageBar(
    breakdowns: List<StorageCategoryBreakdown>,
    totalSizeBytes: Long,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val validBreakdowns = breakdowns.filter { it.totalSizeBytes > 0 }
            val sumBytes = validBreakdowns.sumOf { it.totalSizeBytes }
            if (sumBytes > 0) {
                validBreakdowns.forEach { b ->
                    val weight = (b.totalSizeBytes.toFloat() / sumBytes.toFloat()).coerceAtLeast(0.01f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(weight)
                            .background(b.category.color)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OptInFlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            breakdowns.take(6).forEach { b ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(b.category.color)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = b.category.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptInFlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}
