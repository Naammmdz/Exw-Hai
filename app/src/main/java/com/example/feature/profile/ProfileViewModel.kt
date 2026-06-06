package com.example.feature.profile

import com.example.core.viewmodel.BaseEsmeryViewModel
import com.example.data.EsmeryRepository

sealed interface ProfileUiEvent {
  data class UpdateProfile(val displayName: String, val avatarUrl: String?) : ProfileUiEvent
  data class UploadAvatar(val imageBytes: ByteArray, val fileName: String) : ProfileUiEvent
  data class ChangePassword(val newPassword: String) : ProfileUiEvent
  data object DeleteAccount : ProfileUiEvent
}

class ProfileViewModel(
  repository: EsmeryRepository? = null,
) : BaseEsmeryViewModel(repository ?: com.example.EsmeryServices.repository) {
  fun onEvent(event: ProfileUiEvent) {
    when (event) {
      is ProfileUiEvent.UpdateProfile -> launchAction {
        repository.updateProfile(event.displayName, event.avatarUrl)
      }
      is ProfileUiEvent.UploadAvatar -> launchAction {
        val avatarUrl = repository.uploadAvatarImage(event.imageBytes, event.fileName)
        repository.updateProfile(esmeryState.value.profile.displayName, avatarUrl)
      }
      is ProfileUiEvent.ChangePassword -> launchAction {
        repository.changePassword(event.newPassword)
      }
      ProfileUiEvent.DeleteAccount -> launchAction {
        repository.deleteAccount()
      }
    }
  }
}
