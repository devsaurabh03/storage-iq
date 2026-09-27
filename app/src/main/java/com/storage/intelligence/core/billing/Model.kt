package com.storage.intelligence.core.billing

sealed interface SubscriptionState {
    data object Free : SubscriptionState
    data class ProTrial(val expiryTimestampMs: Long) : SubscriptionState
    data class ProActive(val productId: String, val expiryTimestampMs: Long) : SubscriptionState
}

data class PlanDetails(
    val productId: String,
    val title: String,
    val priceFormatted: String,
    val billingPeriod: String,
    val hasTrial: Boolean = false,
    val trialPeriodDays: Int = 0
)
