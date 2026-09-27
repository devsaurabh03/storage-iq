package com.storage.intelligence.domain.usecase

import com.storage.intelligence.domain.model.OtherStorageBreakdown
import com.storage.intelligence.domain.model.StorageReconciliation
import com.storage.intelligence.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOtherStorageUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    fun getOtherStorageBreakdown(): Flow<OtherStorageBreakdown> = storageRepository.getOtherStorageFlow()
    fun getReconciliation(): Flow<StorageReconciliation> = storageRepository.getReconciliationFlow()
}
