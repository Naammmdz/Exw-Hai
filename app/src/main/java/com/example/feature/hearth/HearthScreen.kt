package com.example.feature.hearth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.appString
import com.example.core.i18n.friendlyTimeText
import com.example.core.i18n.t
import com.example.core.ui.InfoCard
import com.example.core.ui.LanguageButton
import com.example.core.ui.ScreenList
import com.example.data.CircleStatus
import com.example.data.EsmeryNotification
import com.example.data.EsmeryState
import com.example.data.NotificationType
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Taupe

@Composable
fun HearthScreen(
  state: EsmeryState,
  onCheckIn: () -> Unit,
  language: AppLanguage,
  onToggleLanguage: () -> Unit,
  onLogout: () -> Unit,
  onNotificationRead: (String) -> Unit,
) {
  val unreadNotifications = state.notifications.filterNot { it.isRead }
  ScreenList(
    title = t("Good morning, ${state.profile.displayName}", "Chào buổi sáng, ${state.profile.displayName}"),
    subtitle = t("Last check-in: ${friendlyTimeText(state.profile.lastSafeAt)}", "Lần xác nhận gần nhất: ${friendlyTimeText(state.profile.lastSafeAt)}"),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LanguageButton(language = language, onClick = onToggleLanguage)
        OutlinedButton(onClick = onLogout, shape = RoundedCornerShape(8.dp)) {
          Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = Cocoa, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(appString(R.string.logout), color = Cocoa, fontWeight = FontWeight.Bold)
        }
      }
    }
    item {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Button(
          onClick = onCheckIn,
          modifier = Modifier.size(190.dp),
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(containerColor = Apricot),
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(54.dp))
            Text(appString(R.string.im_safe), color = Color.White, fontWeight = FontWeight.Black)
          }
        }
      }
    }
    item {
      InfoCard(icon = Icons.Rounded.NotificationsActive, title = t("Safety signal ready", "Tín hiệu an toàn đã sẵn sàng"), body = appString(R.string.circle_notified))
    }
    item {
      val isAttentionNeeded = unreadNotifications.any { it.type == NotificationType.MissedCheckIn || it.type == NotificationType.EmergencyAlert }
      InfoCard(
        icon = if (isAttentionNeeded) Icons.Rounded.Warning else Icons.Rounded.Check,
        title = if (isAttentionNeeded) t("Needs attention", "Cần chú ý") else t("You are marked safe", "Bạn đang được ghi nhận an toàn"),
        body = t(
          "Inactivity window: ${state.safetySettings.inactivityHours}h, escalation delay: ${state.safetySettings.escalationDelayMinutes}m.",
          "Ngưỡng không hoạt động: ${state.safetySettings.inactivityHours} giờ, chờ cảnh báo: ${state.safetySettings.escalationDelayMinutes} phút.",
        ),
      )
    }
    item {
      val count = state.circleMembers.count { it.status == CircleStatus.Accepted }
      InfoCard(
        icon = Icons.Rounded.Group,
        title = t("Circle health", "Tình trạng vòng thân"),
        body = t("$count trusted people connected.", "$count người tin cậy đang kết nối."),
      )
    }
    if (state.notifications.isNotEmpty()) {
      item {
        Text(t("Recent notifications", "Thông báo gần đây"), color = Cocoa, fontWeight = FontWeight.Black)
      }
      items(state.notifications.take(3)) { notification ->
        NotificationCard(notification = notification, onRead = { onNotificationRead(notification.id) })
      }
    }
  }
}

@Composable
private fun NotificationCard(notification: EsmeryNotification, onRead: () -> Unit) {
  InfoCard(
    icon = when (notification.type) {
      NotificationType.CheckInSuccess -> Icons.Rounded.Check
      NotificationType.GentleNudge -> Icons.Rounded.NotificationsActive
      NotificationType.MissedCheckIn -> Icons.Rounded.Warning
      NotificationType.EmergencyAlert -> Icons.Rounded.Warning
      NotificationType.MomentShared -> Icons.Rounded.Group
    },
    title = notificationTitle(notification),
    body = "${notificationBody(notification)} - ${friendlyTimeText(notification.createdAt)}",
  )
  if (!notification.isRead) {
    TextButton(onClick = onRead) {
      Icon(Icons.Rounded.MarkEmailRead, contentDescription = null, tint = Cocoa)
      Spacer(Modifier.width(6.dp))
      Text(t("Mark read", "Đánh dấu đã đọc"), color = Taupe)
    }
  }
}

@Composable
private fun notificationTitle(notification: EsmeryNotification): String = when (notification.title) {
  "Check-in sent" -> t("Check-in sent", "Đã gửi xác nhận an toàn")
  "Gentle nudge sent" -> t("Gentle nudge sent", "Đã gửi nhắc nhở nhẹ")
  "Missed check-in detected" -> t("Missed check-in detected", "Phát hiện bỏ lỡ xác nhận")
  "Emergency alert sent" -> t("Emergency alert sent", "Đã gửi cảnh báo khẩn cấp")
  "Moment shared" -> t("Moment shared", "Đã chia sẻ khoảnh khắc")
  "Sarah sent a hug" -> t("Sarah sent a hug", "Sarah gửi một cái ôm")
  else -> notification.title
}

@Composable
private fun notificationBody(notification: EsmeryNotification): String = when (notification.body) {
  "Your circle has been notified that you are safe." -> t("Your circle has been notified that you are safe.", "Vòng thân đã được báo rằng bạn an toàn.")
  "A gentle reminder from your circle." -> t("A gentle reminder from your circle.", "Một nhắc nhở nhẹ từ vòng thân.")
  "Your safety rhythm needs attention." -> t("Your safety rhythm needs attention.", "Nhịp an toàn của bạn cần được chú ý.")
  else -> notification.body
}
