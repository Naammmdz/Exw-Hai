package com.example.feature.plans

import androidx.lifecycle.viewModelScope
import com.example.core.viewmodel.BaseEsmeryViewModel
import com.example.data.EsmeryRepository
import com.example.data.PaymentProvider
import com.example.data.SubscriptionPlan
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlansUiEvent {
  data class SelectPlan(val plan: SubscriptionPlan) : PlansUiEvent
  data class CreateSePayOrder(val plan: SubscriptionPlan) : PlansUiEvent
  data object PollPaymentStatus : PlansUiEvent
}

class PlansViewModel(
  repository: EsmeryRepository? = null,
) : BaseEsmeryViewModel(repository ?: com.example.EsmeryServices.repository) {
  private val _activeOrderReference = MutableStateFlow<String?>(null)
  val activeOrderReference: StateFlow<String?> = _activeOrderReference.asStateFlow()

  fun onEvent(event: PlansUiEvent) {
    when (event) {
      is PlansUiEvent.SelectPlan -> launchAction { repository.updateSubscription(event.plan) }
      is PlansUiEvent.CreateSePayOrder -> launchAction {
        val order = repository.createPaymentOrder(event.plan, PaymentProvider.SePay)
        _activeOrderReference.value = order.referenceCode
        startPaymentPolling(order.referenceCode)
      }
      PlansUiEvent.PollPaymentStatus -> launchAction {
        _activeOrderReference.value?.let { repository.markPaymentOrderPaid(it) }
        repository.refresh()
      }
    }
  }

  private fun startPaymentPolling(referenceCode: String) {
    viewModelScope.launch {
      repeat(30) {
        delay(4_000)
        repository.refresh()
        val paid = repository.state.value.paymentOrders.any {
          it.referenceCode == referenceCode && it.status == com.example.data.PaymentOrderStatus.Paid
        }
        if (paid) {
          _activeOrderReference.value = null
          showToast("Payment completed.")
          return@launch
        }
      }
    }
  }
}
