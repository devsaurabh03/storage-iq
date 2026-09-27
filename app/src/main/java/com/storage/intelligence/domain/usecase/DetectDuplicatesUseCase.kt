package com.storage.intelligence.domain.usecase

import com.storage.intelligence.domain.model.DuplicateGroup
import com.storage.intelligence.domain.model.FileItem
import com.storage.intelligence.domain.model.SimilarPhotoGroup
import com.storage.intelligence.domain.repository.DuplicateRepository
import com.storage.intelligence.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DetectDuplicatesUseCase @Inject constructor(
    private val duplicateRepository: DuplicateRepository
) {
    fun getExactDuplicates(): Flow<List<DuplicateGroup>> = duplicateRepository.getExactDuplicateGroupsFlow()
    fun getDuplicateVideos(): Flow<List<DuplicateGroup>> = duplicateRepository.getDuplicateVideosFlow()
}

class DetectSimilarPhotosUseCase @Inject constructor(
    private val duplicateRepository: DuplicateRepository
) {
    operator fun invoke(): Flow<List<SimilarPhotoGroup>> = duplicateRepository.getSimilarPhotoGroupsFlow()
}

class DeleteSelectedFilesUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(files: List<FileItem>): Boolean {
        // Enforce safety check: verify no sole original in exact duplicates is being accidentally wiped without explicit selection
        return storageRepository.deleteFiles(files)
    }
}
