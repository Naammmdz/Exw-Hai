package com.example.feature.moments

import com.example.core.entitlement.EntitlementGate
import com.example.core.viewmodel.BaseEsmeryViewModel
import com.example.data.EsmeryRepository

sealed interface MomentsUiEvent {
  data class ShareMoment(val caption: String, val imageUrl: String) : MomentsUiEvent
  data class ShareMomentWithImage(val caption: String, val imageBytes: ByteArray, val fileName: String) : MomentsUiEvent
}

class MomentsViewModel(
  repository: EsmeryRepository? = null,
) : BaseEsmeryViewModel(repository ?: com.example.EsmeryServices.repository) {
  fun onEvent(event: MomentsUiEvent) {
    when (event) {
      is MomentsUiEvent.ShareMoment -> launchAction {
        repository.shareMoment(event.caption, event.imageUrl)
      }
      is MomentsUiEvent.ShareMomentWithImage -> launchAction {
        val imageUrl = repository.uploadMomentImage(event.imageBytes, event.fileName)
        repository.shareMoment(event.caption, imageUrl)
      }
    }
  }

  fun isPremium(): Boolean = EntitlementGate.isPremiumActive(esmeryState.value)
}
