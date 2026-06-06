package com.example.feature.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.EsmeryServices
import com.example.core.i18n.appString
import com.example.core.i18n.t
import com.example.core.ui.CardBlock
import com.example.core.ui.ScreenList
import com.example.data.BillingManager
import com.example.data.EsmeryState
import com.example.data.PaymentProvider
import com.example.data.SubscriptionPlan
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Sage
import com.example.ui.theme.Taupe
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Composable
fun PlansScreen(
  state: EsmeryState,
  viewModel: PlansViewModel,
  onToast: (String) -> Unit,
) {
  val context = LocalContext.current
  val activity = context as? android.app.Activity
  val activeOrderReference by viewModel.activeOrderReference.collectAsState()
  val latestOrder = state.paymentOrders.firstOrNull()
  val purchaseCompletedMessage = t("Purchase completed.", "Thanh toán hoàn tất.")
  val billingManager = remember(purchaseCompletedMessage) {
    BillingManager(context) { plan ->
      MainScope().launch {
        EsmeryServices.repository.updateSubscription(plan)
        onToast(purchaseCompletedMessage)
      }
    }
  }

  DisposableEffect(Unit) {
    billingManager.start()
    onDispose { billingManager.end() }
  }

  val basicSelected = t("Basic Care selected.", "Đã chọn gói Chăm sóc cơ bản.")
  val monthlySelected = t("Monthly plan selected.", "Đã chọn gói tháng.")
  val yearlySelected = t("Yearly plan selected.", "Đã chọn gói năm.")
  val orderCreated = t("SePay order created.", "Đã tạo đơn SePay.")

  ScreenList(title = appString(R.string.plans), subtitle = t("Google Play Billing is the release default; SePay orders support private or web checkout.", "Google Play Billing là mặc định cho bản phát hành; đơn SePay dùng cho kênh riêng hoặc web checkout.")) {
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
        if (activity != null) billingManager.launchPurchase(activity, SubscriptionPlan.Monthly)
        viewModel.onEvent(PlansUiEvent.SelectPlan(SubscriptionPlan.Monthly))
        onToast(monthlySelected)
      }
    }
    item {
      PlanCard(
        t("Advanced Yearly", "Nâng cao theo năm"),
        t("499,000 VND/year - monthly features plus priority support.", "499.000 VND/năm - gồm tính năng gói tháng và hỗ trợ ưu tiên."),
        state.subscriptionStatus.plan == SubscriptionPlan.Yearly,
      ) {
        if (activity != null) billingManager.launchPurchase(activity, SubscriptionPlan.Yearly)
        viewModel.onEvent(PlansUiEvent.SelectPlan(SubscriptionPlan.Yearly))
        onToast(yearlySelected)
      }
    }
    item {
      CardBlock {
        Text(t("Production entitlement", "Quyền lợi production"), color = Cocoa, fontWeight = FontWeight.Black)
        Text(
          t(
            "Plan: ${state.entitlement.plan.name}, premium: ${state.entitlement.isPremium}, source: ${state.entitlement.source.name}.",
            "Gói: ${state.entitlement.plan.name}, premium: ${state.entitlement.isPremium}, nguồn: ${state.entitlement.source.name}.",
          ),
          color = Taupe,
        )
        if (latestOrder != null) {
          Text(
            t(
              "Latest order: ${latestOrder.referenceCode} - ${latestOrder.status.name}.",
              "Đơn gần nhất: ${latestOrder.referenceCode} - ${latestOrder.status.name}.",
            ),
            color = Taupe,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(onClick = {
            viewModel.onEvent(PlansUiEvent.CreateSePayOrder(SubscriptionPlan.Monthly))
            onToast(orderCreated)
          }, colors = ButtonDefaults.buttonColors(containerColor = Apricot)) {
            Text(t("SePay monthly", "SePay tháng"), color = Color.White)
          }
          Button(onClick = {
            viewModel.onEvent(PlansUiEvent.CreateSePayOrder(SubscriptionPlan.Yearly))
            onToast(orderCreated)
          }, colors = ButtonDefaults.buttonColors(containerColor = Apricot)) {
            Text(t("SePay yearly", "SePay năm"), color = Color.White)
          }
        }
        if (latestOrder?.qrUrl != null) {
          AsyncImage(
            model = latestOrder.qrUrl,
            contentDescription = t("SePay QR", "Mã QR SePay"),
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentScale = ContentScale.Fit,
          )
          Text(
            t("Scan to pay. Status updates automatically.", "Quét để thanh toán. Trạng thái sẽ tự cập nhật."),
            color = Taupe,
          )
        }
        if (activeOrderReference != null) {
          Text(t("Waiting for payment: $activeOrderReference", "Đang chờ thanh toán: $activeOrderReference"), color = Taupe)
        }
      }
    }
  }
}

@Composable
private fun PlanCard(title: String, body: String, selected: Boolean, onSelect: () -> Unit) {
  CardBlock(border = if (selected) BorderStroke(2.dp, Apricot) else null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Icon(if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.CreditCard, contentDescription = null, tint = Apricot)
      Column(modifier = Modifier.weight(1f)) {
        Text(title, color = Cocoa, fontWeight = FontWeight.Black)
        Text(body, color = Taupe)
      }
      Button(onClick = onSelect, colors = ButtonDefaults.buttonColors(containerColor = if (selected) Sage else Apricot)) {
        Text(if (selected) t("Active", "Đang dùng") else t("Choose", "Chọn"), color = if (selected) Cocoa else Color.White)
      }
    }
  }
}
