package com.example.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.R
import com.example.core.i18n.appString
import com.example.core.ui.HubSection
import com.example.core.ui.HubScreen
import com.example.data.EsmeryState
import com.example.feature.moments.MomentsScreen
import com.example.feature.moments.MomentsViewModel
import com.example.feature.timeline.TimelineScreen
import com.example.feature.timeline.TimelineViewModel

@Composable
fun MemoriesHubScreen(
  state: EsmeryState,
  timelineViewModel: TimelineViewModel,
  momentsViewModel: MomentsViewModel,
  onToast: (String) -> Unit,
  onNavigateToPlans: () -> Unit,
) {
  var selectedSection by remember { mutableIntStateOf(0) }
  val sections = listOf(
    HubSection(appString(R.string.timeline)),
    HubSection(appString(R.string.moments)),
  )

  HubScreen(
    sections = sections,
    selectedIndex = selectedSection,
    onSelect = { selectedSection = it },
  ) {
    when (selectedSection) {
      0 -> TimelineScreen(viewModel = timelineViewModel)
      1 -> MomentsScreen(
        state = state,
        viewModel = momentsViewModel,
        onToast = onToast,
        onNavigateToPlans = onNavigateToPlans,
      )
    }
  }
}
