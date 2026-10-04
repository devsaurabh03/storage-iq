package com.storage.intelligence.domain.usecase

import com.storage.intelligence.core.common.WhatsAppMediaDetector
import com.storage.intelligence.data.scanner.FileHasher
import com.storage.intelligence.domain.model.CorrelationType
import com.storage.intelligence.domain.model.FileItem
import com.storage.intelligence.domain.model.SentReceivedCorrelationGroup
import com.storage.intelligence.domain.model.WhatsAppSentReceivedSummary
import com.storage.intelligence.domain.repository.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class GetWhatsAppSentReceivedUseCase @Inject constructor(
    private val storageRepository: StorageRepository,
    private val fileHasher: FileHasher
) {

    operator fun invoke(sourceApp: String = "WhatsApp"): Flow<Pair<WhatsAppSentReceivedSummary, List<SentReceivedCorrelationGroup>>> = flow {
        storageRepository.getMessagingMediaFlow(sourceApp).collect { files ->
            val sentFiles = files.filter { WhatsAppMediaDetector.isSentMedia(it.path) }
            val receivedFiles = files.filter { !WhatsAppMediaDetector.isSentMedia(it.path) }

            val correlationGroups = mutableListOf<SentReceivedCorrelationGroup>()
            val matchedSentIds = mutableSetOf<Long>()
            val matchedReceivedIds = mutableSetOf<Long>()

            // 1. Exact match correlation (via SHA-256 or matching size + content verification)
            for (sent in sentFiles) {
                if (matchedSentIds.contains(sent.id)) continue

                // Find candidate matches in received files
                val candidateReceived = receivedFiles.firstOrNull { recv ->
                    !matchedReceivedIds.contains(recv.id) &&
                            ((sent.sha256Hash != null && recv.sha256Hash != null && sent.sha256Hash == recv.sha256Hash) ||
                             (sent.sizeBytes == recv.sizeBytes && sent.name == recv.name))
                }

                if (candidateReceived != null) {
                    matchedSentIds.add(sent.id)
                    matchedReceivedIds.add(candidateReceived.id)

                    val isTemporal = candidateReceived.modifiedTimeMs <= sent.modifiedTimeMs
                    val correlationType = if (isTemporal) {
                        CorrelationType.STRONG_TEMPORAL
                    } else {
                        CorrelationType.EXACT_MATCH
                    }

                    correlationGroups.add(
                        SentReceivedCorrelationGroup(
                            id = "exact_${sent.id}_${candidateReceived.id}",
                            sentFile = sent,
                            receivedFile = candidateReceived,
                            correlationType = correlationType,
                            reclaimableBytes = sent.sizeBytes
                        )
                    )
                }
            }

            // 2. Visual similarity correlation (for images with perceptual hashes)
            val remainingSentImages = sentFiles.filter { !matchedSentIds.contains(it.id) && (it.mimeType.startsWith("image/") || it.perceptualHash != null) }
            val remainingReceivedImages = receivedFiles.filter { !matchedReceivedIds.contains(it.id) && (it.mimeType.startsWith("image/") || it.perceptualHash != null) }

            for (sent in remainingSentImages) {
                if (matchedSentIds.contains(sent.id)) continue
                val sentHash = sent.perceptualHash ?: fileHasher.computeDHash(File(sent.path), sent.id) ?: continue

                for (recv in remainingReceivedImages) {
                    if (matchedReceivedIds.contains(recv.id)) continue
                    val recvHash = recv.perceptualHash ?: fileHasher.computeDHash(File(recv.path), recv.id) ?: continue

                    val distance = fileHasher.calculateHammingDistance(sentHash, recvHash)
                    if (distance <= 10) {
                        matchedSentIds.add(sent.id)
                        matchedReceivedIds.add(recv.id)

                        correlationGroups.add(
                            SentReceivedCorrelationGroup(
                                id = "visual_${sent.id}_${recv.id}",
                                sentFile = sent,
                                receivedFile = recv,
                                correlationType = CorrelationType.VISUAL_SIMILARITY,
                                reclaimableBytes = sent.sizeBytes
                            )
                        )
                        break
                    }
                }
            }

            val exactCount = correlationGroups.count { it.correlationType == CorrelationType.EXACT_MATCH || it.correlationType == CorrelationType.STRONG_TEMPORAL }
            val similarCount = correlationGroups.count { it.correlationType == CorrelationType.VISUAL_SIMILARITY }
            val totalReclaimable = correlationGroups.sumOf { it.reclaimableBytes }
            val totalCorrelatedBytes = correlationGroups.sumOf { it.sentFile.sizeBytes + it.receivedFile.sizeBytes }

            val summary = WhatsAppSentReceivedSummary(
                totalCorrelatedPairs = correlationGroups.size,
                totalCorrelatedBytes = totalCorrelatedBytes,
                exactMatchCount = exactCount,
                similarMatchCount = similarCount,
                reclaimableBytes = totalReclaimable
            )

            emit(Pair(summary, correlationGroups))
        }
    }.flowOn(Dispatchers.IO)
}
