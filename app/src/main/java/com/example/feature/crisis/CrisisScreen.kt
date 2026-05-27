package com.example.feature.crisis

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.LocalAppLanguage
import com.example.core.i18n.appString
import com.example.core.i18n.t
import com.example.core.i18n.tr
import com.example.core.ui.CardBlock
import com.example.core.ui.EsmeryTextField
import com.example.core.ui.InfoCard
import com.example.core.ui.PrimaryButton
import com.example.core.ui.ScreenList
import com.example.data.EmergencyContact
import com.example.data.EsmeryRepository
import com.example.data.EsmeryState
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Taupe
import kotlinx.coroutines.launch

@Composable
fun CrisisScreen(state: EsmeryState, repository: EsmeryRepository, onToast: (String) -> Unit) {
  var showAdd by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val language = LocalAppLanguage.current
  val unavailableMessage = t("Contact action is unavailable on this device.", "Thiết bị này không mở được thao tác liên hệ.")
  val savedMessage = t("Emergency contact saved.", "Đã lưu liên hệ khẩn cấp.")
  ScreenList(title = appString(R.string.crisis), subtitle = t("Fast access to contacts and safe steps.", "Truy cập nhanh liên hệ và các bước an toàn.")) {
    item { PrimaryButton(text = t("Add emergency contact", "Thêm liên hệ khẩn cấp"), icon = Icons.Rounded.Add) { showAdd = true } }
    item {
      InfoCard(
        icon = Icons.Rounded.Security,
        title = t("My Safe Steps", "Các bước an toàn của tôi"),
        body = t(
          "Pause, move to a safer place, call a trusted contact, then contact local services if needed.",
          "Dừng lại, di chuyển đến nơi an toàn hơn, gọi người tin cậy, rồi liên hệ dịch vụ địa phương nếu cần.",
        ),
      )
    }
    item {
      InfoCard(
        icon = Icons.Rounded.Warning,
        title = t("Local support", "Hỗ trợ địa phương"),
        body = t("Nearby police stations and hospitals are placeholders in v1.", "Đồn công an và bệnh viện gần đây là dữ liệu mô phỏng ở bản v1."),
      )
    }
    items(state.emergencyContacts) { contact ->
      CardBlock {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Icon(Icons.Rounded.Phone, contentDescription = null, tint = Apricot)
          Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, fontWeight = FontWeight.Bold, color = Cocoa)
            val verified = tr(language, if (contact.isVerified) "yes" else "no", if (contact.isVerified) "có" else "không")
            Text("${contact.contact} - ${tr(language, "verified", "đã xác minh")}: $verified", color = Taupe)
          }
          IconButton(onClick = {
            runCatching {
              context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.contact}")))
            }.onFailure { onToast(unavailableMessage) }
          }) {
            Icon(Icons.Rounded.Call, contentDescription = null, tint = Cocoa)
          }
        }
      }
    }
  }
  if (showAdd) {
    EmergencyContactDialog(onDismiss = { showAdd = false }, onSave = { name, contact ->
      scope.launch {
        repository.saveEmergencyContact(EmergencyContact(id = "", userId = state.profile.id, name = name, contact = contact))
        showAdd = false
        onToast(savedMessage)
      }
    })
  }
}

@Composable
private fun EmergencyContactDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
  var name by remember { mutableStateOf("") }
  var contact by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(t("Emergency contact", "Liên hệ khẩn cấp"), color = Cocoa, fontWeight = FontWeight.Black) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EsmeryTextField(name, { name = it }, t("Name", "Tên"))
        EsmeryTextField(contact, { contact = it }, t("Phone or email", "Số điện thoại hoặc email"))
      }
    },
    confirmButton = { Button(onClick = { if (name.isNotBlank() && contact.isNotBlank()) onSave(name, contact) }) { Text(t("Save", "Lưu")) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel", "Hủy")) } },
  )
}
