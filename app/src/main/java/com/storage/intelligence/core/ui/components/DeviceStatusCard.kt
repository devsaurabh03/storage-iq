package com.storage.intelligence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storage.intelligence.core.ui.theme.EmeraldPrimary
import com.storage.intelligence.core.ui.theme.WarningAmber

enum class DeviceHealthLevel(val statusText: String, val subtitleText: String, val color: Color) {
    HEALTHY("Your device looks healthy", "Normal CPU load, stable temperatures, and plenty of storage.", EmeraldPrimary),
    NEEDS_ATTENTION("Your storage is nearly full", "Storage is over 80%. Freeing up space will maintain smooth performance.", WarningAmber),
    WARM("Your device is running warmer than usual", "Sustained foreground activity or charging has elevated device thermals.", WarningAmber),
    CRITICAL("Storage is critically full", "Over 90% space consumed. Performance and app updates may be affected.", Color(0xFFF43F5E))
}

@Composable
fun DeviceStatusCard(
    healthLevel: DeviceHealthLevel,
    storagePercent: Int,
    storageUsedText: String,
    performanceStatusText: String,
    thermalStatusText: String,
    modifier: Modifier = Modifier,
    onActionClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Overall Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(healthLevel.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (healthLevel == DeviceHealthLevel.HEALTHY) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = healthLevel.color,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = healthLevel.statusText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = healthLevel.subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Factual Status Matrix: Storage, Performance, Thermal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusMetricItem(
                    icon = Icons.Default.Storage,
                    label = "Storage",
                    value = "$storagePercent% used",
                    subvalue = storageUsedText,
                    valueColor = if (storagePercent > 85) WarningAmber else MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )

                StatusMetricItem(
                    icon = Icons.Default.Memory,
                    label = "Performance",
                    value = performanceStatusText,
                    subvalue = "CPU & RAM",
                    valueColor = EmeraldPrimary
                )

                HorizontalDivider(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )

                StatusMetricItem(
                    icon = Icons.Default.DeviceThermostat,
                    label = "Thermals",
                    value = thermalStatusText,
                    subvalue = "On-device",
                    valueColor = if (thermalStatusText.contains("Warm", ignoreCase = true) || thermalStatusText.contains("Hot", ignoreCase = true)) WarningAmber else EmeraldPrimary
                )
            }
        }
    }
}

@Composable
private fun StatusMetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    subvalue: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = subvalue,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}
