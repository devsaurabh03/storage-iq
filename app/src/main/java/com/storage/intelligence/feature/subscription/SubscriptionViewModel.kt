package com.storage.intelligence.feature.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storage.intelligence.core.billing.PlanDetails
import com.storage.intelligence.core.billing.SubscriptionState
import com.storage.intelligence.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    val subscriptionState: StateFlow<SubscriptionState> = subscriptionRepository.subscriptionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionState.Free)

    val availablePlans: StateFlow<List<PlanDetails>> = subscriptionRepository.availablePlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun purchasePlan(activity: Activity, productId: String) {
        viewModelScope.launch {
            subscriptionRepository.purchasePlan(activity, productId)
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            subscriptionRepository.restorePurchases()
        }
    }
}
