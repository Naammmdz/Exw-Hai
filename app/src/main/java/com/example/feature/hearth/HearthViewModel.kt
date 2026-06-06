package com.example.feature.hearth

import com.example.core.viewmodel.BaseEsmeryViewModel
import com.example.data.EsmeryRepository

sealed interface HearthUiEvent {
  data object CheckIn : HearthUiEvent
  data class MarkNotificationRead(val notificationId: String) : HearthUiEvent
}

class HearthViewModel(
  repository: EsmeryRepository? = null,
) : BaseEsmeryViewModel(repository ?: com.example.EsmeryServices.repository) {
  fun onEvent(event: HearthUiEvent) {
    when (event) {
      HearthUiEvent.CheckIn -> launchAction { repository.checkIn() }
      is HearthUiEvent.MarkNotificationRead -> launchAction {
        repository.markNotificationRead(event.notificationId)
      }
    }
  }
}
