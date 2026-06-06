package com.example.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.core.i18n.t
import com.example.core.ui.CardBlock
import com.example.core.ui.EsmeryTextField
import com.example.core.ui.PrimaryButton
import com.example.core.ui.ScreenList
import com.example.core.viewmodel.EsmeryViewModelFactory
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Sage

@Composable
fun ProfileScreen(
  onAccountDeleted: () -> Unit,
  viewModel: ProfileViewModel = viewModel(factory = EsmeryViewModelFactory { ProfileViewModel() }),
) {
  val state by viewModel.esmeryState.collectAsState()
  var displayName by remember(state.profile.displayName) { mutableStateOf(state.profile.displayName) }
  var password by remember { mutableStateOf("") }
  var showDeleteDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current

  val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    uri ?: return@rememberLauncherForActivityResult
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
    viewModel.onEvent(ProfileUiEvent.UploadAvatar(bytes, "avatar-${System.currentTimeMillis()}.jpg"))
  }

  ScreenList(title = t("Profile", "Hồ sơ"), subtitle = t("Manage your account details.", "Quản lý thông tin tài khoản.")) {
    item {
      CardBlock {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Surface(shape = CircleShape, color = Sage, modifier = Modifier.size(72.dp)) {
            if (state.profile.avatarUrl.isNullOrBlank()) {
              Icon(Icons.Rounded.Person, contentDescription = null, tint = Cocoa, modifier = Modifier.size(72.dp))
            } else {
              AsyncImage(
                model = state.profile.avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Crop,
              )
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { avatarPicker.launch("image/*") }) {
              Text(t("Upload avatar", "Tải ảnh đại diện"), color = Cocoa)
            }
            Text(state.profile.email.orEmpty(), color = Cocoa)
          }
        }
      }
    }
    item {
      CardBlock {
        EsmeryTextField(displayName, { displayName = it }, t("Display name", "Tên hiển thị"))
        PrimaryButton(text = t("Save profile", "Lưu hồ sơ")) {
          viewModel.onEvent(ProfileUiEvent.UpdateProfile(displayName, state.profile.avatarUrl))
        }
      }
    }
    item {
      CardBlock {
        EsmeryTextField(password, { password = it }, t("New password", "Mật khẩu mới"))
        PrimaryButton(text = t("Change password", "Đổi mật khẩu")) {
          if (password.isNotBlank()) {
            viewModel.onEvent(ProfileUiEvent.ChangePassword(password))
            password = ""
          }
        }
      }
    }
    item {
      Button(
        onClick = { showDeleteDialog = true },
        colors = ButtonDefaults.buttonColors(containerColor = Apricot),
      ) {
        Icon(Icons.Rounded.Delete, contentDescription = null)
        Text(t("Delete account", "Xóa tài khoản"), fontWeight = FontWeight.Bold)
      }
    }
  }

  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text(t("Delete account?", "Xóa tài khoản?"), color = Cocoa, fontWeight = FontWeight.Black) },
      text = { Text(t("This permanently removes your account and local data.", "Thao tác này sẽ xóa vĩnh viễn tài khoản và dữ liệu cục bộ.")) },
      confirmButton = {
        TextButton(onClick = {
          showDeleteDialog = false
          viewModel.onEvent(ProfileUiEvent.DeleteAccount)
          onAccountDeleted()
        }) { Text(t("Delete", "Xóa")) }
      },
      dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(t("Cancel", "Hủy")) } },
    )
  }
}
