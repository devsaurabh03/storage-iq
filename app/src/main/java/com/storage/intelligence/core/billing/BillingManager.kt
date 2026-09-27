package com.storage.intelligence.core.billing

import android.app.Activity
import com.storage.intelligence.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface BillingManager {
    val subscriptionState: Flow<SubscriptionState>
    val availablePlans: Flow<List<PlanDetails>>
    suspend fun launchBillingFlow(activity: Activity, productId: String)
    suspend fun restorePurchases()
}

@Singleton
class BillingManagerImpl @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : BillingManager {

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(
        SubscriptionState.ProActive("pro_unlocked", Long.MAX_VALUE)
    )
    override val subscriptionState: Flow<SubscriptionState> = _subscriptionState.asStateFlow()

    private val _availablePlans = MutableStateFlow(
        listOf(
            PlanDetails("pro_unlocked", "StorageIQ Pro (Free)", "$0.00", "Lifetime", hasTrial = false)
        )
    )
    override val availablePlans: Flow<List<PlanDetails>> = _availablePlans.asStateFlow()

    override suspend fun launchBillingFlow(activity: Activity, productId: String) {
        _subscriptionState.value = SubscriptionState.ProActive("pro_unlocked", Long.MAX_VALUE)
        preferencesRepository.setSubscriptionStatus("PRO")
    }

    override suspend fun restorePurchases() {
        _subscriptionState.value = SubscriptionState.ProActive("pro_unlocked", Long.MAX_VALUE)
        preferencesRepository.setSubscriptionStatus("PRO")
    }
}
