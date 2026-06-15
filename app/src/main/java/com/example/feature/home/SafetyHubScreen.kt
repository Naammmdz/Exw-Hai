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
import com.example.feature.crisis.CrisisScreen
import com.example.feature.crisis.CrisisViewModel
import com.example.feature.safety.SafetyScreen
import com.example.feature.safety.SafetyViewModel

@Composable
fun SafetyHubScreen(
  state: EsmeryState,
  safetyViewModel: SafetyViewModel,
  crisisViewModel: CrisisViewModel,
  onToast: (String) -> Unit,
  onNavigateToPlans: () -> Unit,
) {
  var selectedSection by remember { mutableIntStateOf(0) }
  val sections = listOf(
    HubSection(appString(R.string.safety_rhythm)),
    HubSection(appString(R.string.crisis)),
  )

  HubScreen(
    sections = sections,
    selectedIndex = selectedSection,
    onSelect = { selectedSection = it },
  ) {
    when (selectedSection) {
      0 -> SafetyScreen(
        state = state,
        viewModel = safetyViewModel,
        onToast = onToast,
        onNavigateToPlans = onNavigateToPlans,
      )
      1 -> CrisisScreen(
        state = state,
        viewModel = crisisViewModel,
        onToast = onToast,
        onNavigateToPlans = onNavigateToPlans,
      )
    }
  }
}
