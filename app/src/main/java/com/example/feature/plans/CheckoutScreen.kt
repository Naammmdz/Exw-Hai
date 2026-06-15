package com.example.feature.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.i18n.t
import com.example.core.ui.CardBlock
import com.example.core.ui.PrimaryButton
import com.example.core.ui.ScreenList
import com.example.data.BillingManager
import com.example.data.EsmeryState
import com.example.data.PaymentProvider
import com.example.data.SubscriptionPlan
import com.example.data.planAmountVnd
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Taupe

enum class CheckoutProvider { GooglePlay, SePay }

@Composable
fun CheckoutScreen(
  plan: SubscriptionPlan,
  state: EsmeryState,
  billingManager: BillingManager,
  billingReady: Boolean,
  productsLoaded: Boolean,
  billingError: String?,
  activeOrderReference: String?,
  onBack: () -> Unit,
  onSelectProvider: (CheckoutProvider) -> Unit,
  selectedProvider: CheckoutProvider,
  onPayGooglePlay: () -> Unit,
  onPaySePay: () -> Unit,
) {
  val amount = planAmountVnd(plan)
  val planTitle = when (plan) {
    SubscriptionPlan.Monthly -> t("Advanced Monthly", "Nâng cao theo tháng")
    SubscriptionPlan.Yearly -> t("Advanced Yearly", "Nâng cao theo năm")
    SubscriptionPlan.Basic -> t("Basic Care", "Chăm sóc cơ bản")
  }
  val latestOrder = state.paymentOrders.firstOrNull { it.plan == plan && it.provider == PaymentProvider.SePay }

  ScreenList(
    title = t("Checkout", "Thanh toán"),
    subtitle = t("Review your plan and choose a payment method.", "Xem lại gói và chọn phương thức thanh toán."),
  ) {
    item {
      OutlinedButton(onClick = onBack) {
        Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = Cocoa)
        Text(t("Back to plans", "Quay lại gói dịch vụ"), color = Cocoa)
      }
    }
    item {
      CardBlock {
        Text(planTitle, color = Cocoa, fontWeight = FontWeight.Black)
        Text(
          t("$amount VND", "$amount VND"),
          color = Apricot,
          fontWeight = FontWeight.Bold,
        )
        Text(
          t(
            "Includes smart inactivity detection and unlimited contacts.",
            "Gồm phát hiện không hoạt động thông minh và không giới hạn liên hệ.",
          ),
          color = Taupe,
        )
      }
    }
    item {
      CardBlock {
        Text(t("Payment method", "Phương thức thanh toán"), color = Cocoa, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = { onSelectProvider(CheckoutProvider.GooglePlay) },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (selectedProvider == CheckoutProvider.GooglePlay) Apricot else Taupe.copy(alpha = 0.3f),
            ),
          ) {
            Text(t("Google Play", "Google Play"), color = if (selectedProvider == CheckoutProvider.GooglePlay) Color.White else Cocoa)
          }
          Button(
            onClick = { onSelectProvider(CheckoutProvider.SePay) },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (selectedProvider == CheckoutProvider.SePay) Apricot else Taupe.copy(alpha = 0.3f),
            ),
          ) {
            Text(t("SePay QR", "SePay QR"), color = if (selectedProvider == CheckoutProvider.SePay) Color.White else Cocoa)
          }
        }
      }
    }
    item {
      when (selectedProvider) {
        CheckoutProvider.GooglePlay -> {
          CardBlock {
            if (!billingReady) {
              Text(t("Connecting to Google Play...", "Đang kết nối Google Play..."), color = Taupe)
            } else if (!productsLoaded) {
              Text(
                billingError ?: t("Products unavailable.", "Sản phẩm không khả dụng."),
                color = Taupe,
              )
            } else {
              PrimaryButton(text = t("Pay with Google Play", "Thanh toán qua Google Play")) {
                onPayGooglePlay()
              }
              OutlinedButton(onClick = { billingManager.openSubscriptionManagement() }) {
                Text(t("Manage on Google Play", "Quản lý trên Google Play"), color = Cocoa)
              }
            }
          }
        }
        CheckoutProvider.SePay -> {
          CardBlock {
            PrimaryButton(text = t("Create SePay order", "Tạo đơn SePay")) { onPaySePay() }
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
              Text(
                t("Reference: ${latestOrder.referenceCode}", "Mã tham chiếu: ${latestOrder.referenceCode}"),
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
    item {
      CardBlock {
        Text(t("Terms", "Điều khoản"), color = Cocoa, fontWeight = FontWeight.Black)
        Text(
          t(
            "Subscriptions renew automatically unless cancelled. Payment data is processed securely.",
            "Gói sẽ tự gia hạn trừ khi bạn hủy. Dữ liệu thanh toán được xử lý an toàn.",
          ),
          color = Taupe,
        )
      }
    }
  }
}
