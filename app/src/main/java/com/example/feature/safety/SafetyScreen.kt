package com.example.feature.safety

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.appString
import com.example.core.i18n.localizedRhythmLabel
import com.example.core.i18n.t
import com.example.core.ui.CardBlock
import com.example.core.ui.EsmeryTextField
import com.example.core.ui.InfoCard
import com.example.core.ui.PrimaryButton
import com.example.core.ui.ScreenList
import com.example.data.EsmeryRepository
import com.example.data.EsmeryState
import com.example.data.SafetyRhythm
import com.example.data.SafetySettings
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Taupe
import kotlinx.coroutines.launch

@Composable
fun SafetyScreen(state: EsmeryState, repository: EsmeryRepository, onToast: (String) -> Unit) {
  var label by remember { mutableStateOf("") }
  var time by remember { mutableStateOf("") }
  var editingId by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()
  val savedMessage = t("Safety rhythm saved.", "Đã lưu nhịp an toàn.")
  val settingsSavedMessage = t("Safety settings saved.", "Đã lưu cài đặt an toàn.")
  ScreenList(title = appString(R.string.safety_rhythm), subtitle = t("Daily reminders and missed-check detection for your private safety rhythm.", "Nhắc hằng ngày và phát hiện bỏ lỡ xác nhận theo nhịp an toàn riêng tư.")) {
    item {
      CardBlock {
        EsmeryTextField(value = label, onValueChange = { label = it }, label = t("Check label", "Tên lịch xác nhận"))
        EsmeryTextField(value = time, onValueChange = { time = it }, label = t("Time, e.g. 18:00", "Thời gian, ví dụ 18:00"))
        PrimaryButton(text = t("Save rhythm", "Lưu nhịp an toàn")) {
          if (label.isNotBlank() && time.isNotBlank()) {
            scope.launch {
              repository.saveSafetyRhythm(SafetyRhythm(id = editingId.orEmpty(), userId = state.profile.id, label = label, checkTime = time))
              editingId = null
              label = ""
              time = ""
              onToast(savedMessage)
            }
          }
        }
      }
    }
    item {
      SafetySettingsCard(
        settings = state.safetySettings,
        onSave = { settings ->
          scope.launch {
            repository.updateSafetySettings(settings)
            onToast(settingsSavedMessage)
          }
        },
      )
    }
    items(state.safetyRhythms) { rhythm ->
      CardBlock {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Icon(Icons.Rounded.Schedule, contentDescription = null, tint = Cocoa)
          Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
            Text(localizedRhythmLabel(rhythm.label), color = Cocoa, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
            Text("${rhythm.checkTime} - ${if (rhythm.isEnabled) t("enabled", "đang bật") else t("paused", "đang tạm dừng")}", color = Taupe)
          }
          Switch(
            checked = rhythm.isEnabled,
            onCheckedChange = {
              scope.launch { repository.toggleSafetyRhythm(rhythm.id) }
            },
          )
          IconButton(onClick = {
            editingId = rhythm.id
            label = rhythm.label
            time = rhythm.checkTime
          }) {
            Icon(Icons.Rounded.Edit, contentDescription = null, tint = Cocoa)
          }
          IconButton(onClick = {
            scope.launch { repository.deleteSafetyRhythm(rhythm.id) }
          }) {
            Icon(Icons.Rounded.Delete, contentDescription = null, tint = Taupe)
          }
        }
      }
    }
    item {
      InfoCard(
        icon = Icons.Rounded.Warning,
        title = t("Escalation delay", "Thời gian chờ trước cảnh báo"),
        body = t(
          "Auto-notify emergency contacts after missed check-ins when they are enabled.",
          "Tự động báo liên hệ khẩn cấp sau khi bỏ lỡ xác nhận nếu liên hệ đang bật.",
        ),
      )
    }
  }
}

@Composable
private fun SafetySettingsCard(settings: SafetySettings, onSave: (SafetySettings) -> Unit) {
  var inactivityHours by remember(settings) { mutableStateOf(settings.inactivityHours) }
  var escalationDelay by remember(settings) { mutableStateOf(settings.escalationDelayMinutes) }
  var locationSharing by remember(settings) { mutableStateOf(settings.locationSharingEnabled) }
  CardBlock {
    Text(t("Missed check-in detection", "Phát hiện bỏ lỡ xác nhận"), color = Cocoa, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
    Text(t("Inactivity window", "Ngưỡng không hoạt động"), color = Taupe)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf(2, 4, 12).forEach { hours ->
        FilterChip(selected = inactivityHours == hours, onClick = { inactivityHours = hours }, label = { Text("${hours}h") })
      }
    }
    Text(t("Escalation delay", "Thời gian chờ cảnh báo"), color = Taupe)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf(15, 30, 60).forEach { minutes ->
        FilterChip(selected = escalationDelay == minutes, onClick = { escalationDelay = minutes }, label = { Text("${minutes}m") })
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
      Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
        Text(t("Location sharing", "Chia sẻ vị trí"), color = Cocoa, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(t("Available for emergency-only flows.", "Chỉ dùng cho luồng khẩn cấp."), color = Taupe)
      }
      Switch(checked = locationSharing, onCheckedChange = { locationSharing = it })
    }
    PrimaryButton(text = t("Save settings", "Lưu cài đặt")) {
      onSave(
        settings.copy(
          inactivityHours = inactivityHours,
          escalationDelayMinutes = escalationDelay,
          locationSharingEnabled = locationSharing,
        ),
      )
    }
  }
}
