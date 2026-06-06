package com.example.feature.safety

import com.example.core.viewmodel.BaseEsmeryViewModel
import com.example.data.EsmeryRepository
import com.example.data.SafetyRhythm
import com.example.data.SafetySettings

sealed interface SafetyUiEvent {
  data class SaveRhythm(val rhythm: SafetyRhythm) : SafetyUiEvent
  data class DeleteRhythm(val rhythmId: String) : SafetyUiEvent
  data class ToggleRhythm(val rhythmId: String) : SafetyUiEvent
  data class SaveSettings(val settings: SafetySettings) : SafetyUiEvent
}

class SafetyViewModel(
  repository: EsmeryRepository? = null,
) : BaseEsmeryViewModel(repository ?: com.example.EsmeryServices.repository) {
  fun onEvent(event: SafetyUiEvent) {
    when (event) {
      is SafetyUiEvent.SaveRhythm -> launchAction { repository.saveSafetyRhythm(event.rhythm) }
      is SafetyUiEvent.DeleteRhythm -> launchAction { repository.deleteSafetyRhythm(event.rhythmId) }
      is SafetyUiEvent.ToggleRhythm -> launchAction { repository.toggleSafetyRhythm(event.rhythmId) }
      is SafetyUiEvent.SaveSettings -> launchAction { repository.updateSafetySettings(event.settings) }
    }
  }
}
