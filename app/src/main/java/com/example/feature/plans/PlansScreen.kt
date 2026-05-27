package com.example.feature.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.appString
import com.example.core.i18n.t
import com.example.core.ui.CardBlock
import com.example.core.ui.ScreenList
import com.example.data.EsmeryRepository
import com.example.data.EsmeryState
import com.example.data.SubscriptionPlan
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Sage
import com.example.ui.theme.Taupe
import kotlinx.coroutines.launch

@Composable
fun PlansScreen(state: EsmeryState, repository: EsmeryRepository, onToast: (String) -> Unit) {
  val scope = rememberCoroutineScope()
  val basicSelected = t("Basic Care selected.", "Đã chọn gói Chăm sóc cơ bản.")
  val monthlySelected = t("Monthly plan selected.", "Đã chọn gói tháng.")
  val yearlySelected = t("Yearly plan selected.", "Đã chọn gói năm.")
  ScreenList(title = appString(R.string.plans), subtitle = t("Checkout is a functional stub in v1.", "Thanh toán đang được mô phỏng ở bản v1.")) {
    item {
      PlanCard(
        t("Basic Care", "Chăm sóc cơ bản"),
        t("Free - manual daily check-in, 1 family notification.", "Miễn phí - check-in thủ công hằng ngày, thông báo cho 1 người thân."),
        state.subscriptionStatus.plan == SubscriptionPlan.Basic,
      ) {
        scope.launch { repository.updateSubscription(SubscriptionPlan.Basic); onToast(basicSelected) }
      }
    }
    item {
      PlanCard(
        t("Advanced Monthly", "Nâng cao theo tháng"),
        t("49,000 VND/month - smart inactivity detection and unlimited contacts.", "49.000 VND/tháng - phát hiện không hoạt động thông minh và không giới hạn liên hệ."),
        state.subscriptionStatus.plan == SubscriptionPlan.Monthly,
      ) {
        scope.launch { repository.updateSubscription(SubscriptionPlan.Monthly); onToast(monthlySelected) }
      }
    }
    item {
      PlanCard(
        t("Advanced Yearly", "Nâng cao theo năm"),
        t("499,000 VND/year - monthly features plus priority support.", "499.000 VND/năm - gồm tính năng gói tháng và hỗ trợ ưu tiên."),
        state.subscriptionStatus.plan == SubscriptionPlan.Yearly,
      ) {
        scope.launch { repository.updateSubscription(SubscriptionPlan.Yearly); onToast(yearlySelected) }
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
