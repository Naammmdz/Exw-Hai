package com.example.feature.circle

import com.example.core.viewmodel.BaseEsmeryViewModel
import com.example.data.CircleStatus
import com.example.data.EsmeryRepository

sealed interface CircleUiEvent {
  data object Refresh : CircleUiEvent
  data class AddFriend(val contact: String, val name: String, val relationship: String) : CircleUiEvent
  data class UpdateFriendRequest(val requestId: String, val status: CircleStatus) : CircleUiEvent
  data class SendNudge(val memberId: String) : CircleUiEvent
}

class CircleViewModel(
  repository: EsmeryRepository? = null,
) : BaseEsmeryViewModel(repository ?: com.example.EsmeryServices.repository) {
  fun onEvent(event: CircleUiEvent) {
    when (event) {
      CircleUiEvent.Refresh -> launchAction { repository.refresh() }
      is CircleUiEvent.AddFriend -> launchAction {
        repository.addFriendRequest(event.contact, event.name, event.relationship)
      }
      is CircleUiEvent.UpdateFriendRequest -> launchAction {
        repository.updateFriendRequest(event.requestId, event.status)
      }
      is CircleUiEvent.SendNudge -> launchAction { repository.sendNudge(event.memberId) }
    }
  }
}
