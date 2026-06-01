package com.example.data

import android.util.Log
import com.example.supabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TAG = "EsmeryRemote"

class ResilientEsmeryRepository(
  private val local: InMemoryEsmeryRepository = InMemoryEsmeryRepository(),
  private val remote: EsmeryRemoteDataSource = SupabaseEsmeryRemoteDataSource(),
) : EsmeryRepository {
  override val state: StateFlow<EsmeryState> = local.state
  private val refreshMutex = Mutex()
  private var userId: String? = null
  private var email: String? = null
  private var displayName: String? = null

  override suspend fun loadForUser(userId: String, email: String?, displayName: String?) {
    this.userId = userId
    this.email = email?.normalizedContact()
    this.displayName = displayName
    local.replaceWithEmptyUser(userId, this.email, displayName)
    refresh()
  }

  override suspend fun refresh() {
    refreshMutex.withLock {
      val id = userId ?: return
      val currentEmail = email
      val currentDisplayName = displayName
      val remoteState = remote.tryFetchState(id, currentEmail, currentDisplayName)
      if (id != userId) return
      if (remoteState != null) {
        local.replaceState(mergeState(local.state.value, remoteState))
        remote.tryRemote {
          upsertProfile(local.state.value.profile)
          upsertSubscription(local.state.value.subscriptionStatus)
          upsertSafetySettings(local.state.value.safetySettings)
        }
      } else {
        remote.tryRemote {
          upsertProfile(local.state.value.profile)
          upsertSubscription(local.state.value.subscriptionStatus)
          upsertSafetySettings(local.state.value.safetySettings)
        }
        remote.tryFetchState(id, currentEmail, currentDisplayName)?.let {
          if (id == userId) local.replaceState(mergeState(local.state.value, it))
        }
      }
    }
  }

  override suspend fun clearLocalSession() {
    userId = null
    email = null
    displayName = null
    local.clearLocalSession()
  }

  override suspend fun checkIn(note: String?): CheckIn {
    val checkIn = local.checkIn(note)
    val state = local.state.value
    remote.tryRemote {
      insertCheckIn(checkIn)
      updateProfileLastSafeAt(state.profile.id, state.profile.lastSafeAt)
      insertTimelineEvent(state.timelineEvents.first())
      insertNotification(state.notifications.first())
      insertLocalEnvelope(state)
      recipientDeliveriesForCheckIn(state, checkIn.id, checkIn.createdAt).forEach { delivery ->
        insertNotification(delivery.notification)
        insertTimelineEvent(delivery.event)
      }
    }
    return checkIn
  }

  override suspend fun addFriendRequest(contact: String, name: String, relationship: String): FriendRequest {
    val normalizedContact = contact.normalizedContact()
    val matchedProfile = remote.tryFindProfileByContact(normalizedContact)
    refresh()
    val syncedState = local.state.value
    if (matchedProfile?.id == syncedState.profile.id) {
      return FriendRequest(
        id = "",
        senderUserId = syncedState.profile.id,
        receiverUserId = syncedState.profile.id,
        receiverContact = normalizedContact,
        status = CircleStatus.Declined,
        createdAt = "",
      )
    }
    val existingRequest = syncedState.friendRequests.firstOrNull {
      it.status != CircleStatus.Declined && it.matchesConnection(
        currentUserId = syncedState.profile.id,
        contact = normalizedContact,
        profileId = matchedProfile?.id,
      )
    }
    if (existingRequest != null) return existingRequest
    val existingMember = syncedState.circleMembers.firstOrNull {
      it.status != CircleStatus.Declined && it.matchesConnection(
        contact = normalizedContact,
        profileId = matchedProfile?.id,
      )
    }
    if (existingMember != null) {
      return FriendRequest(
        id = existingMember.id,
        senderUserId = syncedState.profile.id,
        receiverUserId = existingMember.memberUserId ?: matchedProfile?.id,
        receiverContact = existingMember.invitedContact.normalizedContact(),
        status = existingMember.status,
        createdAt = "",
      )
    }
    val request = local.addFriendRequest(normalizedContact, name, relationship)
    if (matchedProfile != null) {
      val current = local.state.value
      local.replaceState(
        current.copy(
          friendRequests = current.friendRequests.map { item ->
            if (item.id == request.id) item.copy(receiverUserId = matchedProfile.id) else item
          },
          circleMembers = current.circleMembers.map { member ->
            if (member.id == request.id) {
              member.copy(
                memberUserId = matchedProfile.id,
                name = name.ifBlank { matchedProfile.displayName },
              )
            } else {
              member
            }
          },
        ),
      )
    }
    val state = local.state.value
    val requestForRemote = state.friendRequests.firstOrNull { it.id == request.id } ?: request
    remote.tryRemote {
      insertFriendRequest(requestForRemote)
      upsertCircleMember(state.circleMembers.first())
      insertTimelineEvent(state.timelineEvents.first())
      state.auditLogs.firstOrNull()?.let { insertAuditLog(it) }
      requestForRemote.receiverUserId?.let { receiverId ->
        insertNotification(
          EsmeryNotification(
            id = id(),
            userId = receiverId,
            type = NotificationType.FriendRequest,
            title = "Circle invitation received",
            body = "Open Circle to accept or decline this invitation.",
            relatedEntityId = requestForRemote.id,
            createdAt = requestForRemote.createdAt,
          ),
        )
      }
    }
    return request
  }

  override suspend fun updateFriendRequest(requestId: String, status: CircleStatus) {
    val currentUserId = userId
    val requestBeforeUpdate = local.state.value.friendRequests.firstOrNull { it.id == requestId }
    local.updateFriendRequest(requestId, status)
    val reciprocalMember = if (
      currentUserId != null &&
      requestBeforeUpdate != null &&
      requestBeforeUpdate.senderUserId != currentUserId &&
      status == CircleStatus.Accepted
    ) {
      val senderProfile = remote.tryFindProfileById(requestBeforeUpdate.senderUserId)
      CircleMember(
        id = UUID.randomUUID().toString(),
        ownerUserId = currentUserId,
        memberUserId = requestBeforeUpdate.senderUserId,
        invitedContact = senderProfile?.email ?: requestBeforeUpdate.senderUserId,
        name = senderProfile?.displayName ?: "Trusted contact",
        relationship = "Trusted contact",
        status = CircleStatus.Accepted,
        lastSafeAt = senderProfile?.lastSafeAt,
      )
    } else {
      null
    }
    if (reciprocalMember != null) {
      val current = local.state.value
      local.replaceState(
        current.copy(
          circleMembers = listOf(reciprocalMember) + current.circleMembers.filterNot {
            it.memberUserId == reciprocalMember.memberUserId
          },
        ),
      )
    }
    remote.tryRemote {
      updateFriendRequestStatus(requestId, status)
      updateCircleMemberStatus(requestId, status, currentUserId)
      if (reciprocalMember != null) upsertCircleMember(reciprocalMember)
      insertTimelineEvent(local.state.value.timelineEvents.first())
      local.state.value.auditLogs.firstOrNull()?.let { insertAuditLog(it) }
    }
  }

  override suspend fun sendNudge(memberId: String): TimelineEvent {
    val event = local.sendNudge(memberId)
    val state = local.state.value
    remote.tryRemote {
      insertTimelineEvent(event)
      insertNotification(local.state.value.notifications.first())
      insertLocalEnvelope(local.state.value)
      state.circleMembers.firstOrNull { it.id == memberId }?.memberUserId?.let { receiverId ->
        val notification = EsmeryNotification(
          id = id(),
          userId = receiverId,
          type = NotificationType.GentleNudge,
          title = "Gentle nudge received",
          body = "${state.profile.displayName} sent you a gentle nudge.",
          relatedEntityId = memberId,
          createdAt = event.createdAt,
        )
        insertNotification(notification)
        insertTimelineEvent(
          TimelineEvent(
            id = id(),
            userId = receiverId,
            type = TimelineEventType.Nudge,
            title = "Gentle nudge received",
            body = "${state.profile.displayName} sent you a gentle nudge.",
            relatedEntityId = memberId,
            createdAt = event.createdAt,
          ),
        )
      }
    }
    return event
  }

  override suspend fun shareMoment(caption: String, imageUrl: String): Moment {
    val moment = local.shareMoment(caption, imageUrl)
    val event = local.state.value.timelineEvents.first()
    remote.tryRemote {
      insertMoment(moment)
      insertTimelineEvent(event)
      insertNotification(local.state.value.notifications.first())
      insertLocalEnvelope(local.state.value)
      recipientDeliveriesForMoment(local.state.value, moment, event.createdAt).forEach { delivery ->
        insertNotification(delivery.notification)
        insertTimelineEvent(delivery.event)
      }
    }
    return moment
  }

  override suspend fun markNotificationRead(notificationId: String) {
    local.markNotificationRead(notificationId)
    remote.tryRemote { markNotificationRead(notificationId) }
  }

  override suspend fun saveEmergencyContact(contact: EmergencyContact): EmergencyContact {
    val saved = local.saveEmergencyContact(contact)
    val event = local.state.value.timelineEvents.first()
    remote.tryRemote {
      upsertEmergencyContact(saved)
      insertTimelineEvent(event)
      local.state.value.auditLogs.firstOrNull()?.let { insertAuditLog(it) }
    }
    return saved
  }

  override suspend fun deleteEmergencyContact(contactId: String) {
    local.deleteEmergencyContact(contactId)
    remote.tryRemote { deleteEmergencyContact(contactId) }
  }

  override suspend fun toggleEmergencyContactVerified(contactId: String): EmergencyContact? {
    val updated = local.toggleEmergencyContactVerified(contactId)
    if (updated != null) remote.tryRemote { upsertEmergencyContact(updated) }
    return updated
  }

  override suspend fun toggleEmergencyContactAutoNotify(contactId: String): EmergencyContact? {
    val updated = local.toggleEmergencyContactAutoNotify(contactId)
    if (updated != null) remote.tryRemote { upsertEmergencyContact(updated) }
    return updated
  }

  override suspend fun saveSafetyRhythm(rhythm: SafetyRhythm): SafetyRhythm {
    val saved = local.saveSafetyRhythm(rhythm)
    val event = local.state.value.timelineEvents.first()
    remote.tryRemote {
      upsertSafetyRhythm(saved)
      insertTimelineEvent(event)
    }
    return saved
  }

  override suspend fun deleteSafetyRhythm(rhythmId: String) {
    local.deleteSafetyRhythm(rhythmId)
    remote.tryRemote { deleteSafetyRhythm(rhythmId) }
  }

  override suspend fun toggleSafetyRhythm(rhythmId: String): SafetyRhythm? {
    val updated = local.toggleSafetyRhythm(rhythmId)
    if (updated != null) remote.tryRemote { upsertSafetyRhythm(updated) }
    return updated
  }

  override suspend fun updateSafetySettings(settings: SafetySettings): SafetySettings {
    val saved = local.updateSafetySettings(settings)
    remote.tryRemote { upsertSafetySettings(saved) }
    return saved
  }

  override suspend fun evaluateMissedCheckIns(): TimelineEvent? {
    val event = local.evaluateMissedCheckIns()
    if (event != null) {
      remote.tryRemote {
        insertTimelineEvent(event)
        insertNotification(local.state.value.notifications.first())
        insertLocalEnvelope(local.state.value)
      }
    }
    return event
  }

  override suspend fun triggerEmergencyAlert(): TimelineEvent {
    val event = local.triggerEmergencyAlert()
    remote.tryRemote {
      insertTimelineEvent(event)
      insertNotification(local.state.value.notifications.first())
      insertLocalEnvelope(local.state.value)
    }
    return event
  }

  override suspend fun updateSubscription(plan: SubscriptionPlan): SubscriptionStatus {
    val subscription = local.updateSubscription(plan)
    remote.tryRemote {
      upsertSubscription(subscription)
      upsertEntitlement(local.state.value.entitlement)
      upsertProfile(local.state.value.profile)
      local.state.value.auditLogs.firstOrNull()?.let { insertAuditLog(it) }
    }
    return subscription
  }

  override suspend fun registerDeviceToken(token: String, provider: String): DeviceToken {
    val saved = local.registerDeviceToken(token, provider)
    remote.tryRemote {
      upsertDeviceToken(saved)
      local.state.value.auditLogs.firstOrNull()?.let { insertAuditLog(it) }
    }
    return saved
  }

  override suspend fun unregisterDeviceToken(token: String) {
    local.unregisterDeviceToken(token)
    remote.tryRemote {
      deactivateDeviceToken(token)
      local.state.value.auditLogs.firstOrNull()?.let { insertAuditLog(it) }
    }
  }

  override suspend fun resolveAlertIncident(incidentId: String): AlertIncident? {
    val resolved = local.resolveAlertIncident(incidentId)
    if (resolved != null) {
      remote.tryRemote {
        upsertAlertIncident(resolved)
        local.state.value.alertJobs
          .filter { it.incidentId == incidentId }
          .forEach { upsertAlertJob(it) }
        local.state.value.auditLogs.firstOrNull()?.let { insertAuditLog(it) }
      }
    }
    return resolved
  }

  override suspend fun shareEmergencyLocation(latitude: Double, longitude: Double, accuracyMeters: Double?): LocationShare {
    val share = local.shareEmergencyLocation(latitude, longitude, accuracyMeters)
    remote.tryRemote {
      upsertLocationShare(share)
      local.state.value.auditLogs.firstOrNull()?.let { insertAuditLog(it) }
    }
    return share
  }

  override suspend fun createPaymentOrder(plan: SubscriptionPlan, provider: PaymentProvider): PaymentOrder {
    val order = local.createPaymentOrder(plan, provider)
    remote.tryRemote {
      upsertPaymentOrder(order)
      local.state.value.auditLogs.firstOrNull()?.let { insertAuditLog(it) }
    }
    return order
  }

  override suspend fun markPaymentOrderPaid(referenceCode: String): Entitlement? {
    val entitlement = local.markPaymentOrderPaid(referenceCode)
    if (entitlement != null) {
      remote.tryRemote {
        local.state.value.paymentOrders.firstOrNull { it.referenceCode == referenceCode }?.let { upsertPaymentOrder(it) }
        upsertEntitlement(entitlement)
        upsertSubscription(local.state.value.subscriptionStatus)
        upsertProfile(local.state.value.profile)
        local.state.value.auditLogs.firstOrNull()?.let { insertAuditLog(it) }
      }
    }
    return entitlement
  }

  private suspend fun EsmeryRemoteDataSource.insertLocalEnvelope(state: EsmeryState) {
    val notificationId = state.notifications.firstOrNull()?.id
    if (notificationId != null) {
      state.notificationDeliveries
        .filter { it.notificationId == notificationId }
        .forEach { insertNotificationDelivery(it) }
    }
    state.alertIncidents.firstOrNull()?.let { upsertAlertIncident(it) }
    state.alertJobs
      .filter { job -> state.alertIncidents.firstOrNull()?.id == job.incidentId }
      .forEach { upsertAlertJob(it) }
    state.auditLogs.firstOrNull()?.let { insertAuditLog(it) }
  }

  private fun recipientDeliveriesForCheckIn(
    state: EsmeryState,
    checkInId: String,
    createdAt: String,
  ): List<RemoteDelivery> = acceptedRecipientIds(state).map { receiverId ->
    val title = "${state.profile.displayName} is safe"
    val body = "A fresh safety check-in was sent."
    RemoteDelivery(
      event = TimelineEvent(
        id = id(),
        userId = receiverId,
        type = TimelineEventType.CheckIn,
        title = title,
        body = body,
        relatedEntityId = checkInId,
        createdAt = createdAt,
      ),
      notification = EsmeryNotification(
        id = id(),
        userId = receiverId,
        type = NotificationType.CheckInSuccess,
        title = title,
        body = body,
        relatedEntityId = checkInId,
        createdAt = createdAt,
      ),
    )
  }

  private fun recipientDeliveriesForMoment(
    state: EsmeryState,
    moment: Moment,
    createdAt: String,
  ): List<RemoteDelivery> = acceptedRecipientIds(state).map { receiverId ->
    val title = "${state.profile.displayName} shared a moment"
    RemoteDelivery(
      event = TimelineEvent(
        id = id(),
        userId = receiverId,
        type = TimelineEventType.Moment,
        title = title,
        body = moment.caption,
        relatedEntityId = moment.id,
        createdAt = createdAt,
      ),
      notification = EsmeryNotification(
        id = id(),
        userId = receiverId,
        type = NotificationType.MomentShared,
        title = title,
        body = moment.caption,
        relatedEntityId = moment.id,
        createdAt = createdAt,
      ),
    )
  }

  private fun acceptedRecipientIds(state: EsmeryState): List<String> = state.circleMembers
    .filter { it.status == CircleStatus.Accepted }
    .mapNotNull { it.memberUserId }
    .filterNot { it == state.profile.id }
    .distinct()

  private fun id(): String = UUID.randomUUID().toString()

  private fun mergeState(localState: EsmeryState, remoteState: EsmeryState): EsmeryState {
    if (localState.profile.id != remoteState.profile.id) return remoteState
    return remoteState.copy(
      circleMembers = dedupeCircleMembers(mergeById(localState.circleMembers, remoteState.circleMembers) { it.id })
        .sortedByDescending { it.lastSafeAt.orEmpty() },
      friendRequests = mergeById(localState.friendRequests, remoteState.friendRequests) { it.id }
        .sortedByDescending { it.createdAt },
      checkIns = mergeById(localState.checkIns, remoteState.checkIns) { it.id }
        .sortedByDescending { it.createdAt },
      timelineEvents = mergeById(localState.timelineEvents, remoteState.timelineEvents) { it.id }
        .sortedByDescending { it.createdAt },
      notifications = mergeById(localState.notifications, remoteState.notifications) { it.id }
        .sortedByDescending { it.createdAt },
      moments = mergeById(localState.moments, remoteState.moments) { it.id }
        .sortedByDescending { it.createdAt },
      emergencyContacts = mergeById(localState.emergencyContacts, remoteState.emergencyContacts) { it.id }
        .sortedBy { it.name },
      safetyRhythms = mergeById(localState.safetyRhythms, remoteState.safetyRhythms) { it.id }
        .sortedBy { it.checkTime },
      deviceTokens = mergeById(localState.deviceTokens, remoteState.deviceTokens) { it.id }
        .sortedByDescending { it.lastSeenAt },
      notificationDeliveries = mergeById(localState.notificationDeliveries, remoteState.notificationDeliveries) { it.id }
        .sortedByDescending { it.createdAt },
      alertIncidents = mergeById(localState.alertIncidents, remoteState.alertIncidents) { it.id }
        .sortedByDescending { it.createdAt },
      alertJobs = mergeById(localState.alertJobs, remoteState.alertJobs) { it.id }
        .sortedByDescending { it.runAt },
      locationShares = mergeById(localState.locationShares, remoteState.locationShares) { it.id }
        .sortedByDescending { it.createdAt },
      paymentOrders = mergeById(localState.paymentOrders, remoteState.paymentOrders) { it.id }
        .sortedByDescending { it.createdAt },
      entitlement = if (remoteState.entitlement.updatedAt >= localState.entitlement.updatedAt) remoteState.entitlement else localState.entitlement,
      auditLogs = mergeById(localState.auditLogs, remoteState.auditLogs) { it.id }
        .sortedByDescending { it.createdAt },
    )
  }

  private fun <T> mergeById(localItems: List<T>, remoteItems: List<T>, key: (T) -> String): List<T> {
    val result = LinkedHashMap<String, T>()
    localItems.forEach { result[key(it)] = it }
    remoteItems.forEach { result[key(it)] = it }
    return result.values.toList()
  }

  private fun dedupeCircleMembers(items: List<CircleMember>): List<CircleMember> {
    val sorted = items.sortedWith(
      compareByDescending<CircleMember> { it.status == CircleStatus.Accepted }
        .thenByDescending { it.memberUserId != null }
        .thenByDescending { it.lastSafeAt.orEmpty() },
    )
    val result = mutableListOf<CircleMember>()
    sorted.forEach { item ->
      val keys = item.connectionKeys()
      val hasSameConnection = result.any { existing ->
        existing.connectionKeys().any { it in keys }
      }
      if (!hasSameConnection) result += item
    }
    return result
  }
}

