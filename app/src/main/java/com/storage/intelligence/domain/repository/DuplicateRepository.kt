package com.storage.intelligence.domain.repository

import com.storage.intelligence.core.billing.PlanDetails
import com.storage.intelligence.core.billing.SubscriptionState
import com.storage.intelligence.domain.model.DuplicateGroup
import com.storage.intelligence.domain.model.SimilarPhotoGroup
import com.storage.intelligence.domain.model.StorageSnapshot
import kotlinx.coroutines.flow.Flow

interface DuplicateRepository {
    fun getExactDuplicateGroupsFlow(): Flow<List<DuplicateGroup>>
    fun getDuplicateVideosFlow(): Flow<List<DuplicateGroup>>
    fun getSimilarPhotoGroupsFlow(): Flow<List<SimilarPhotoGroup>>
    suspend fun runExactDuplicateDetection()
    suspend fun runSimilarPhotoDetection()
}

interface TimelineRepository {
    fun getRecentSnapshotsFlow(limitDays: Int): Flow<List<StorageSnapshot>>
    fun getSnapshotsInRangeFlow(timeframeDays: Int): Flow<List<StorageSnapshot>>
    fun getGrowthReportFlow(timeframeDays: Int): Flow<com.storage.intelligence.domain.model.StorageGrowthReport>
    suspend fun getEarliestTrackingTimestamp(): Long?
    suspend fun recordCurrentSnapshot()
}

interface SubscriptionRepository {
    val subscriptionState: Flow<SubscriptionState>
    val availablePlans: Flow<List<PlanDetails>>
    suspend fun purchasePlan(activity: android.app.Activity, productId: String)
    suspend fun restorePurchases()
}
