package com.storage.intelligence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.storage.intelligence.core.common.FormattingUtils
import com.storage.intelligence.domain.model.FileItem
import com.storage.intelligence.domain.model.StorageCategory
import java.io.File

@Composable
fun FileCard(
    fileItem: FileItem,
    onSelectionChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isPreservedOriginal: Boolean = false,
    onVerifyClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail or category icon
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val file = File(fileItem.path)
            if (file.exists() && (fileItem.category == StorageCategory.PHOTOS || fileItem.category == StorageCategory.VIDEOS || fileItem.mimeType.startsWith("image/") || fileItem.mimeType.startsWith("video/"))) {
                AsyncImage(
                    model = fileItem.path,
                    contentDescription = fileItem.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = getIconForCategory(fileItem.category),
                    contentDescription = fileItem.category.displayName,
                    tint = fileItem.category.color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = FormattingUtils.formatBytes(fileItem.sizeBytes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = " • ${FormattingUtils.formatDate(fileItem.modifiedTimeMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (fileItem.isDuplicate) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(androidx.compose.ui.graphics.Color(0xFFFFF3E0))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (fileItem.duplicateCopyCount > 1) "DUPLICATE (${fileItem.duplicateCopyCount} copies)" else "DUPLICATE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color(0xFFE65100)
                        )
                    }

                    if (onVerifyClick != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verify Copies →",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = com.storage.intelligence.core.ui.theme.EmeraldPrimary,
                            modifier = Modifier.clickable { onVerifyClick() }
                        )
                    }
                }
            } else if (isPreservedOriginal) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Original copy (Preserved)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        if (onSelectionChanged != null && !isPreservedOriginal) {
            Checkbox(
                checked = fileItem.isSelectedForDeletion,
                onCheckedChange = onSelectionChanged,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

private fun getIconForCategory(category: StorageCategory): ImageVector {
    return when (category) {
        StorageCategory.VIDEOS -> Icons.Default.Movie
        StorageCategory.PHOTOS -> Icons.Default.Image
        StorageCategory.DOWNLOADS -> Icons.Default.Folder
        else -> Icons.Default.Description
    }
}