private data class RemoteDelivery(
  val event: TimelineEvent,
  val notification: EsmeryNotification,
)

private fun CircleMember.matchesConnection(contact: String, profileId: String?): Boolean {
  val normalizedContact = contact.normalizedContact()
  return (memberUserId != null && memberUserId == profileId) ||
    invitedContact.normalizedContact() == normalizedContact
}

private fun FriendRequest.matchesConnection(
  currentUserId: String,
  contact: String,
  profileId: String?,
): Boolean {
  val normalizedContact = contact.normalizedContact()
  val receiverMatches = (receiverUserId != null && receiverUserId == profileId) ||
    receiverContact.normalizedContact() == normalizedContact
  val senderMatches = senderUserId == profileId && senderUserId != currentUserId
  return receiverMatches || senderMatches
}

private fun CircleMember.connectionKeys(): Set<String> = buildSet {
  memberUserId?.takeIf { it.isNotBlank() }?.let { add("user:$it") }
  invitedContact.normalizedContact().takeIf { it.isNotBlank() }?.let { add("contact:$it") }
}

interface EsmeryRemoteDataSource {
  suspend fun fetchState(userId: String, email: String?, displayName: String?): EsmeryState?
  suspend fun findProfileByContact(contact: String): Profile?
  suspend fun findProfileById(userId: String): Profile?
  suspend fun upsertProfile(profile: Profile)
  suspend fun updateProfileLastSafeAt(userId: String, lastSafeAt: String?)
  suspend fun upsertCircleMember(member: CircleMember)
  suspend fun updateCircleMemberStatus(memberId: String, status: CircleStatus, memberUserId: String?)
  suspend fun insertFriendRequest(request: FriendRequest)
  suspend fun updateFriendRequestStatus(requestId: String, status: CircleStatus)
  suspend fun insertCheckIn(checkIn: CheckIn)
  suspend fun insertTimelineEvent(event: TimelineEvent)
  suspend fun insertNotification(notification: EsmeryNotification)
  suspend fun markNotificationRead(notificationId: String)
  suspend fun insertMoment(moment: Moment)
  suspend fun upsertEmergencyContact(contact: EmergencyContact)
  suspend fun deleteEmergencyContact(contactId: String)
  suspend fun upsertSafetyRhythm(rhythm: SafetyRhythm)
  suspend fun deleteSafetyRhythm(rhythmId: String)
  suspend fun upsertSafetySettings(settings: SafetySettings)
  suspend fun upsertSubscription(subscription: SubscriptionStatus)
  suspend fun upsertDeviceToken(token: DeviceToken)
  suspend fun deactivateDeviceToken(token: String)
  suspend fun insertNotificationDelivery(delivery: NotificationDelivery)
  suspend fun upsertAlertIncident(incident: AlertIncident)
  suspend fun upsertAlertJob(job: AlertJob)
  suspend fun upsertLocationShare(share: LocationShare)
  suspend fun upsertPaymentOrder(order: PaymentOrder)
  suspend fun upsertEntitlement(entitlement: Entitlement)
  suspend fun insertAuditLog(log: AuditLog)
}

