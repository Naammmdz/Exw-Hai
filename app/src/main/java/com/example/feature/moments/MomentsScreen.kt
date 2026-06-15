package com.example.feature.moments

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.i18n.appString
import com.example.core.i18n.friendlyTimeText
import com.example.core.i18n.localizedEventText
import com.example.core.i18n.t
import com.example.core.ui.CardBlock
import com.example.core.ui.EsmeryTextField
import com.example.core.ui.PrimaryButton
import com.example.core.ui.ScreenList
import com.example.data.EsmeryState
import com.example.data.Moment
import com.example.data.PRESET_IMAGES
import com.example.ui.theme.Cocoa

@Composable
fun MomentsScreen(
  state: EsmeryState,
  viewModel: MomentsViewModel,
  onToast: (String) -> Unit,
  onNavigateToPlans: () -> Unit = {},
) {
  var showAdd by remember { mutableStateOf(false) }
  var showPremiumDialog by remember { mutableStateOf(false) }
  val sharedMessage = t("Moment shared.", "Đã chia sẻ khoảnh khắc.")
  ScreenList(title = appString(R.string.moments), subtitle = t("Small updates for the people who care.", "Những cập nhật nhỏ dành cho người quan tâm bạn.")) {
    item {
      PrimaryButton(text = appString(R.string.share_moment), icon = Icons.Rounded.Add) {
        if (viewModel.isPremium()) showAdd = true else showPremiumDialog = true
      }
    }
    items(state.moments) { moment ->
      MomentCard(moment)
    }
  }
  if (showAdd) {
    MomentDialog(
      onDismiss = { showAdd = false },
      onShare = { caption, image ->
        viewModel.onEvent(MomentsUiEvent.ShareMoment(caption, image))
        showAdd = false
        onToast(sharedMessage)
      },
      onShareCustomImage = { caption, bytes, fileName ->
        viewModel.onEvent(MomentsUiEvent.ShareMomentWithImage(caption, bytes, fileName))
        showAdd = false
        onToast(sharedMessage)
      },
    )
  }
  if (showPremiumDialog) {
    AlertDialog(
      onDismissRequest = { showPremiumDialog = false },
      title = { Text(t("Premium feature", "Tính năng Premium"), color = Cocoa, fontWeight = FontWeight.Black) },
      text = { Text(t("Upload your own photos with an Advanced plan.", "Tải ảnh của bạn với gói Nâng cao.")) },
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
private fun MomentCard(moment: Moment) {
  CardBlock {
    AsyncImage(
      model = moment.imageUrl,
      contentDescription = null,
      modifier = Modifier.fillMaxWidth().height(150.dp),
      contentScale = ContentScale.Crop,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Icon(Icons.Rounded.LocalFlorist, contentDescription = null, tint = Cocoa)
      Column {
        Text(localizedEventText(moment.caption), color = Cocoa, fontWeight = FontWeight.Black)
        Text(t("Shared to circle - ${friendlyTimeText(moment.createdAt)}", "Đã chia sẻ với vòng thân - ${friendlyTimeText(moment.createdAt)}"))
      }
    }
  }
}

@Composable
private fun MomentDialog(
  onDismiss: () -> Unit,
  onShare: (String, String) -> Unit,
  onShareCustomImage: (String, ByteArray, String) -> Unit,
) {
  var caption by remember { mutableStateOf("") }
  var image by remember { mutableStateOf(PRESET_IMAGES.first()) }
  val context = LocalContext.current
  val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    uri ?: return@rememberLauncherForActivityResult
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
    if (caption.isNotBlank()) {
      onShareCustomImage(caption, bytes, "moment-${System.currentTimeMillis()}.jpg")
    }
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(appString(R.string.share_moment), color = Cocoa, fontWeight = FontWeight.Black) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EsmeryTextField(caption, { caption = it }, t("Caption", "Chú thích"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          PRESET_IMAGES.forEachIndexed { index, url ->
            FilterChip(selected = image == url, onClick = { image = url }, label = { Text(t("Image ${index + 1}", "Ảnh ${index + 1}")) })
          }
        }
        Button(onClick = { galleryPicker.launch("image/*") }) {
          Text(t("Pick from gallery", "Chọn từ thư viện"))
        }
      }
    },
    confirmButton = { Button(onClick = { if (caption.isNotBlank()) onShare(caption, image) }) { Text(t("Share", "Chia sẻ")) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel", "Hủy")) } },
  )
}
