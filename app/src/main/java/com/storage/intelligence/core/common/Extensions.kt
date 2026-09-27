package com.storage.intelligence.core.common

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object FormattingUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceAtMost(units.size - 1)
        val df = DecimalFormat("#,##0.0")
        return "${df.format(bytes / 1024.0.pow(digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    fun formatDate(timestampMs: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    fun formatMonthYear(timestampMs: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    fun isOlderThanDays(timestampMs: Long, days: Int): Boolean {
        val diffMs = System.currentTimeMillis() - timestampMs
        val daysMs = days.toLong() * 24 * 60 * 60 * 1000
        return diffMs >= daysMs
    }
}