class SupabaseEsmeryRemoteDataSource(
  private val client: SupabaseClient = supabase,
) : EsmeryRemoteDataSource {
  override suspend fun findProfileById(userId: String): Profile? {
    if (userId.isBlank()) return null
    return client.from("profiles").select {
      filter { eq("id", userId) }
    }.decodeSingleOrNull<Profile>()
  }

  override suspend fun findProfileByContact(contact: String): Profile? {
    val trimmed = contact.normalizedContact()
    if (trimmed.isBlank()) return null
    val emailMatch = client.from("profiles").select {
      filter { eq("email", trimmed) }
    }.decodeSingleOrNull<Profile>()
    if (emailMatch != null) return emailMatch
    return client.from("profiles").select {
      filter { eq("phone", trimmed) }
    }.decodeSingleOrNull<Profile>()
  }

  override suspend fun fetchState(userId: String, email: String?, displayName: String?): EsmeryState? {
    val normalizedEmail = email?.normalizedContact()
    val profile = remoteOrNull("profiles current user") {
      client.from("profiles").select {
        filter { eq("id", userId) }
      }.decodeSingleOrNull<Profile>()
    }
    val resolvedProfile = profile ?: Profile(
      id = userId,
      displayName = displayName ?: normalizedEmail?.substringBefore('@') ?: "ESMERY Friend",
      email = normalizedEmail,
    )

    val sentRequests = remoteOrDefault("friend_requests sent", emptyList()) {
      client.from("friend_requests").select {
        filter { eq("sender_user_id", userId) }
      }.decodeList<FriendRequest>()
    }
    val receivedByUserId = remoteOrDefault("friend_requests received by user id", emptyList()) {
      client.from("friend_requests").select {
        filter { eq("receiver_user_id", userId) }
      }.decodeList<FriendRequest>()
    }
    val receivedByContact = normalizedEmail?.takeIf { it.isNotBlank() }?.let { contact ->
      remoteOrDefault("friend_requests received by contact", emptyList()) {
        client.from("friend_requests").select {
          filter { ilike("receiver_contact", contact) }
        }.decodeList<FriendRequest>()
      }
    }.orEmpty()
    val receivedRequests = receivedByUserId + receivedByContact
    val receivedSenderProfiles = receivedRequests
      .map { it.senderUserId }
      .distinct()
      .associateWith { senderId ->
        remoteOrNull("profiles request sender $senderId") {
          client.from("profiles").select {
            filter { eq("id", senderId) }
          }.decodeSingleOrNull<Profile>()
        }
      }

    val subscription = remoteOrNull("subscription_status") {
      client.from("subscription_status").select {
        filter { eq("user_id", userId) }
      }.decodeSingleOrNull<SubscriptionStatus>()
    } ?: SubscriptionStatus(userId = userId)

    val entitlement = remoteOrNull("entitlements") {
      client.from("entitlements").select {
        filter { eq("user_id", userId) }
      }.decodeSingleOrNull<Entitlement>()
    } ?: Entitlement(
      userId = userId,
      plan = subscription.plan,
      isPremium = subscription.plan != SubscriptionPlan.Basic,
      source = if (subscription.plan == SubscriptionPlan.Basic) EntitlementSource.Basic else EntitlementSource.Manual,
      updatedAt = "",
    )

    val settings = remoteOrNull("safety_settings") {
      client.from("safety_settings").select {
        filter { eq("user_id", userId) }
      }.decodeSingleOrNull<SafetySettings>()
    } ?: SafetySettings(userId = userId)

    val ownedCircleMembers = remoteOrDefault("circle_members owned", emptyList()) {
      client.from("circle_members").select {
        filter { eq("owner_user_id", userId) }
      }.decodeList<CircleMember>()
    }
    val memberCircleRows = remoteOrDefault("circle_members received", emptyList()) {
      client.from("circle_members").select {
        filter { eq("member_user_id", userId) }
      }.decodeList<CircleMember>()
    }
    val receivedCircleMembers = memberCircleRows
      .filter { it.ownerUserId != userId && it.status == CircleStatus.Accepted }
      .map { row ->
        val ownerProfile = remoteOrNull("profiles circle owner ${row.ownerUserId}") {
          client.from("profiles").select {
            filter { eq("id", row.ownerUserId) }
          }.decodeSingleOrNull<Profile>()
        }
        CircleMember(
          id = row.id,
          ownerUserId = userId,
          memberUserId = row.ownerUserId,
          invitedContact = ownerProfile?.email ?: row.ownerUserId,
          name = ownerProfile?.displayName ?: row.name,
          relationship = row.relationship,
          status = row.status,
          lastSafeAt = ownerProfile?.lastSafeAt ?: row.lastSafeAt,
        )
      }
    val requestCircleMembers = receivedRequests.map { request ->
      val senderProfile = receivedSenderProfiles[request.senderUserId]
      CircleMember(
        id = request.id,
        ownerUserId = userId,
        memberUserId = request.senderUserId,
        invitedContact = senderProfile?.email ?: request.senderUserId,
        name = senderProfile?.displayName ?: "Trusted contact",
        relationship = "Trusted contact",
        status = request.status,
        lastSafeAt = senderProfile?.lastSafeAt,
      )
    }
    val circleMembers = (ownedCircleMembers + receivedCircleMembers + requestCircleMembers)
      .distinctBy { it.memberUserId ?: it.invitedContact }
      .sortedByDescending { it.lastSafeAt.orEmpty() }

    return EsmeryState(
      profile = resolvedProfile.copy(
        displayName = resolvedProfile.displayName.ifBlank { displayName ?: "ESMERY Friend" },
        email = resolvedProfile.email?.normalizedContact() ?: normalizedEmail,
      ),
      circleMembers = circleMembers,
      friendRequests = (sentRequests + receivedRequests).distinctBy { it.id }.sortedByDescending { it.createdAt },
      checkIns = remoteOrDefault("check_ins", emptyList()) {
        client.from("check_ins").select {
          filter { eq("user_id", userId) }
        }.decodeList<CheckIn>()
      }.sortedByDescending { it.createdAt },
      timelineEvents = remoteOrDefault("timeline_events", emptyList()) {
        client.from("timeline_events").select {
          filter { eq("user_id", userId) }
        }.decodeList<TimelineEvent>()
      }.sortedByDescending { it.createdAt },
      notifications = remoteOrDefault("notifications", emptyList()) {
        client.from("notifications").select {
          filter { eq("user_id", userId) }
        }.decodeList<EsmeryNotification>()
      }.sortedByDescending { it.createdAt },
      moments = remoteOrDefault("moments", emptyList()) {
        client.from("moments").select().decodeList<Moment>()
      }.sortedByDescending { it.createdAt },
      emergencyContacts = remoteOrDefault("emergency_contacts", emptyList()) {
        client.from("emergency_contacts").select {
          filter { eq("user_id", userId) }
        }.decodeList<EmergencyContact>()
      }.sortedBy { it.name },
      safetyRhythms = remoteOrDefault("safety_rhythms", emptyList()) {
        client.from("safety_rhythms").select {
          filter { eq("user_id", userId) }
        }.decodeList<SafetyRhythm>()
      }.sortedBy { it.checkTime },
      safetySettings = settings,
      subscriptionStatus = subscription,
      deviceTokens = remoteOrDefault("device_tokens", emptyList()) {
        client.from("device_tokens").select {
          filter { eq("user_id", userId) }
        }.decodeList<DeviceToken>()
      }.sortedByDescending { it.lastSeenAt },
      notificationDeliveries = remoteOrDefault("notification_deliveries", emptyList()) {
        client.from("notification_deliveries").select {
          filter { eq("user_id", userId) }
        }.decodeList<NotificationDelivery>()
      }.sortedByDescending { it.createdAt },
      alertIncidents = remoteOrDefault("alert_incidents", emptyList()) {
        client.from("alert_incidents").select {
          filter { eq("user_id", userId) }
        }.decodeList<AlertIncident>()
      }.sortedByDescending { it.createdAt },
      alertJobs = remoteOrDefault("alert_jobs", emptyList()) {
        client.from("alert_jobs").select {
          filter { eq("user_id", userId) }
        }.decodeList<AlertJob>()
      }.sortedByDescending { it.runAt },
      locationShares = remoteOrDefault("location_shares", emptyList()) {
        client.from("location_shares").select {
          filter { eq("user_id", userId) }
        }.decodeList<LocationShare>()
      }.sortedByDescending { it.createdAt },
      paymentOrders = remoteOrDefault("payment_orders", emptyList()) {
        client.from("payment_orders").select {
          filter { eq("user_id", userId) }
        }.decodeList<PaymentOrder>()
      }.sortedByDescending { it.createdAt },
      entitlement = entitlement,
      auditLogs = remoteOrDefault("audit_logs", emptyList()) {
        client.from("audit_logs").select {
          filter { eq("user_id", userId) }
        }.decodeList<AuditLog>()
      }.sortedByDescending { it.createdAt },
    )
  }

  override suspend fun upsertProfile(profile: Profile) {
    client.from("profiles").upsert(profile)
  }

  override suspend fun updateProfileLastSafeAt(userId: String, lastSafeAt: String?) {
    client.from("profiles").update(LastSafeAtUpdate(lastSafeAt)) {
      filter { eq("id", userId) }
    }
  }

  override suspend fun upsertCircleMember(member: CircleMember) {
    client.from("circle_members").upsert(member)
  }

  override suspend fun updateCircleMemberStatus(memberId: String, status: CircleStatus, memberUserId: String?) {
    client.from("circle_members").update(CircleMemberStatusUpdate(status = status, memberUserId = memberUserId)) {
      filter { eq("id", memberId) }
    }
  }

  override suspend fun insertFriendRequest(request: FriendRequest) {
    client.from("friend_requests").insert(request)
  }

  override suspend fun updateFriendRequestStatus(requestId: String, status: CircleStatus) {
    client.from("friend_requests").update(StatusUpdate(status)) {
      filter { eq("id", requestId) }
    }
  }

  override suspend fun insertCheckIn(checkIn: CheckIn) {
    client.from("check_ins").insert(checkIn)
  }

  override suspend fun insertTimelineEvent(event: TimelineEvent) {
    client.from("timeline_events").insert(event)
  }

  override suspend fun insertNotification(notification: EsmeryNotification) {
    client.from("notifications").insert(notification)
  }

  override suspend fun markNotificationRead(notificationId: String) {
    client.from("notifications").update(NotificationReadUpdate(isRead = true)) {
      filter { eq("id", notificationId) }
    }
  }

  override suspend fun insertMoment(moment: Moment) {
    client.from("moments").insert(moment)
  }

  override suspend fun upsertEmergencyContact(contact: EmergencyContact) {
    client.from("emergency_contacts").upsert(contact)
  }

  override suspend fun deleteEmergencyContact(contactId: String) {
    client.from("emergency_contacts").delete {
      filter { eq("id", contactId) }
    }
  }

  override suspend fun upsertSafetyRhythm(rhythm: SafetyRhythm) {
    client.from("safety_rhythms").upsert(rhythm)
  }

  override suspend fun deleteSafetyRhythm(rhythmId: String) {
    client.from("safety_rhythms").delete {
      filter { eq("id", rhythmId) }
    }
  }

  override suspend fun upsertSafetySettings(settings: SafetySettings) {
    client.from("safety_settings").upsert(settings)
  }

  override suspend fun upsertSubscription(subscription: SubscriptionStatus) {
    client.from("subscription_status").upsert(subscription)
  }

  override suspend fun upsertDeviceToken(token: DeviceToken) {
    client.from("device_tokens").upsert(token)
  }

  override suspend fun deactivateDeviceToken(token: String) {
    client.from("device_tokens").update(DeviceTokenActiveUpdate(isActive = false)) {
      filter { eq("token", token) }
    }
  }

  override suspend fun insertNotificationDelivery(delivery: NotificationDelivery) {
    client.from("notification_deliveries").insert(delivery)
  }

  override suspend fun upsertAlertIncident(incident: AlertIncident) {
    client.from("alert_incidents").upsert(incident)
  }

  override suspend fun upsertAlertJob(job: AlertJob) {
    client.from("alert_jobs").upsert(job)
  }

  override suspend fun upsertLocationShare(share: LocationShare) {
    client.from("location_shares").upsert(share)
  }

  override suspend fun upsertPaymentOrder(order: PaymentOrder) {
    client.from("payment_orders").upsert(order)
  }

  override suspend fun upsertEntitlement(entitlement: Entitlement) {
    client.from("entitlements").upsert(entitlement)
  }

  override suspend fun insertAuditLog(log: AuditLog) {
    client.from("audit_logs").insert(log)
  }
}

