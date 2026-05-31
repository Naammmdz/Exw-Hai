package com.example.data

import kotlinx.coroutines.flow.StateFlow

interface EsmeryRepository {
  val state: StateFlow<EsmeryState>

  suspend fun loadForUser(userId: String, email: String?, displayName: String?)
  suspend fun refresh()
  suspend fun clearLocalSession()
  suspend fun checkIn(note: String? = null): CheckIn
  suspend fun addFriendRequest(contact: String, name: String, relationship: String): FriendRequest
  suspend fun updateFriendRequest(requestId: String, status: CircleStatus)
  suspend fun sendNudge(memberId: String): TimelineEvent
  suspend fun shareMoment(caption: String, imageUrl: String): Moment
  suspend fun markNotificationRead(notificationId: String)
  suspend fun saveEmergencyContact(contact: EmergencyContact): EmergencyContact
  suspend fun deleteEmergencyContact(contactId: String)
  suspend fun toggleEmergencyContactVerified(contactId: String): EmergencyContact?
  suspend fun toggleEmergencyContactAutoNotify(contactId: String): EmergencyContact?
  suspend fun saveSafetyRhythm(rhythm: SafetyRhythm): SafetyRhythm
  suspend fun deleteSafetyRhythm(rhythmId: String)
  suspend fun toggleSafetyRhythm(rhythmId: String): SafetyRhythm?
  suspend fun updateSafetySettings(settings: SafetySettings): SafetySettings
  suspend fun evaluateMissedCheckIns(): TimelineEvent?
  suspend fun triggerEmergencyAlert(): TimelineEvent
  suspend fun updateSubscription(plan: SubscriptionPlan): SubscriptionStatus
}
