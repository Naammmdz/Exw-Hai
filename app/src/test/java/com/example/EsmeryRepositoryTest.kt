package com.example

import com.example.data.CircleStatus
import com.example.data.EmergencyContact
import com.example.data.EsmeryRemoteDataSource
import com.example.data.EsmeryState
import com.example.data.InMemoryEsmeryRepository
import com.example.data.NotificationType
import com.example.data.Profile
import com.example.data.ResilientEsmeryRepository
import com.example.data.SafetyRhythm
import com.example.data.SafetySettings
import com.example.data.SubscriptionPlan
import com.example.data.CheckIn
import com.example.data.CircleMember
import com.example.data.EsmeryNotification
import com.example.data.FriendRequest
import com.example.data.Moment
import com.example.data.SubscriptionStatus
import com.example.data.TimelineEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EsmeryRepositoryTest {
  @Test
  fun checkInAddsTimelineEventAndUpdatesLastSafeAt() = runTest {
    val repository = InMemoryEsmeryRepository()
    val before = repository.state.value.timelineEvents.size

    repository.checkIn()

    val state = repository.state.value
    assertEquals(before + 1, state.timelineEvents.size)
    assertEquals(NotificationType.CheckInSuccess, state.notifications.first().type)
    assertEquals("safe", state.checkIns.first().status)
    assertTrue(state.profile.lastSafeAt != null)
  }

  @Test
  fun addFriendRequestCreatesPendingMember() = runTest {
    val repository = InMemoryEsmeryRepository()

    repository.addFriendRequest("friend@example.com", "Friend", "Sibling")

    val state = repository.state.value
    assertEquals("friend@example.com", state.friendRequests.first().receiverContact)
    assertEquals(CircleStatus.Pending, state.circleMembers.first().status)
  }

  @Test
  fun shareMomentAndSaveRhythmAppendState() = runTest {
    val repository = InMemoryEsmeryRepository()

    repository.shareMoment("Coffee break", "image")
    repository.saveSafetyRhythm(SafetyRhythm(id = "", userId = "local-user", label = "Midday", checkTime = "13:00"))

    val state = repository.state.value
    assertEquals("Coffee break", state.moments.first().caption)
    assertTrue(state.safetyRhythms.any { it.label == "Midday" })
  }

  @Test
  fun selectingPremiumPlanUpdatesProfile() = runTest {
    val repository = InMemoryEsmeryRepository()

    repository.updateSubscription(SubscriptionPlan.Yearly)

    val state = repository.state.value
    assertEquals(SubscriptionPlan.Yearly, state.subscriptionStatus.plan)
    assertTrue(state.profile.isPremium)
  }

  @Test
  fun nudgeAndEmergencyAlertCreateTimelineAndNotifications() = runTest {
    val repository = InMemoryEsmeryRepository()
    val memberId = repository.state.value.circleMembers.first().id

    repository.sendNudge(memberId)
    repository.triggerEmergencyAlert()

    val notifications = repository.state.value.notifications
    assertEquals(NotificationType.EmergencyAlert, notifications.first().type)
    assertTrue(notifications.any { it.type == NotificationType.GentleNudge })
  }

  @Test
  fun missedCheckInEvaluationCreatesSingleNotification() = runTest {
    val repository = InMemoryEsmeryRepository()

    val event = repository.evaluateMissedCheckIns()
    val duplicate = repository.evaluateMissedCheckIns()

    assertNotNull(event)
    assertEquals(null, duplicate)
    assertTrue(repository.state.value.notifications.any { it.type == NotificationType.MissedCheckIn })
  }

  @Test
  fun safetySettingsAndEmergencyContactActionsUpdateState() = runTest {
    val repository = InMemoryEsmeryRepository()
    val settings = repository.updateSafetySettings(SafetySettings(userId = "local-user", inactivityHours = 12, escalationDelayMinutes = 60))
    val contact = repository.saveEmergencyContact(EmergencyContact(id = "", userId = "local-user", name = "Aunt", contact = "+84123"))

    repository.toggleEmergencyContactVerified(contact.id)
    repository.toggleEmergencyContactAutoNotify(contact.id)
    repository.deleteEmergencyContact(contact.id)

    assertEquals(12, settings.inactivityHours)
    assertFalse(repository.state.value.emergencyContacts.any { it.id == contact.id })
  }

  @Test
  fun resilientRepositoryLoadsRemoteStateWhenAvailable() = runTest {
    val remoteState = remoteState()
    val remote = FakeRemote(remoteState)
    val repository = ResilientEsmeryRepository(remote = remote)

    repository.loadForUser("remote-user", "remote@example.com", "Remote")

    assertEquals("Remote Profile", repository.state.value.profile.displayName)
    assertEquals("Remote moment", repository.state.value.moments.first().caption)
  }

  @Test
  fun resilientRepositoryWritesNotificationAfterCheckIn() = runTest {
    val remote = FakeRemote(null)
    val repository = ResilientEsmeryRepository(remote = remote)

    repository.loadForUser("remote-user", "remote@example.com", "Remote")
    repository.checkIn()

    assertEquals(NotificationType.CheckInSuccess, remote.notifications.first().type)
    assertTrue(remote.checkIns.isNotEmpty())
  }

  @Test
  fun acceptingReceivedRequestCreatesReciprocalCircleMember() = runTest {
    val request = FriendRequest(
      id = "request-a-to-b",
      senderUserId = "account-a",
      receiverContact = "b@example.com",
      createdAt = "2026-05-31T10:00:00",
    )
    val bState = emptyStateFor("account-b", "b@example.com", "Account B").copy(friendRequests = listOf(request))
    val remote = FakeRemote(
      remoteState = bState,
      profilesById = mapOf("account-a" to Profile(id = "account-a", displayName = "Account A", email = "a@example.com")),
    )
    val repository = ResilientEsmeryRepository(remote = remote)

    repository.loadForUser("account-b", "b@example.com", "Account B")
    repository.updateFriendRequest(request.id, CircleStatus.Accepted)

    val member = repository.state.value.circleMembers.first()
    assertEquals("account-a", member.memberUserId)
    assertEquals(CircleStatus.Accepted, member.status)
    assertTrue(remote.circleMembers.any { it.ownerUserId == "account-b" && it.memberUserId == "account-a" })
    assertEquals("account-b", remote.updatedCircleMemberUserId)
  }

  @Test
  fun resilientRepositoryStartsRealUserWithEmptyCircleWhenRemoteHasNoData() = runTest {
    val remote = FakeRemote(null)
    val repository = ResilientEsmeryRepository(remote = remote)

    repository.loadForUser("real-user", "real@example.com", "Real User")

    val state = repository.state.value
    assertEquals("Real User", state.profile.displayName)
    assertTrue(state.circleMembers.isEmpty())
    assertTrue(state.friendRequests.isEmpty())
    assertTrue(state.moments.isEmpty())
    assertTrue(state.emergencyContacts.isEmpty())
  }

  private fun emptyStateFor(userId: String, email: String, displayName: String): EsmeryState = EsmeryState(
    profile = Profile(id = userId, displayName = displayName, email = email),
    circleMembers = emptyList(),
    friendRequests = emptyList(),
    checkIns = emptyList(),
    timelineEvents = emptyList(),
    notifications = emptyList(),
    moments = emptyList(),
    emergencyContacts = emptyList(),
    safetyRhythms = emptyList(),
    safetySettings = SafetySettings(userId = userId),
    subscriptionStatus = SubscriptionStatus(userId = userId),
  )

  private suspend fun remoteState(): EsmeryState {
    val repository = InMemoryEsmeryRepository()
    repository.loadForUser("remote-user", "remote@example.com", "Remote Profile")
    repository.shareMoment("Remote moment", "image")
    return repository.state.value.copy(profile = repository.state.value.profile.copy(displayName = "Remote Profile"))
  }
}

