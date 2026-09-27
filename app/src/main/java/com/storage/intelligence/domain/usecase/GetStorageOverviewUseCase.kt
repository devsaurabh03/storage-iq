package com.storage.intelligence.domain.usecase

import com.storage.intelligence.domain.model.StorageOverview
import com.storage.intelligence.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStorageOverviewUseCase @Inject constructor(
    private val repository: StorageRepository
) {
    operator fun invoke(): Flow<StorageOverview> = repository.getStorageOverviewFlow()
}
