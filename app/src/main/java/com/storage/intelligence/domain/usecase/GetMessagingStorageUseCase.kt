package com.storage.intelligence.domain.usecase

import com.storage.intelligence.domain.model.MessagingStorageReport
import com.storage.intelligence.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagingStorageUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    operator fun invoke(): Flow<MessagingStorageReport> = storageRepository.getMessagingReportFlow()
}
