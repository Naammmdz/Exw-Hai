package com.example.feature.crisis

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.entitlement.EntitlementGate
import com.example.core.i18n.LocalAppLanguage
import com.example.core.i18n.appString
import com.example.core.i18n.t
import com.example.core.i18n.tr
import com.example.core.ui.CardBlock
import com.example.core.ui.EsmeryTextField
import com.example.core.ui.InfoCard
import com.example.core.ui.PrimaryButton
import com.example.core.ui.ScreenList
import com.example.data.AlertIncidentStatus
import com.example.data.DeliveryStatus
import com.example.data.EmergencyContact
import com.example.data.EsmeryState
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Taupe
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CrisisScreen(
  state: EsmeryState,
  viewModel: CrisisViewModel,
  onToast: (String) -> Unit,
  onNavigateToPlans: () -> Unit = {},
) {
  var showAdd by remember { mutableStateOf(false) }
  var showPremiumDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val language = LocalAppLanguage.current
  val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
  val unavailableMessage = t("Contact action is unavailable on this device.", "Thiết bị này không mở được thao tác liên hệ.")
  val savedMessage = t("Emergency contact saved.", "Đã lưu liên hệ khẩn cấp.")
  val alertMessage = t("Emergency alert recorded for enabled contacts.", "Đã ghi nhận cảnh báo khẩn cấp cho liên hệ đang bật.")
  val resolvedMessage = t("Alert incident resolved.", "Đã đóng cảnh báo.")
  val locationSharedMessage = t("Emergency location shared.", "Đã chia sẻ vị trí khẩn cấp.")
  val locationUnavailableMessage = t("Location unavailable.", "Không lấy được vị trí.")
  val activeIncident = state.alertIncidents.firstOrNull {
    it.status == AlertIncidentStatus.Active || it.status == AlertIncidentStatus.Escalated
  }
  val latestShare = state.locationShares.firstOrNull()

  LaunchedEffect(Unit) {
    if (!locationPermission.status.isGranted) {
      locationPermission.launchPermissionRequest()
    }
  }

  ScreenList(title = appString(R.string.crisis), subtitle = t("Fast access to contacts and safe steps.", "Truy cập nhanh liên hệ và các bước an toàn.")) {
    item {
      PrimaryButton(text = t("Alert emergency contacts", "Cảnh báo liên hệ khẩn cấp"), icon = Icons.Rounded.Warning) {
        viewModel.onEvent(CrisisUiEvent.TriggerEmergency)
        onToast(alertMessage)
      }
    }
    item {
      OutlinedButton(onClick = {
        if (!locationPermission.status.isGranted) {
          locationPermission.launchPermissionRequest()
          return@OutlinedButton
        }
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation.addOnSuccessListener { location ->
          if (location != null) {
            viewModel.onEvent(
              CrisisUiEvent.ShareLocation(location.latitude, location.longitude, location.accuracy.toDouble()),
            )
            onToast(locationSharedMessage)
          } else {
            onToast(locationUnavailableMessage)
          }
        }
      }) {
        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Cocoa)
        Text(t("Share location", "Chia sẻ vị trí"), color = Cocoa)
      }
    }
    if (latestShare != null) {
      item {
        CardBlock {
          Text(t("Latest location share", "Vị trí chia sẻ gần nhất"), color = Cocoa, fontWeight = FontWeight.Black)
          Text("${latestShare.latitude}, ${latestShare.longitude}", color = Taupe)
          OutlinedButton(onClick = {
            val uri = Uri.parse("geo:${latestShare.latitude},${latestShare.longitude}?q=${latestShare.latitude},${latestShare.longitude}")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
          }) {
            Text(t("Open in Maps", "Mở bản đồ"), color = Cocoa)
          }
        }
      }
    }
    if (activeIncident != null) {
      item {
        CardBlock {
          Text(t("Active alert", "Cảnh báo đang mở"), color = Cocoa, fontWeight = FontWeight.Black)
          Text(
            t(
              "${activeIncident.reason} - escalation due ${activeIncident.escalationDueAt.take(16)}",
              "${activeIncident.reason} - sẽ leo thang lúc ${activeIncident.escalationDueAt.take(16)}",
            ),
            color = Taupe,
          )
          val openDeliveries = state.notificationDeliveries.count {
            it.status == DeliveryStatus.Pending || it.status == DeliveryStatus.Failed
          }
          Text(t("Open deliveries: $openDeliveries", "Thông báo đang xử lý: $openDeliveries"), color = Taupe)
          OutlinedButton(onClick = {
            viewModel.onEvent(CrisisUiEvent.ResolveIncident(activeIncident.id))
            onToast(resolvedMessage)
          }) {
            Text(t("Resolve alert", "Đóng cảnh báo"), color = Cocoa)
          }
        }
      }
    }
    item {
      PrimaryButton(
        text = t("Add emergency contact", "Thêm liên hệ khẩn cấp"),
        icon = Icons.Rounded.Add,
      ) {
        if (EntitlementGate.canAddEmergencyContact(state)) showAdd = true else showPremiumDialog = true
      }
    }
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
        icon = Icons.Rounded.Phone,
        title = t("Vietnam emergency numbers", "Số khẩn cấp tại Việt Nam"),
        body = t("Police 113 - Fire 114 - Ambulance 115.", "Công an 113 - Cứu hỏa 114 - Cấp cứu 115."),
      )
    }
    items(state.emergencyContacts) { contact ->
      EmergencyContactCard(
        contact = contact,
        language = language,
        onDial = {
          runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.contact}")))
          }.onFailure { onToast(unavailableMessage) }
        },
        onDelete = { viewModel.onEvent(CrisisUiEvent.DeleteContact(contact.id)) },
        onToggleVerified = { viewModel.onEvent(CrisisUiEvent.ToggleVerified(contact.id)) },
        onToggleAutoNotify = { viewModel.onEvent(CrisisUiEvent.ToggleAutoNotify(contact.id)) },
      )
    }
  }
  if (showAdd) {
    EmergencyContactDialog(
      onDismiss = { showAdd = false },
      onSave = { name, contact ->
        viewModel.onEvent(CrisisUiEvent.SaveContact(EmergencyContact(id = "", userId = state.profile.id, name = name, contact = contact)))
        showAdd = false
        onToast(savedMessage)
      },
    )
  }
  if (showPremiumDialog) {
    AlertDialog(
      onDismissRequest = { showPremiumDialog = false },
      title = { Text(t("Premium feature", "Tính năng Premium"), color = Cocoa, fontWeight = FontWeight.Black) },
      text = {
        Text(
          t(
            "Advanced plans include unlimited emergency contacts.",
            "Gói Nâng cao cho phép thêm không giới hạn liên hệ khẩn cấp.",
          ),
        )
      },
      confirmButton = {
        TextButton(onClick = {
          showPremiumDialog = false
          onNavigateToPlans()
        }) {
          Text(t("Upgrade", "Nâng cấp"))
        }
      },
      dismissButton = { TextButton(onClick = { showPremiumDialog = false }) { Text(t("Cancel", "Hủy")) } },
    )
  }
}

