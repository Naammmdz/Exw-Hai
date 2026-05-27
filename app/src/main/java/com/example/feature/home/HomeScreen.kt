package com.example.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.AuthGateway
import com.example.EsmeryServices
import com.example.R
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.appString
import com.example.core.i18n.t
import com.example.data.EsmeryRepository
import com.example.data.InMemoryEsmeryRepository
import com.example.feature.circle.CircleScreen
import com.example.feature.crisis.CrisisScreen
import com.example.feature.hearth.HearthScreen
import com.example.feature.moments.MomentsScreen
import com.example.feature.plans.PlansScreen
import com.example.feature.safety.SafetyScreen
import com.example.feature.timeline.TimelineScreen
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Cream
import com.example.ui.theme.Surface
import com.example.ui.theme.Taupe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class MainTab(val labelRes: Int, val icon: ImageVector) {
  Hearth(R.string.hearth, Icons.Rounded.Home),
  Circle(R.string.circle, Icons.Rounded.Group),
  Timeline(R.string.timeline, Icons.Rounded.Schedule),
  Moments(R.string.moments, Icons.Rounded.LocalFlorist),
  Safety(R.string.safety, Icons.Rounded.Security),
  Crisis(R.string.crisis, Icons.Rounded.Warning),
  Plans(R.string.plans, Icons.Rounded.CreditCard),
}

@Composable
fun HomeScreen(
  repository: EsmeryRepository = EsmeryServices.repository,
  authGateway: AuthGateway = remember { AuthGateway(InMemoryEsmeryRepository()) },
  language: AppLanguage = AppLanguage.English,
  onToggleLanguage: () -> Unit = {},
  onSignedOut: () -> Unit = {},
) {
  val state by repository.state.collectAsState()
  var selectedTab by remember { mutableStateOf(MainTab.Hearth) }
  val scope = rememberCoroutineScope()
  var toast by remember { mutableStateOf<String?>(null) }
  val circleNotifiedToast = t("Your circle has been notified.", "Vòng thân của bạn đã được thông báo.")

  LaunchedEffect(toast) {
    if (toast != null) {
      delay(2200)
      toast = null
    }
  }

  Scaffold(
    bottomBar = {
      Surface(color = Surface, shadowElevation = 8.dp) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceAround,
        ) {
          MainTab.entries.forEach { tab ->
            TabButton(tab = tab, selected = selectedTab == tab, onClick = { selectedTab = tab })
          }
        }
      }
    },
    containerColor = Cream,
  ) { padding ->
    Box(
      modifier = Modifier
        .padding(padding)
        .fillMaxSize()
        .background(Cream),
    ) {
      when (selectedTab) {
        MainTab.Hearth -> HearthScreen(
          state = state,
          onCheckIn = {
            scope.launch {
              repository.checkIn()
              toast = circleNotifiedToast
            }
          },
          language = language,
          onToggleLanguage = onToggleLanguage,
          onLogout = {
            scope.launch {
              authGateway.signOut()
              onSignedOut()
            }
          },
        )

        MainTab.Circle -> CircleScreen(state, repository, onToast = { toast = it })
        MainTab.Timeline -> TimelineScreen(state.timelineEvents)
        MainTab.Moments -> MomentsScreen(state.moments, repository, onToast = { toast = it })
        MainTab.Safety -> SafetyScreen(state, repository, onToast = { toast = it })
        MainTab.Crisis -> CrisisScreen(state, repository, onToast = { toast = it })
        MainTab.Plans -> PlansScreen(state, repository, onToast = { toast = it })
      }

      toast?.let {
        Surface(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
          color = Cocoa,
          shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
          shadowElevation = 8.dp,
        ) {
          Text(it, color = Cream, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun TabButton(tab: MainTab, selected: Boolean, onClick: () -> Unit) {
  Column(
    modifier = Modifier
      .width(52.dp)
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(tab.icon, contentDescription = null, tint = if (selected) Apricot else Taupe, modifier = Modifier.size(24.dp))
    Spacer(Modifier.size(4.dp))
    Text(appString(tab.labelRes), color = if (selected) Cocoa else Taupe, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
  }
}
