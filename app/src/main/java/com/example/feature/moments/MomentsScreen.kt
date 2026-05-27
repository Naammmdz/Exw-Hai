package com.example.feature.moments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.appString
import com.example.core.i18n.friendlyTimeText
import com.example.core.i18n.localizedEventText
import com.example.core.i18n.t
import com.example.core.ui.EsmeryTextField
import com.example.core.ui.InfoCard
import com.example.core.ui.PrimaryButton
import com.example.core.ui.ScreenList
import com.example.data.EsmeryRepository
import com.example.data.Moment
import com.example.data.PRESET_IMAGES
import com.example.ui.theme.Cocoa
import kotlinx.coroutines.launch

@Composable
fun MomentsScreen(moments: List<Moment>, repository: EsmeryRepository, onToast: (String) -> Unit) {
  var showAdd by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val sharedMessage = t("Moment shared.", "Đã chia sẻ khoảnh khắc.")
  ScreenList(title = appString(R.string.moments), subtitle = t("Small updates for the people who care.", "Những cập nhật nhỏ dành cho người quan tâm bạn.")) {
    item { PrimaryButton(text = appString(R.string.share_moment), icon = Icons.Rounded.Add) { showAdd = true } }
    items(moments) { moment ->
      InfoCard(
        icon = Icons.Rounded.LocalFlorist,
        title = localizedEventText(moment.caption),
        body = t("Shared to circle - ${friendlyTimeText(moment.createdAt)}", "Đã chia sẻ với vòng thân - ${friendlyTimeText(moment.createdAt)}"),
      )
    }
  }
  if (showAdd) {
    MomentDialog(onDismiss = { showAdd = false }, onShare = { caption, image ->
      scope.launch {
        repository.shareMoment(caption, image)
        showAdd = false
        onToast(sharedMessage)
      }
    })
  }
}

@Composable
private fun MomentDialog(onDismiss: () -> Unit, onShare: (String, String) -> Unit) {
  var caption by remember { mutableStateOf("") }
  var image by remember { mutableStateOf(PRESET_IMAGES.first()) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(appString(R.string.share_moment), color = Cocoa, fontWeight = androidx.compose.ui.text.font.FontWeight.Black) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EsmeryTextField(caption, { caption = it }, t("Caption", "Chú thích"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          PRESET_IMAGES.forEachIndexed { index, url ->
            FilterChip(selected = image == url, onClick = { image = url }, label = { Text(t("Image ${index + 1}", "Ảnh ${index + 1}")) })
          }
        }
      }
    },
    confirmButton = { Button(onClick = { if (caption.isNotBlank()) onShare(caption, image) }) { Text(t("Share", "Chia sẻ")) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel", "Hủy")) } },
  )
}