@Composable
private fun EmergencyContactCard(
  contact: EmergencyContact,
  language: com.example.core.i18n.AppLanguage,
  onDial: () -> Unit,
  onDelete: () -> Unit,
  onToggleVerified: () -> Unit,
  onToggleAutoNotify: () -> Unit,
) {
  CardBlock {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Icon(Icons.Rounded.Phone, contentDescription = null, tint = Apricot)
      Column(modifier = Modifier.weight(1f)) {
        Text(contact.name, fontWeight = FontWeight.Bold, color = Cocoa)
        val verified = tr(language, if (contact.isVerified) "yes" else "no", if (contact.isVerified) "có" else "không")
        val autoNotify = tr(language, if (contact.autoNotify) "on" else "off", if (contact.autoNotify) "bật" else "tắt")
        Text("${contact.contact} - ${tr(language, "verified", "đã xác minh")}: $verified - ${tr(language, "auto notify", "tự động báo")}: $autoNotify", color = Taupe)
      }
      IconButton(onClick = onDial) { Icon(Icons.Rounded.Call, contentDescription = null, tint = Cocoa) }
      IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Taupe) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
      Text(t("Verified", "Đã xác minh"), color = Cocoa)
      Switch(checked = contact.isVerified, onCheckedChange = { onToggleVerified() })
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
      Text(t("Auto notify", "Tự động báo"), color = Cocoa)
      Switch(checked = contact.autoNotify, onCheckedChange = { onToggleAutoNotify() })
    }
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
