package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.runtime.Composable
import com.example.core.i18n.AppLanguage
import com.example.data.InMemoryEsmeryRepository
import com.example.feature.circle.CircleScreen
import com.example.feature.circle.CircleViewModel
import com.example.feature.crisis.CrisisScreen
import com.example.feature.crisis.CrisisViewModel
import com.example.feature.hearth.HearthScreen
import com.example.feature.home.MeHubScreen
import com.example.feature.home.MeSection
import com.example.feature.home.MemoriesHubScreen
import com.example.feature.home.SafetyHubScreen
import com.example.feature.moments.MomentsScreen
import com.example.feature.moments.MomentsViewModel
import com.example.feature.plans.PlansScreen
import com.example.feature.plans.PlansViewModel
import com.example.feature.timeline.TimelineContent
import com.example.feature.safety.SafetyScreen
import com.example.feature.safety.SafetyViewModel
import com.example.feature.timeline.TimelineViewModel
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EsmeryFeatureSmokeTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun hearthRendersNotificationFeed() {
    val repository = InMemoryEsmeryRepository()

    composeTestRule.setContent {
      MyApplicationTheme {
        HearthScreen(
          state = repository.state.value,
          onCheckIn = {},
          language = AppLanguage.English,
          onToggleLanguage = {},
          onLogout = {},
          onNotificationRead = {},
        )
      }
    }

    composeTestRule.onRoot().fetchSemanticsNode()
  }

  @Test
  fun circleRenders() {
    val repository = InMemoryEsmeryRepository()
    render { CircleScreen(repository.state.value, CircleViewModel(repository), onToast = {}) }
  }

  @Test
  fun momentsRenders() {
    val repository = InMemoryEsmeryRepository()
    render { MomentsScreen(repository.state.value, MomentsViewModel(repository), onToast = {}) }
  }

  @Test
  fun safetyRenders() {
    val repository = InMemoryEsmeryRepository()
    render { SafetyScreen(repository.state.value, SafetyViewModel(repository), onToast = {}) }
  }

  @Test
  fun crisisRenders() {
    val repository = InMemoryEsmeryRepository()
    render { CrisisScreen(repository.state.value, CrisisViewModel(repository), onToast = {}) }
  }

  @Test
  fun plansRenders() {
    val repository = InMemoryEsmeryRepository()
    render { PlansScreen(repository.state.value, PlansViewModel(repository), onToast = {}) }
  }

  @Test
  fun timelineRendersEmptyState() {
    val repository = InMemoryEsmeryRepository()
    render { TimelineContent(events = repository.state.value.timelineEvents) }
  }

  @Test
  fun memoriesHubRenders() {
    val repository = InMemoryEsmeryRepository()
    render {
      MemoriesHubScreen(
        state = repository.state.value,
        timelineViewModel = TimelineViewModel(repository),
        momentsViewModel = MomentsViewModel(repository),
        onToast = {},
        onNavigateToPlans = {},
      )
    }
  }

  @Test
  fun safetyHubRenders() {
    val repository = InMemoryEsmeryRepository()
    render {
      SafetyHubScreen(
        state = repository.state.value,
        safetyViewModel = SafetyViewModel(repository),
        crisisViewModel = CrisisViewModel(repository),
        onToast = {},
        onNavigateToPlans = {},
      )
    }
  }

  @Test
  fun meHubRenders() {
    val repository = InMemoryEsmeryRepository()
    render {
      MeHubScreen(
        state = repository.state.value,
        plansViewModel = PlansViewModel(repository),
        selectedSection = MeSection.Plans,
        onSectionChange = {},
        onToast = {},
        onAccountDeleted = {},
      )
    }
  }

  private fun render(content: @Composable () -> Unit) {
    composeTestRule.setContent { MyApplicationTheme { content() } }
    composeTestRule.onRoot().fetchSemanticsNode()
  }
}
