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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.AuthGateway
import com.example.EsmeryServices
import com.example.R
import com.example.automation.EsmeryNotificationChannels
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.appString
import com.example.core.i18n.t
import com.example.core.viewmodel.EsmeryViewModelFactory
import com.example.data.EsmeryRepository
import com.example.feature.circle.CircleScreen
import com.example.feature.circle.CircleViewModel
import com.example.feature.crisis.CrisisScreen
import com.example.feature.crisis.CrisisViewModel
import com.example.feature.hearth.HearthScreen
import com.example.feature.hearth.HearthViewModel
import com.example.feature.moments.MomentsScreen
import com.example.feature.moments.MomentsViewModel
import com.example.feature.plans.PlansScreen
import com.example.feature.plans.PlansViewModel
import com.example.feature.profile.ProfileScreen
import com.example.feature.safety.SafetyScreen
import com.example.feature.safety.SafetyViewModel
import com.example.feature.timeline.TimelineScreen
import com.example.feature.timeline.TimelineViewModel
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
  Profile(R.string.profile, Icons.Rounded.Person),
}

@Composable
fun HomeScreen(
  repository: EsmeryRepository = EsmeryServices.repository,
  authGateway: AuthGateway = remember { AuthGateway() },
  language: AppLanguage = AppLanguage.English,
  onToggleLanguage: () -> Unit = {},
  onSignedOut: () -> Unit = {},
) {
  val hearthViewModel: HearthViewModel = viewModel(factory = EsmeryViewModelFactory { HearthViewModel(repository) })
  val circleViewModel: CircleViewModel = viewModel(factory = EsmeryViewModelFactory { CircleViewModel(repository) })
  val timelineViewModel: TimelineViewModel = viewModel(factory = EsmeryViewModelFactory { TimelineViewModel(repository) })
  val momentsViewModel: MomentsViewModel = viewModel(factory = EsmeryViewModelFactory { MomentsViewModel(repository) })
  val safetyViewModel: SafetyViewModel = viewModel(factory = EsmeryViewModelFactory { SafetyViewModel(repository) })
  val crisisViewModel: CrisisViewModel = viewModel(factory = EsmeryViewModelFactory { CrisisViewModel(repository) })
  val plansViewModel: PlansViewModel = viewModel(factory = EsmeryViewModelFactory { PlansViewModel(repository) })

  val state by hearthViewModel.esmeryState.collectAsState()
  var selectedTab by remember { mutableStateOf(MainTab.Hearth) }
  val scope = rememberCoroutineScope()
  var toast by remember { mutableStateOf<String?>(null) }
  val context = LocalContext.current
  val circleNotifiedToast = t("Your circle has been notified.", "Vòng thân của bạn đã được thông báo.")

  LaunchedEffect(toast) {
    if (toast != null) {
      delay(2200)
      toast = null
    }
  }

  val newNotificationTitle = t("New notification", "Thông báo mới")
  val newNotificationBody = t("You have a new safety update.", "Bạn có cập nhật an toàn mới.")

  LaunchedEffect(state.profile.id, newNotificationTitle, newNotificationBody) {
    repository.startNotificationRealtime {
      scope.launch { repository.refresh() }
      EsmeryNotificationChannels.showSafetyNotification(
        context,
        newNotificationTitle,
        newNotificationBody,
      )
    }
  }

  LaunchedEffect(state.profile.id, state.profile.lastSafeAt, state.safetySettings) {
    scope.launch { repository.evaluateMissedCheckIns() }
  }

  Scaffold(
    bottomBar = {
      Surface(color = Surface, shadowElevation = 8.dp) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceAround,
        ) {
          MainTab.entries.forEach { tab ->
            TabButton(
              tab = tab,
              selected = selectedTab == tab,
              onClick = {
                selectedTab = tab
                scope.launch { repository.refresh() }
              },
            )
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
              hearthViewModel.onEvent(com.example.feature.hearth.HearthUiEvent.CheckIn)
              toast = circleNotifiedToast
            }
          },
          language = language,
          onToggleLanguage = onToggleLanguage,
          onLogout = {
            scope.launch {
              repository.stopNotificationRealtime()
              authGateway.signOut()
              onSignedOut()
            }
          },
          onNotificationRead = { notificationId ->
            hearthViewModel.onEvent(com.example.feature.hearth.HearthUiEvent.MarkNotificationRead(notificationId))
          },
        )

        MainTab.Circle -> CircleScreen(state = state, viewModel = circleViewModel, onToast = { toast = it })
        MainTab.Timeline -> TimelineScreen(events = state.timelineEvents)
        MainTab.Moments -> MomentsScreen(state = state, viewModel = momentsViewModel, onToast = { toast = it })
        MainTab.Safety -> SafetyScreen(state = state, viewModel = safetyViewModel, onToast = { toast = it })
        MainTab.Crisis -> CrisisScreen(state = state, viewModel = crisisViewModel, onToast = { toast = it })
        MainTab.Plans -> PlansScreen(state = state, viewModel = plansViewModel, onToast = { toast = it })
        MainTab.Profile -> ProfileScreen(onAccountDeleted = {
          scope.launch {
            repository.stopNotificationRealtime()
            authGateway.signOut()
            onSignedOut()
          }
        })
      }

      toast?.let {
        Surface(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
          color = Cocoa,
          shape = RoundedCornerShape(8.dp),
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
      .width(44.dp)
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(tab.icon, contentDescription = null, tint = if (selected) Apricot else Taupe, modifier = Modifier.size(22.dp))
    Spacer(Modifier.size(4.dp))
    Text(appString(tab.labelRes), color = if (selected) Cocoa else Taupe, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
  }
}
