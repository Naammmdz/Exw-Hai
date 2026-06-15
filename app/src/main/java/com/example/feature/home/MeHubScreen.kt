package com.example.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.R
import com.example.core.i18n.appString
import com.example.core.ui.HubSection
import com.example.core.ui.HubScreen
import com.example.data.EsmeryState
import com.example.feature.plans.PlansScreen
import com.example.feature.plans.PlansViewModel
import com.example.feature.profile.ProfileScreen

enum class MeSection { Profile, Plans }

@Composable
fun MeHubScreen(
  state: EsmeryState,
  plansViewModel: PlansViewModel,
  selectedSection: MeSection,
  onSectionChange: (MeSection) -> Unit,
  onToast: (String) -> Unit,
  onAccountDeleted: () -> Unit,
) {
  var sectionIndex by remember { mutableIntStateOf(selectedSection.ordinal) }
  val sections = listOf(
    HubSection(appString(R.string.profile)),
    HubSection(appString(R.string.plans)),
  )

  LaunchedEffect(selectedSection) {
    sectionIndex = selectedSection.ordinal
  }

  HubScreen(
    sections = sections,
    selectedIndex = sectionIndex,
    onSelect = { index ->
      sectionIndex = index
      onSectionChange(MeSection.entries[index])
    },
  ) {
    when (MeSection.entries[sectionIndex]) {
      MeSection.Profile -> ProfileScreen(onAccountDeleted = onAccountDeleted)
      MeSection.Plans -> PlansScreen(state = state, viewModel = plansViewModel, onToast = onToast)
    }
  }
}