private suspend fun EsmeryRemoteDataSource.tryRemote(block: suspend EsmeryRemoteDataSource.() -> Unit) {
  try {
    block()
  } catch (error: CancellationException) {
    throw error
  } catch (error: Throwable) {
    Log.e(TAG, "Remote write failed", error)
  }
}

private suspend fun EsmeryRemoteDataSource.tryFetchState(
  userId: String,
  email: String?,
  displayName: String?,
): EsmeryState? = try {
  fetchState(userId, email, displayName)
} catch (error: CancellationException) {
  throw error
} catch (error: Throwable) {
  Log.e(TAG, "Remote fetch failed for $userId", error)
  null
}

private suspend fun EsmeryRemoteDataSource.tryFindProfileByContact(contact: String): Profile? = try {
  findProfileByContact(contact)
} catch (error: CancellationException) {
  throw error
} catch (error: Throwable) {
  Log.e(TAG, "Profile lookup failed for contact=$contact", error)
  null
}

private suspend fun EsmeryRemoteDataSource.tryFindProfileById(userId: String): Profile? = try {
  findProfileById(userId)
} catch (error: CancellationException) {
  throw error
} catch (error: Throwable) {
  Log.e(TAG, "Profile lookup failed for userId=$userId", error)
  null
}

private suspend inline fun <T> remoteOrDefault(label: String, default: T, block: suspend () -> T): T =
  try {
    block()
  } catch (error: CancellationException) {
    throw error
  } catch (error: Throwable) {
    Log.e(TAG, "Remote read failed: $label", error)
    default
  }

private suspend inline fun <T> remoteOrNull(label: String, block: suspend () -> T?): T? =
  try {
    block()
  } catch (error: CancellationException) {
    throw error
  } catch (error: Throwable) {
    Log.e(TAG, "Remote read failed: $label", error)
    null
  }

@Serializable
private data class LastSafeAtUpdate(
  @SerialName("last_safe_at") val lastSafeAt: String?,
)

@Serializable
private data class StatusUpdate(
  val status: CircleStatus,
)

@Serializable
private data class CircleMemberStatusUpdate(
  val status: CircleStatus,
  @SerialName("member_user_id") val memberUserId: String?,
)

@Serializable
private data class NotificationReadUpdate(
  @SerialName("is_read") val isRead: Boolean,
)

@Serializable
private data class DeviceTokenActiveUpdate(
  @SerialName("is_active") val isActive: Boolean,
)
