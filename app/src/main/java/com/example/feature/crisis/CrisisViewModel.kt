package com.example.feature.crisis

import com.example.core.viewmodel.BaseEsmeryViewModel
import com.example.data.EmergencyContact
import com.example.data.EsmeryRepository

sealed interface CrisisUiEvent {
  data object TriggerEmergency : CrisisUiEvent
  data class ResolveIncident(val incidentId: String) : CrisisUiEvent
  data class SaveContact(val contact: EmergencyContact) : CrisisUiEvent
  data class DeleteContact(val contactId: String) : CrisisUiEvent
  data class ToggleVerified(val contactId: String) : CrisisUiEvent
  data class ToggleAutoNotify(val contactId: String) : CrisisUiEvent
  data class ShareLocation(val latitude: Double, val longitude: Double, val accuracy: Double?) : CrisisUiEvent
}

class CrisisViewModel(
  repository: EsmeryRepository? = null,
) : BaseEsmeryViewModel(repository ?: com.example.EsmeryServices.repository) {
  fun onEvent(event: CrisisUiEvent) {
    when (event) {
      CrisisUiEvent.TriggerEmergency -> launchAction { repository.triggerEmergencyAlert() }
      is CrisisUiEvent.ResolveIncident -> launchAction { repository.resolveAlertIncident(event.incidentId) }
      is CrisisUiEvent.SaveContact -> launchAction { repository.saveEmergencyContact(event.contact) }
      is CrisisUiEvent.DeleteContact -> launchAction { repository.deleteEmergencyContact(event.contactId) }
      is CrisisUiEvent.ToggleVerified -> launchAction { repository.toggleEmergencyContactVerified(event.contactId) }
      is CrisisUiEvent.ToggleAutoNotify -> launchAction { repository.toggleEmergencyContactAutoNotify(event.contactId) }
      is CrisisUiEvent.ShareLocation -> launchAction {
        repository.shareEmergencyLocation(event.latitude, event.longitude, event.accuracy)
      }
    }
  }
}
