package com.example.feature.circle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.LocalAppLanguage
import com.example.core.i18n.appString
import com.example.core.i18n.friendlyTimeText
import com.example.core.i18n.localizedCircleStatus
import com.example.core.i18n.localizedRelationship
import com.example.core.i18n.t
import com.example.core.i18n.tr
import com.example.core.ui.CardBlock
import com.example.core.ui.EsmeryTextField
import com.example.core.ui.InfoCard
import com.example.core.ui.PrimaryButton
import com.example.core.ui.ScreenList
import com.example.data.CircleMember
import com.example.data.CircleStatus
import com.example.data.EsmeryRepository
import com.example.data.EsmeryState
import com.example.data.FriendRequest
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Sage
import com.example.ui.theme.Taupe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CircleScreen(
  state: EsmeryState,
  repository: EsmeryRepository,
  onToast: (String) -> Unit,
  actionScope: CoroutineScope = rememberCoroutineScope(),
) {
  var showAdd by remember { mutableStateOf(false) }
  val language = LocalAppLanguage.current
  val acceptedMessage = t("Friend request accepted.", "Đã chấp nhận lời mời.")
  val declinedMessage = t("Friend request declined.", "Đã từ chối lời mời.")
  val invitationSentMessage = t("Invitation sent.", "Đã gửi lời mời.")
  val refreshedMessage = t("Circle updated.", "Đã cập nhật vòng thân.")

  ScreenList(title = appString(R.string.circle), subtitle = t("Trusted people who can receive safety alerts.", "Những người tin cậy có thể nhận cảnh báo an toàn.")) {
    item {
      PrimaryButton(text = appString(R.string.add_friend), icon = Icons.Rounded.Add) { showAdd = true }
    }
    item {
      OutlinedButton(
        onClick = {
          actionScope.launch {
            repository.refresh()
            onToast(refreshedMessage)
          }
        },
        shape = RoundedCornerShape(8.dp),
      ) {
        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Cocoa)
        Text(t("Refresh Circle", "Cập nhật Vòng thân"), color = Cocoa)
      }
    }
    if (state.friendRequests.isEmpty() && state.circleMembers.isEmpty()) {
      item {
        InfoCard(
          icon = Icons.Rounded.PersonSearch,
          title = t("No trusted people yet", "Chưa có người tin cậy"),
          body = t(
            "Add a real email, phone, or friend ID to start your Circle.",
            "Thêm email, số điện thoại hoặc ID thật để bắt đầu Vòng thân.",
          ),
        )
      }
    }
    items(state.friendRequests) { request ->
      FriendRequestCard(
        request = request,
        currentUserId = state.profile.id,
        onAccept = {
          actionScope.launch {
            repository.updateFriendRequest(request.id, CircleStatus.Accepted)
            onToast(acceptedMessage)
          }
        },
        onDecline = {
          actionScope.launch {
            repository.updateFriendRequest(request.id, CircleStatus.Declined)
            onToast(declinedMessage)
          }
        },
      )
    }
    items(state.circleMembers) { member ->
      CircleMemberCard(member, onNudge = {
        actionScope.launch {
          repository.sendNudge(member.id)
          onToast(tr(language, "Gentle nudge sent to ${member.name}.", "Đã gửi nhắc nhở nhẹ nhàng cho ${member.name}."))
        }
      })
    }
  }

  if (showAdd) {
    AddFriendDialog(onDismiss = { showAdd = false }, onAdd = { contact, name, relationship ->
      actionScope.launch {
        repository.addFriendRequest(contact, name, relationship)
        showAdd = false
        onToast(invitationSentMessage)
      }
    })
  }
}

@Composable
private fun CircleMemberCard(member: CircleMember, onNudge: () -> Unit) {
  CardBlock {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Surface(shape = CircleShape, color = Sage, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) { Text(member.name.take(1), color = Cocoa, fontWeight = FontWeight.Black) }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(member.name, color = Cocoa, fontWeight = FontWeight.Bold)
        Text("${localizedRelationship(member.relationship)} - ${localizedCircleStatus(member.status)} - ${friendlyTimeText(member.lastSafeAt)}", color = Taupe)
      }
      OutlinedButton(onClick = onNudge, shape = RoundedCornerShape(8.dp)) {
        Text(t("Nudge", "Nhắc nhẹ"), color = Cocoa)
      }
    }
  }
}

@Composable
private fun FriendRequestCard(
  request: FriendRequest,
  currentUserId: String,
  onAccept: () -> Unit,
  onDecline: () -> Unit,
) {
  val isReceivedRequest = request.senderUserId != currentUserId
  CardBlock {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Icon(Icons.Rounded.Group, contentDescription = null, tint = Apricot)
      Column(modifier = Modifier.weight(1f)) {
        Text(
          if (isReceivedRequest) t("Incoming request", "Lời mời nhận được") else t("Sent request", "Lời mời đã gửi"),
          color = Cocoa,
          fontWeight = FontWeight.Bold,
        )
        Text(
          if (isReceivedRequest) {
            t("From ${request.senderUserId.take(8)} - ${localizedCircleStatus(request.status)}", "Từ ${request.senderUserId.take(8)} - ${localizedCircleStatus(request.status)}")
          } else {
            "${request.receiverContact} - ${localizedCircleStatus(request.status)}"
          },
          color = Taupe,
        )
      }
      if (request.status == CircleStatus.Pending && isReceivedRequest) {
        IconButton(onClick = onAccept) { Icon(Icons.Rounded.Check, contentDescription = null, tint = Cocoa) }
        IconButton(onClick = onDecline) { Icon(Icons.Rounded.Close, contentDescription = null, tint = Taupe) }
      }
    }
  }
}

private enum class AddFriendMode {
  Contact,
  Id,
  Qr,
}

@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
  var contact by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var relationship by remember { mutableStateOf("") }
  var mode by remember { mutableStateOf(AddFriendMode.Contact) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(appString(R.string.add_friend), color = Cocoa, fontWeight = FontWeight.Black) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(selected = mode == AddFriendMode.Contact, onClick = { mode = AddFriendMode.Contact }, label = { Text(t("Contact", "Liên hệ")) })
          FilterChip(selected = mode == AddFriendMode.Id, onClick = { mode = AddFriendMode.Id }, label = { Text("ID") })
          FilterChip(selected = mode == AddFriendMode.Qr, onClick = { mode = AddFriendMode.Qr }, label = { Text("QR") })
        }
        EsmeryTextField(
          contact,
          { contact = it },
          when (mode) {
            AddFriendMode.Contact -> t("Email or phone", "Email hoặc số điện thoại")
            AddFriendMode.Id -> t("Friend ID", "ID bạn bè")
            AddFriendMode.Qr -> t("QR result", "Kết quả QR")
          },
        )
        if (mode == AddFriendMode.Qr) {
          Text(t("QR scanner can be connected here; paste a scanned code for MVP.", "Có thể nối máy quét QR tại đây; MVP dùng cách dán mã đã quét."), color = Taupe)
        }
        EsmeryTextField(name, { name = it }, t("Name", "Tên"))
        EsmeryTextField(relationship, { relationship = it }, t("Relationship", "Mối quan hệ"))
      }
    },
    confirmButton = { Button(onClick = { if (contact.isNotBlank()) onAdd(contact, name, relationship) }) { Text(t("Send", "Gửi")) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel", "Hủy")) } },
  )
}