private class FakeRemote(
  private val remoteState: EsmeryState?,
  private val profilesById: Map<String, Profile> = emptyMap(),
) : EsmeryRemoteDataSource {
  val notifications = mutableListOf<EsmeryNotification>()
  val checkIns = mutableListOf<CheckIn>()
  val circleMembers = mutableListOf<CircleMember>()
  var updatedCircleMemberUserId: String? = null

  override suspend fun fetchState(userId: String, email: String?, displayName: String?): EsmeryState? = remoteState
  override suspend fun findProfileByContact(contact: String): Profile? = null
  override suspend fun findProfileById(userId: String): Profile? = profilesById[userId]
  override suspend fun upsertProfile(profile: Profile) = Unit
  override suspend fun updateProfileLastSafeAt(userId: String, lastSafeAt: String?) = Unit
  override suspend fun upsertCircleMember(member: CircleMember) {
    circleMembers += member
  }
  override suspend fun updateCircleMemberStatus(memberId: String, status: CircleStatus, memberUserId: String?) {
    updatedCircleMemberUserId = memberUserId
  }
  override suspend fun insertFriendRequest(request: FriendRequest) = Unit
  override suspend fun updateFriendRequestStatus(requestId: String, status: CircleStatus) = Unit
  override suspend fun insertCheckIn(checkIn: CheckIn) {
    checkIns += checkIn
  }
  override suspend fun insertTimelineEvent(event: TimelineEvent) = Unit
  override suspend fun insertNotification(notification: EsmeryNotification) {
    notifications += notification
  }
  override suspend fun markNotificationRead(notificationId: String) = Unit
  override suspend fun insertMoment(moment: Moment) = Unit
  override suspend fun upsertEmergencyContact(contact: EmergencyContact) = Unit
  override suspend fun deleteEmergencyContact(contactId: String) = Unit
  override suspend fun upsertSafetyRhythm(rhythm: SafetyRhythm) = Unit
  override suspend fun deleteSafetyRhythm(rhythmId: String) = Unit
  override suspend fun upsertSafetySettings(settings: SafetySettings) = Unit
  override suspend fun upsertSubscription(subscription: SubscriptionStatus) = Unit
}
