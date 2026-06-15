package com.example.feature.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.appString
import com.example.core.i18n.t
import com.example.core.ui.CardBlock
import com.example.core.ui.ScreenList
import com.example.data.BillingManager
import com.example.data.EsmeryState
import com.example.data.SubscriptionPlan
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Sage
import com.example.ui.theme.Taupe
import kotlinx.coroutines.launch

@Composable
fun PlansScreen(
  state: EsmeryState,
  viewModel: PlansViewModel,
  onToast: (String) -> Unit,
) {
  val context = LocalContext.current
  val activity = context as? android.app.Activity
  val scope = rememberCoroutineScope()
  val checkoutPlan by viewModel.checkoutPlan.collectAsState()
  val checkoutProvider by viewModel.checkoutProvider.collectAsState()
  val activeOrderReference by viewModel.activeOrderReference.collectAsState()
  val paymentToast by viewModel.toast.collectAsState()

  val billingManager = remember {
    BillingManager(context) { purchase ->
      scope.launch {
        viewModel.onEvent(PlansUiEvent.VerifyGooglePlayPurchase(purchase))
      }
    }
  }
  val billingReady by billingManager.isReady.collectAsState()
  val productsLoaded by billingManager.productsLoaded.collectAsState()
  val billingError by billingManager.billingError.collectAsState()

  DisposableEffect(Unit) {
    billingManager.start()
    onDispose { billingManager.end() }
  }

  LaunchedEffect(paymentToast) {
    paymentToast?.let {
      onToast(it)
      viewModel.clearToast()
    }
  }

  if (checkoutPlan != null && checkoutPlan != SubscriptionPlan.Basic) {
    CheckoutScreen(
      plan = checkoutPlan!!,
      state = state,
      billingManager = billingManager,
      billingReady = billingReady,
      productsLoaded = productsLoaded,
      billingError = billingError,
      activeOrderReference = activeOrderReference,
      onBack = { viewModel.onEvent(PlansUiEvent.CloseCheckout) },
      onSelectProvider = { viewModel.onEvent(PlansUiEvent.SelectCheckoutProvider(it)) },
      selectedProvider = checkoutProvider,
      onPayGooglePlay = {
        if (activity != null && billingManager.canPurchase(checkoutPlan!!)) {
          billingManager.launchPurchase(activity, checkoutPlan!!)
        }
      },
      onPaySePay = { viewModel.onEvent(PlansUiEvent.CreateSePayOrder(checkoutPlan!!)) },
    )
    return
  }

  val basicSelected = t("Basic Care selected.", "Đã chọn gói Chăm sóc cơ bản.")
  val openCheckout: (SubscriptionPlan) -> Unit = { plan ->
    viewModel.onEvent(
      PlansUiEvent.SelectCheckoutProvider(
        if (billingReady && productsLoaded) CheckoutProvider.GooglePlay else CheckoutProvider.SePay,
      ),
    )
    viewModel.onEvent(PlansUiEvent.OpenCheckout(plan))
  }

  ScreenList(
    title = appString(R.string.plans),
    subtitle = t(
      "Choose a plan, then checkout with Google Play or SePay.",
      "Chọn gói, sau đó thanh toán qua Google Play hoặc SePay.",
    ),
  ) {
    item {
      SubscriptionStatusCard(state = state, billingManager = billingManager)
    }
    item {
      PlanCard(
        t("Basic Care", "Chăm sóc cơ bản"),
        t("Free - manual daily check-in, 1 family notification.", "Miễn phí - check-in thủ công hằng ngày, thông báo cho 1 người thân."),
        state.subscriptionStatus.plan == SubscriptionPlan.Basic,
      ) {
        viewModel.onEvent(PlansUiEvent.SelectPlan(SubscriptionPlan.Basic))
        onToast(basicSelected)
      }
    }
    item {
      PlanCard(
        t("Advanced Monthly", "Nâng cao theo tháng"),
        t("49,000 VND/month - smart inactivity detection and unlimited contacts.", "49.000 VND/tháng - phát hiện không hoạt động thông minh và không giới hạn liên hệ."),
        state.subscriptionStatus.plan == SubscriptionPlan.Monthly,
      ) {
        openCheckout(SubscriptionPlan.Monthly)
      }
    }
    item {
      PlanCard(
        t("Advanced Yearly", "Nâng cao theo năm"),
        t("499,000 VND/year - monthly features plus priority support.", "499.000 VND/năm - gồm tính năng gói tháng và hỗ trợ ưu tiên."),
        state.subscriptionStatus.plan == SubscriptionPlan.Yearly,
      ) {
        openCheckout(SubscriptionPlan.Yearly)
      }
    }
    item {
      OutlinedButton(
        onClick = {
          billingManager.restorePurchases { purchase ->
            viewModel.handleRestoredPurchase(purchase)
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(t("Restore purchases", "Khôi phục gói"), color = Cocoa)
      }
    }
    if (!billingReady || !productsLoaded) {
      item {
        Text(
          billingError ?: t(
            "Google Play is not ready yet. You can still pay with SePay QR.",
            "Google Play chưa sẵn sàng. Bạn vẫn có thể thanh toán qua SePay QR.",
          ),
          color = Taupe,
        )
      }
    }
    if (state.paymentOrders.isNotEmpty()) {
      item {
        CardBlock {
          Text(t("Order history", "Lịch sử đơn hàng"), color = Cocoa, fontWeight = FontWeight.Black)
          state.paymentOrders.take(5).forEach { order ->
            Text(
              t(
                "${order.referenceCode} - ${order.plan.name} - ${order.status.name} (${order.provider.name})",
                "${order.referenceCode} - ${order.plan.name} - ${order.status.name} (${order.provider.name})",
              ),
              color = Taupe,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SubscriptionStatusCard(state: EsmeryState, billingManager: BillingManager) {
  CardBlock {
    Text(t("Current subscription", "Gói hiện tại"), color = Cocoa, fontWeight = FontWeight.Black)
    Text(
      t(
        "Plan: ${state.entitlement.plan.name}, premium: ${state.entitlement.isPremium}, source: ${state.entitlement.source.name}.",
        "Gói: ${state.entitlement.plan.name}, premium: ${state.entitlement.isPremium}, nguồn: ${state.entitlement.source.name}.",
      ),
      color = Taupe,
    )
    state.entitlement.validUntil?.let { validUntil ->
      Text(t("Valid until: $validUntil", "Hết hạn: $validUntil"), color = Taupe)
    }
    if (state.entitlement.source == com.example.data.EntitlementSource.GooglePlay) {
      OutlinedButton(onClick = { billingManager.openSubscriptionManagement() }) {
        Text(t("Manage on Google Play", "Quản lý trên Google Play"), color = Cocoa)
      }
    }
    if (state.entitlement.source == com.example.data.EntitlementSource.SePay) {
      Text(
        t("Contact support to cancel SePay subscriptions.", "Liên hệ hỗ trợ để hủy gói SePay."),
        color = Taupe,
      )
    }
  }
}

@Composable
private fun PlanCard(
  title: String,
  body: String,
  selected: Boolean,
  enabled: Boolean = true,
  onSelect: () -> Unit,
) {
  CardBlock(border = if (selected) BorderStroke(2.dp, Apricot) else null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Icon(if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.CreditCard, contentDescription = null, tint = Apricot)
      Column(modifier = Modifier.weight(1f)) {
        Text(title, color = Cocoa, fontWeight = FontWeight.Black)
        Text(body, color = Taupe)
      }
      Button(
        onClick = onSelect,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = if (selected) Sage else Apricot),
      ) {
        Text(if (selected) t("Active", "Đang dùng") else t("Choose", "Chọn"), color = if (selected) Cocoa else Color.White)
      }
    }
  }
}
