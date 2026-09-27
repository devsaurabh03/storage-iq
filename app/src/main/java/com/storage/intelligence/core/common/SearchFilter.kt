package com.storage.intelligence.core.common

import com.storage.intelligence.domain.model.StorageCategory

enum class SizeThreshold(val minBytes: Long) {
    ALL(0L),
    OVER_100MB(100L * 1024 * 1024),
    OVER_500MB(500L * 1024 * 1024),
    OVER_1GB(1L * 1024 * 1024 * 1024),
    OVER_2GB(2L * 1024 * 1024 * 1024)
}

enum class AgeThreshold(val minDays: Int) {
    ALL(0),
    OLDER_THAN_90_DAYS(90),
    OLDER_THAN_1_YEAR(365)
}

data class SearchFilter(
    val query: String = "",
    val category: StorageCategory? = null,
    val sizeThreshold: SizeThreshold = SizeThreshold.ALL,
    val ageThreshold: AgeThreshold = AgeThreshold.ALL,
    val exactDuplicatesOnly: Boolean = false,
    val similarOnly: Boolean = false
)
