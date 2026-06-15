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
import com.example.data.NotificationDelivery
import com.example.data.AlertIncident
import com.example.data.AlertJob
import com.example.data.LocationShare
import com.example.data.PaymentOrder
import com.example.data.PaymentProvider
import com.example.data.Entitlement
import com.example.data.DeviceToken
import com.example.data.SubscriptionStatus
import com.example.data.TimelineEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EsmeryRepositoryTest {
  private suspend fun demoRepository(): InMemoryEsmeryRepository =
    InMemoryEsmeryRepository().also { it.loadForUser("demo", "alex@example.com", "Alex Rivers") }

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
  fun addFriendRequestDoesNotDuplicateSameContactLocally() = runTest {
    val repository = InMemoryEsmeryRepository()

    val first = repository.addFriendRequest("friend@example.com", "Friend", "Sibling")
    val second = repository.addFriendRequest("FRIEND@example.com", "Friend Again", "Sibling")

    assertEquals(first.id, second.id)
    assertEquals(1, repository.state.value.friendRequests.count { it.receiverContact == "friend@example.com" })
    assertEquals(1, repository.state.value.circleMembers.count { it.invitedContact == "friend@example.com" })
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
    val repository = demoRepository()
    val memberId = repository.state.value.circleMembers.first().id

    repository.sendNudge(memberId)
    repository.triggerEmergencyAlert()

    val notifications = repository.state.value.notifications
    assertEquals(NotificationType.EmergencyAlert, notifications.first().type)
    assertTrue(notifications.any { it.type == NotificationType.GentleNudge })
  }

  @Test
  fun missedCheckInEvaluationCreatesSingleNotification() = runTest {
    val repository = demoRepository()

    val event = repository.evaluateMissedCheckIns()
    val duplicate = repository.evaluateMissedCheckIns()

    assertNotNull(event)
    assertEquals(null, duplicate)
    assertTrue(repository.state.value.notifications.any { it.type == NotificationType.MissedCheckIn })
  }

  @Test
  fun missedCheckInEvaluationCreatesIncidentJobAndDelivery() = runTest {
    val repository = demoRepository()

    repository.evaluateMissedCheckIns()

    val state = repository.state.value
    assertTrue(state.alertIncidents.any { it.reason == "missed_check_in" })
    assertTrue(state.alertJobs.isNotEmpty())
    assertTrue(state.notificationDeliveries.any { it.notificationId == state.notifications.first().id })
  }

  @Test
  fun checkInResolvesActiveIncident() = runTest {
    val repository = demoRepository()

    val incidentEvent = repository.evaluateMissedCheckIns()
    assertNotNull(incidentEvent)
    repository.checkIn()

    assertTrue(repository.state.value.alertIncidents.all { it.status != com.example.data.AlertIncidentStatus.Active })
  }

  @Test
  fun deviceTokenAndSePayOrderUpdateProductionContracts() = runTest {
    val repository = InMemoryEsmeryRepository()

    repository.registerDeviceToken("token-1")
    val order = repository.createPaymentOrder(SubscriptionPlan.Monthly, PaymentProvider.SePay)
    val entitlement = repository.markPaymentOrderPaid(order.referenceCode)

    val state = repository.state.value
    assertEquals("token-1", state.deviceTokens.first().token)
    assertEquals(com.example.data.PaymentOrderStatus.Paid, state.paymentOrders.first().status)
    assertEquals(SubscriptionPlan.Monthly, entitlement?.plan)
    assertTrue(state.profile.isPremium)
    assertNotNull(entitlement?.validUntil)
  }

  @Test
  fun googlePlayVerifyCreatesEntitlementWithValidUntil() = runTest {
    val repository = InMemoryEsmeryRepository()

    val entitlement = repository.verifyGooglePlayPurchase("purchase-token-abc", "esmery_monthly")

    assertNotNull(entitlement)
    assertEquals(SubscriptionPlan.Monthly, entitlement?.plan)
    assertEquals(com.example.data.EntitlementSource.GooglePlay, entitlement?.source)
    assertNotNull(entitlement?.validUntil)
    assertTrue(repository.state.value.profile.isPremium)
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
  fun checkInDeliversTimelineAndNotificationToAcceptedCircleMember() = runTest {
    val remoteState = emptyStateFor("account-a", "a@example.com", "Account A").copy(
      circleMembers = listOf(
        CircleMember(
          id = "member-b",
          ownerUserId = "account-a",
          memberUserId = "account-b",
          invitedContact = "b@example.com",
          name = "Account B",
          relationship = "Trusted contact",
          status = CircleStatus.Accepted,
        ),
      ),
    )
    val remote = FakeRemote(remoteState)
    val repository = ResilientEsmeryRepository(remote = remote)

    repository.loadForUser("account-a", "a@example.com", "Account A")
    repository.checkIn()

    assertTrue(remote.notifications.any { it.userId == "account-b" && it.type == NotificationType.CheckInSuccess })
    assertTrue(remote.timelineEvents.any { it.userId == "account-b" })
  }

  @Test
  fun refreshKeepsLocalTimelineWhenRemoteIsBehind() = runTest {
    val remoteState = emptyStateFor("account-a", "a@example.com", "Account A")
    val remote = FakeRemote(remoteState)
    val repository = ResilientEsmeryRepository(remote = remote)

    repository.loadForUser("account-a", "a@example.com", "Account A")
    repository.checkIn()
    repository.refresh()

    assertTrue(repository.state.value.timelineEvents.any { it.title == "Check-in confirmed" })
    assertTrue(repository.state.value.notifications.any { it.type == NotificationType.CheckInSuccess })
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
  fun resilientRepositoryDoesNotSendDuplicateWhenAlreadyAccepted() = runTest {
    val remoteState = emptyStateFor("account-a", "a@example.com", "Account A").copy(
      circleMembers = listOf(
        CircleMember(
          id = "member-a-b",
          ownerUserId = "account-a",
          memberUserId = "account-b",
          invitedContact = "b@example.com",
          name = "Account B",
          relationship = "Trusted contact",
          status = CircleStatus.Accepted,
        ),
      ),
    )
    val remote = FakeRemote(
      remoteState = remoteState,
      profilesByContact = mapOf("b@example.com" to Profile(id = "account-b", displayName = "Account B", email = "b@example.com")),
    )
    val repository = ResilientEsmeryRepository(remote = remote)

    repository.loadForUser("account-a", "a@example.com", "Account A")
    val request = repository.addFriendRequest("b@example.com", "Account B", "Trusted contact")

    assertEquals(CircleStatus.Accepted, request.status)
    assertTrue(remote.friendRequests.isEmpty())
    assertEquals(1, repository.state.value.circleMembers.count { it.memberUserId == "account-b" })
  }

  @Test
  fun resilientRepositoryUsesIncomingPendingRequestInsteadOfCreatingReverseDuplicate() = runTest {
    val incoming = FriendRequest(
      id = "request-a-to-b",
      senderUserId = "account-a",
      receiverUserId = "account-b",
      receiverContact = "b@example.com",
      status = CircleStatus.Pending,
      createdAt = "2026-05-31T10:00:00",
    )
    val remoteState = emptyStateFor("account-b", "b@example.com", "Account B").copy(friendRequests = listOf(incoming))
    val remote = FakeRemote(
      remoteState = remoteState,
      profilesByContact = mapOf("a@example.com" to Profile(id = "account-a", displayName = "Account A", email = "a@example.com")),
    )
    val repository = ResilientEsmeryRepository(remote = remote)

    repository.loadForUser("account-b", "b@example.com", "Account B")
    val request = repository.addFriendRequest("a@example.com", "Account A", "Trusted contact")

    assertEquals("request-a-to-b", request.id)
    assertTrue(remote.friendRequests.isEmpty())
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
  private val profilesByContact: Map<String, Profile> = emptyMap(),
) : EsmeryRemoteDataSource {
  val notifications = mutableListOf<EsmeryNotification>()
  val checkIns = mutableListOf<CheckIn>()
  val circleMembers = mutableListOf<CircleMember>()
  val friendRequests = mutableListOf<FriendRequest>()
  val timelineEvents = mutableListOf<TimelineEvent>()
  var updatedCircleMemberUserId: String? = null

  override suspend fun fetchState(userId: String, email: String?, displayName: String?): EsmeryState? = remoteState
  override suspend fun findProfileByContact(contact: String): Profile? = profilesByContact[contact.lowercase()]
  override suspend fun findProfileById(userId: String): Profile? = profilesById[userId]
  override suspend fun upsertProfile(profile: Profile) = Unit
  override suspend fun updateProfileLastSafeAt(userId: String, lastSafeAt: String?) = Unit
  override suspend fun upsertCircleMember(member: CircleMember) {
    circleMembers += member
  }
  override suspend fun updateCircleMemberStatus(memberId: String, status: CircleStatus, memberUserId: String?) {
    updatedCircleMemberUserId = memberUserId
  }
  override suspend fun insertFriendRequest(request: FriendRequest) {
    friendRequests += request
  }
  override suspend fun updateFriendRequestStatus(requestId: String, status: CircleStatus) = Unit
  override suspend fun insertCheckIn(checkIn: CheckIn) {
    checkIns += checkIn
  }
  override suspend fun insertTimelineEvent(event: TimelineEvent) {
    timelineEvents += event
  }
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
  override suspend fun upsertDeviceToken(token: DeviceToken) = Unit
  override suspend fun deactivateDeviceToken(token: String) = Unit
  override suspend fun insertNotificationDelivery(delivery: NotificationDelivery) = Unit
  override suspend fun upsertAlertIncident(incident: AlertIncident) = Unit
  override suspend fun upsertAlertJob(job: AlertJob) = Unit
  override suspend fun upsertLocationShare(share: LocationShare) = Unit
  override suspend fun upsertPaymentOrder(order: PaymentOrder) = Unit
  override suspend fun upsertEntitlement(entitlement: Entitlement) = Unit
  override suspend fun verifyGooglePlayPurchase(purchaseToken: String, productId: String): Entitlement? = null
  override suspend fun insertAuditLog(log: com.example.data.AuditLog) = Unit
  override suspend fun changePassword(newPassword: String) = Unit
  override suspend fun deleteAccount(userId: String) = Unit
  override suspend fun tryUpload(path: String, bytes: ByteArray): String? = "https://example.com/$path"
  override suspend fun startNotificationRealtime(userId: String, onChange: () -> Unit) = Unit
  override suspend fun stopNotificationRealtime() = Unit
}
