package com.storage.intelligence.data.repository

import android.app.Activity
import com.storage.intelligence.core.billing.BillingManager
import com.storage.intelligence.core.billing.PlanDetails
import com.storage.intelligence.core.billing.SubscriptionState
import com.storage.intelligence.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val billingManager: BillingManager
) : SubscriptionRepository {

    override val subscriptionState: Flow<SubscriptionState> = billingManager.subscriptionState
    override val availablePlans: Flow<List<PlanDetails>> = billingManager.availablePlans

    override suspend fun purchasePlan(activity: Activity, productId: String) {
        billingManager.launchBillingFlow(activity, productId)
    }

    override suspend fun restorePurchases() {
        billingManager.restorePurchases()
    }
}
