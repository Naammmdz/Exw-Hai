package com.example.feature.safety

import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch

@Composable
fun SafetyScreen(state: EsmeryState, repository: EsmeryRepository, onToast: (String) -> Unit) {
  var label by remember { mutableStateOf("") }
  var time by remember { mutableStateOf("") }
  val scope = rememberCoroutineScope()
  val savedMessage = t("Safety rhythm saved.", "Đã lưu nhịp an toàn.")
  ScreenList(title = appString(R.string.safety_rhythm), subtitle = t("Reminder and inactivity settings are stored as v1 stubs.", "Cài đặt nhắc nhở và phát hiện không hoạt động đang được lưu mô phỏng ở bản v1.")) {
    item {
      CardBlock {
        EsmeryTextField(value = label, onValueChange = { label = it }, label = t("Check label", "Tên lịch xác nhận"))
        EsmeryTextField(value = time, onValueChange = { time = it }, label = t("Time, e.g. 18:00", "Thời gian, ví dụ 18:00"))
        PrimaryButton(text = t("Save rhythm", "Lưu nhịp an toàn")) {
          if (label.isNotBlank() && time.isNotBlank()) {
            scope.launch {
              repository.saveSafetyRhythm(SafetyRhythm(id = "", userId = state.profile.id, label = label, checkTime = time))
              label = ""
              time = ""
              onToast(savedMessage)
            }
          }
        }
      }
    }
    items(state.safetyRhythms) { rhythm ->
      InfoCard(
        icon = Icons.Rounded.Schedule,
        title = localizedRhythmLabel(rhythm.label),
        body = "${rhythm.checkTime} - ${if (rhythm.isEnabled) t("enabled", "đang bật") else t("paused", "đang tạm dừng")}",
      )
    }
    item {
      InfoCard(
        icon = Icons.Rounded.Warning,
        title = t("Escalation delay", "Thời gian chờ trước cảnh báo"),
        body = t(
          "Stored setting stub: alert emergency contacts after missed check-ins.",
          "Cài đặt mô phỏng: cảnh báo liên hệ khẩn cấp sau khi bỏ lỡ xác nhận an toàn.",
        ),
      )
    }
  }
}
