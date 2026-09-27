package com.storage.intelligence.domain.usecase

import com.storage.intelligence.domain.model.StorageGrowthReport
import com.storage.intelligence.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStorageGrowthUseCase @Inject constructor(
    private val timelineRepository: TimelineRepository
) {
    operator fun invoke(timeframeDays: Int = 30): Flow<StorageGrowthReport> {
        return timelineRepository.getGrowthReportFlow(timeframeDays)
    }

    suspend fun getEarliestTrackingTimestamp(): Long? {
        return timelineRepository.getEarliestTrackingTimestamp()
    }
}
