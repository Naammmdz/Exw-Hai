package com.example.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.appString
import com.example.core.i18n.t
import com.example.core.ui.PrimaryButton
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Cream
import com.example.ui.theme.Surface
import com.example.ui.theme.Taupe

@Composable
fun OnboardingPagerScreen(onDone: () -> Unit = {}) {
  val pages = listOf(
    appString(R.string.onboarding_one) to t("Stay connected without feeling watched.", "Giữ kết nối mà không có cảm giác bị theo dõi."),
    appString(R.string.onboarding_two) to t("Tap once to tell your trusted people you are safe.", "Chạm một lần để báo cho người tin cậy rằng bạn vẫn an toàn."),
    appString(R.string.onboarding_three) to t("Share gentle moments, nudges, and calm safety signals.", "Chia sẻ khoảnh khắc, nhắc nhở nhẹ nhàng và tín hiệu an toàn bình yên."),
  )
  var page by remember { mutableStateOf(0) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Cream)
      .padding(24.dp),
    verticalArrangement = Arrangement.SpaceBetween,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(Modifier.height(24.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(8.dp)) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Icon(Icons.Rounded.Security, contentDescription = null, tint = Apricot, modifier = Modifier.size(72.dp))
        Text(pages[page].first, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Black, color = Cocoa)
        Text(pages[page].second, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Taupe)
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      pages.indices.forEach { index ->
        Surface(
          modifier = Modifier.size(width = if (index == page) 28.dp else 10.dp, height = 10.dp),
          shape = CircleShape,
          color = if (index == page) Apricot else Taupe.copy(alpha = 0.25f),
          content = {},
        )
      }
    }
    PrimaryButton(text = if (page == pages.lastIndex) appString(R.string.get_started) else t("Next", "Tiếp theo")) {
      if (page == pages.lastIndex) onDone() else page += 1
    }
  }
}

@Composable
fun SetupScreen(title: String, body: String, primary: String, onPrimary: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Cream)
      .padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(Icons.Rounded.Group, contentDescription = null, tint = Apricot, modifier = Modifier.size(72.dp))
    Spacer(Modifier.height(20.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Cocoa, textAlign = TextAlign.Center)
    Spacer(Modifier.height(10.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = Taupe, textAlign = TextAlign.Center)
    Spacer(Modifier.height(28.dp))
    PrimaryButton(text = primary, onClick = onPrimary)
  }
}
