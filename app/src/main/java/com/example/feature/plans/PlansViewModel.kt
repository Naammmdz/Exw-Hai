package com.example.feature.plans

import androidx.lifecycle.viewModelScope
import com.example.core.viewmodel.BaseEsmeryViewModel
import com.example.data.EsmeryRepository
import com.example.data.GooglePlayPurchase
import com.example.data.PaymentProvider
import com.example.data.SubscriptionPlan
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlansUiEvent {
  data class SelectPlan(val plan: SubscriptionPlan) : PlansUiEvent
  data class OpenCheckout(val plan: SubscriptionPlan) : PlansUiEvent
  data object CloseCheckout : PlansUiEvent
  data class SelectCheckoutProvider(val provider: CheckoutProvider) : PlansUiEvent
  data class CreateSePayOrder(val plan: SubscriptionPlan) : PlansUiEvent
  data class VerifyGooglePlayPurchase(val purchase: GooglePlayPurchase) : PlansUiEvent
  data object RestorePurchases : PlansUiEvent
}

class PlansViewModel(
  repository: EsmeryRepository? = null,
) : BaseEsmeryViewModel(repository ?: com.example.EsmeryServices.repository) {
  private val _activeOrderReference = MutableStateFlow<String?>(null)
  val activeOrderReference: StateFlow<String?> = _activeOrderReference.asStateFlow()

  private val _checkoutPlan = MutableStateFlow<SubscriptionPlan?>(null)
  val checkoutPlan: StateFlow<SubscriptionPlan?> = _checkoutPlan.asStateFlow()

  private val _checkoutProvider = MutableStateFlow(CheckoutProvider.SePay)
  val checkoutProvider: StateFlow<CheckoutProvider> = _checkoutProvider.asStateFlow()

  fun onEvent(event: PlansUiEvent) {
    when (event) {
      is PlansUiEvent.SelectPlan -> launchAction { repository.updateSubscription(event.plan) }
      is PlansUiEvent.OpenCheckout -> _checkoutPlan.value = event.plan
      PlansUiEvent.CloseCheckout -> _checkoutPlan.value = null
      is PlansUiEvent.SelectCheckoutProvider -> _checkoutProvider.value = event.provider
      is PlansUiEvent.CreateSePayOrder -> launchAction {
        val order = repository.createPaymentOrder(event.plan, PaymentProvider.SePay)
        _activeOrderReference.value = order.referenceCode
        startPaymentPolling(order.referenceCode)
      }
      is PlansUiEvent.VerifyGooglePlayPurchase -> launchAction {
        val entitlement = repository.verifyGooglePlayPurchase(
          event.purchase.purchaseToken,
          event.purchase.productId,
        )
        if (entitlement != null) {
          _checkoutPlan.value = null
          showToast("Payment completed.")
        } else {
          showToast("Purchase verification failed.")
        }
      }
      PlansUiEvent.RestorePurchases -> Unit
    }
  }

  fun handleRestoredPurchase(purchase: GooglePlayPurchase?) {
    if (purchase == null) {
      showToast("No active subscription found.")
      return
    }
    onEvent(PlansUiEvent.VerifyGooglePlayPurchase(purchase))
  }

  private fun startPaymentPolling(referenceCode: String) {
    viewModelScope.launch {
      repeat(30) {
        delay(4_000)
        repository.expireStalePaymentOrders()
        repository.refresh()
        val paid = repository.state.value.paymentOrders.any {
          it.referenceCode == referenceCode && it.status == com.example.data.PaymentOrderStatus.Paid
        }
        if (paid) {
          _activeOrderReference.value = null
          _checkoutPlan.value = null
          showToast("Payment completed.")
          return@launch
        }
      }
    }
  }
}
