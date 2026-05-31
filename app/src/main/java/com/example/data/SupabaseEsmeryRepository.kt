package com.example.data

import com.example.supabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.util.UUID
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ResilientEsmeryRepository(
  private val local: InMemoryEsmeryRepository = InMemoryEsmeryRepository(),
  private val remote: EsmeryRemoteDataSource = SupabaseEsmeryRemoteDataSource(),
) : EsmeryRepository {
  override val state: StateFlow<EsmeryState> = local.state
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
    val id = userId ?: return
    val remoteState = remote.tryFetchState(id, email, displayName)
    if (remoteState != null) {
      local.replaceState(remoteState)
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
      remote.tryFetchState(id, email, displayName)?.let { local.replaceState(it) }
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
    }
    return checkIn
  }

  override suspend fun addFriendRequest(contact: String, name: String, relationship: String): FriendRequest {
    val normalizedContact = contact.normalizedContact()
    val request = local.addFriendRequest(normalizedContact, name, relationship)
    val matchedProfile = remote.tryFindProfileByContact(normalizedContact)
    if (matchedProfile != null) {
      val current = local.state.value
      local.replaceState(
        current.copy(
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
    remote.tryRemote {
      insertFriendRequest(request)
      upsertCircleMember(state.circleMembers.first())
      insertTimelineEvent(state.timelineEvents.first())
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
    }
  }

  override suspend fun sendNudge(memberId: String): TimelineEvent {
    val event = local.sendNudge(memberId)
    remote.tryRemote {
      insertTimelineEvent(event)
      insertNotification(local.state.value.notifications.first())
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
      }
    }
    return event
  }

  override suspend fun triggerEmergencyAlert(): TimelineEvent {
    val event = local.triggerEmergencyAlert()
    remote.tryRemote {
      insertTimelineEvent(event)
      insertNotification(local.state.value.notifications.first())
    }
    return event
  }

  override suspend fun updateSubscription(plan: SubscriptionPlan): SubscriptionStatus {
    val subscription = local.updateSubscription(plan)
    remote.tryRemote {
      upsertSubscription(subscription)
      upsertProfile(local.state.value.profile)
    }
    return subscription
  }
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
    val profile = runCatching {
      client.from("profiles").select {
        filter { eq("id", userId) }
      }.decodeSingleOrNull<Profile>()
    }.getOrNull()
    val resolvedProfile = profile ?: Profile(
      id = userId,
      displayName = displayName ?: normalizedEmail?.substringBefore('@') ?: "ESMERY Friend",
      email = normalizedEmail,
    )

    val sentRequests = runCatching {
      client.from("friend_requests").select {
        filter { eq("sender_user_id", userId) }
      }.decodeList<FriendRequest>()
    }.getOrDefault(emptyList())
    val receivedRequests = normalizedEmail?.takeIf { it.isNotBlank() }?.let { contact ->
      runCatching {
        client.from("friend_requests").select {
          filter { ilike("receiver_contact", contact) }
        }.decodeList<FriendRequest>()
      }.getOrDefault(emptyList())
    }.orEmpty()

    val subscription = runCatching {
      client.from("subscription_status").select {
        filter { eq("user_id", userId) }
      }.decodeSingleOrNull<SubscriptionStatus>()
    }.getOrNull() ?: SubscriptionStatus(userId = userId)

    val settings = runCatching {
      client.from("safety_settings").select {
        filter { eq("user_id", userId) }
      }.decodeSingleOrNull<SafetySettings>()
    }.getOrNull() ?: SafetySettings(userId = userId)

    return EsmeryState(
      profile = resolvedProfile.copy(
        displayName = resolvedProfile.displayName.ifBlank { displayName ?: "ESMERY Friend" },
        email = resolvedProfile.email?.normalizedContact() ?: normalizedEmail,
      ),
      circleMembers = runCatching {
        client.from("circle_members").select {
          filter { eq("owner_user_id", userId) }
        }.decodeList<CircleMember>()
      }.getOrDefault(emptyList()).sortedByDescending { it.lastSafeAt.orEmpty() },
      friendRequests = (sentRequests + receivedRequests).distinctBy { it.id }.sortedByDescending { it.createdAt },
      checkIns = runCatching {
        client.from("check_ins").select {
          filter { eq("user_id", userId) }
        }.decodeList<CheckIn>()
      }.getOrDefault(emptyList()).sortedByDescending { it.createdAt },
      timelineEvents = runCatching {
        client.from("timeline_events").select {
          filter { eq("user_id", userId) }
        }.decodeList<TimelineEvent>()
      }.getOrDefault(emptyList()).sortedByDescending { it.createdAt },
      notifications = runCatching {
        client.from("notifications").select {
          filter { eq("user_id", userId) }
        }.decodeList<EsmeryNotification>()
      }.getOrDefault(emptyList()).sortedByDescending { it.createdAt },
      moments = runCatching {
        client.from("moments").select {
          filter { eq("user_id", userId) }
        }.decodeList<Moment>()
      }.getOrDefault(emptyList()).sortedByDescending { it.createdAt },
      emergencyContacts = runCatching {
        client.from("emergency_contacts").select {
          filter { eq("user_id", userId) }
        }.decodeList<EmergencyContact>()
      }.getOrDefault(emptyList()).sortedBy { it.name },
      safetyRhythms = runCatching {
        client.from("safety_rhythms").select {
          filter { eq("user_id", userId) }
        }.decodeList<SafetyRhythm>()
      }.getOrDefault(emptyList()).sortedBy { it.checkTime },
      safetySettings = settings,
      subscriptionStatus = subscription,
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
}

private suspend fun EsmeryRemoteDataSource.tryRemote(block: suspend EsmeryRemoteDataSource.() -> Unit) {
  runCatching { block() }
}

private suspend fun EsmeryRemoteDataSource.tryFetchState(
  userId: String,
  email: String?,
  displayName: String?,
): EsmeryState? = runCatching { fetchState(userId, email, displayName) }.getOrNull()

private suspend fun EsmeryRemoteDataSource.tryFindProfileByContact(contact: String): Profile? =
  runCatching { findProfileByContact(contact) }.getOrNull()

private suspend fun EsmeryRemoteDataSource.tryFindProfileById(userId: String): Profile? =
  runCatching { findProfileById(userId) }.getOrNull()

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
