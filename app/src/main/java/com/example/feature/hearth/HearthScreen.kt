package com.example.feature.hearth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.example.data.EsmeryState
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa

@Composable
fun HearthScreen(
  state: EsmeryState,
  onCheckIn: () -> Unit,
  language: AppLanguage,
  onToggleLanguage: () -> Unit,
  onLogout: () -> Unit,
) {
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
      val count = state.circleMembers.count { it.status == CircleStatus.Accepted }
      InfoCard(
        icon = Icons.Rounded.Group,
        title = t("Circle health", "Tình trạng vòng thân"),
        body = t("$count trusted people connected.", "$count người tin cậy đang kết nối."),
      )
    }
  }
}
