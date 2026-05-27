package com.example.feature.timeline

import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import com.example.core.i18n.appString
import com.example.core.i18n.friendlyTimeText
import com.example.core.i18n.localizedEventText
import com.example.core.i18n.t
import com.example.core.ui.InfoCard
import com.example.core.ui.ScreenList
import com.example.data.TimelineEvent
import com.example.data.TimelineEventType

@Composable
fun TimelineScreen(events: List<TimelineEvent>) {
  ScreenList(title = appString(R.string.timeline), subtitle = t("Your safety history.", "Lịch sử an toàn của bạn.")) {
    items(events) { event ->
      InfoCard(
        icon = event.type.icon(),
        title = localizedEventText(event.title),
        body = "${localizedEventText(event.body)} - ${friendlyTimeText(event.createdAt)}",
      )
    }
  }
}

private fun TimelineEventType.icon(): ImageVector = when (this) {
  TimelineEventType.CheckIn -> Icons.Rounded.CheckCircle
  TimelineEventType.Moment -> Icons.Rounded.LocalFlorist
  TimelineEventType.Nudge -> Icons.Rounded.NotificationsActive
  TimelineEventType.FriendRequest -> Icons.Rounded.Group
  TimelineEventType.SafetyRhythm -> Icons.Rounded.Schedule
  TimelineEventType.Emergency -> Icons.Rounded.Warning
}
