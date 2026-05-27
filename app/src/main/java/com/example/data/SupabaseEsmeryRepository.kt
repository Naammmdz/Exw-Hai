package com.example.data

import com.example.supabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ResilientEsmeryRepository(
  private val local: InMemoryEsmeryRepository = InMemoryEsmeryRepository(),
  private val remote: SupabaseEsmeryRemoteDataSource = SupabaseEsmeryRemoteDataSource(),
) : EsmeryRepository {
  override val state: StateFlow<EsmeryState> = local.state

  override suspend fun loadForUser(userId: String, email: String?, displayName: String?) {
    local.loadForUser(userId, email, displayName)
    remote.tryRemote {
      upsertProfile(local.state.value.profile)
      upsertSubscription(local.state.value.subscriptionStatus)
    }
  }

  override suspend fun clearLocalSession() {
    local.clearLocalSession()
  }

  override suspend fun checkIn(note: String?): CheckIn {
    val checkIn = local.checkIn(note)
    val state = local.state.value
    remote.tryRemote {
      insertCheckIn(checkIn)
      updateProfileLastSafeAt(state.profile.id, state.profile.lastSafeAt)
      insertTimelineEvent(state.timelineEvents.first())
    }
    return checkIn
  }

  override suspend fun addFriendRequest(contact: String, name: String, relationship: String): FriendRequest {
    val request = local.addFriendRequest(contact, name, relationship)
    val state = local.state.value
    remote.tryRemote {
      insertFriendRequest(request)
      upsertCircleMember(state.circleMembers.first())
      insertTimelineEvent(state.timelineEvents.first())
    }
    return request
  }

  override suspend fun updateFriendRequest(requestId: String, status: CircleStatus) {
    local.updateFriendRequest(requestId, status)
    remote.tryRemote { updateFriendRequestStatus(requestId, status) }
  }

  override suspend fun sendNudge(memberId: String): TimelineEvent {
    val event = local.sendNudge(memberId)
    remote.tryRemote { insertTimelineEvent(event) }
    return event
  }

  override suspend fun shareMoment(caption: String, imageUrl: String): Moment {
    val moment = local.shareMoment(caption, imageUrl)
    val event = local.state.value.timelineEvents.first()
    remote.tryRemote {
      insertMoment(moment)
      insertTimelineEvent(event)
    }
    return moment
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

  override suspend fun saveSafetyRhythm(rhythm: SafetyRhythm): SafetyRhythm {
    val saved = local.saveSafetyRhythm(rhythm)
    val event = local.state.value.timelineEvents.first()
    remote.tryRemote {
      upsertSafetyRhythm(saved)
      insertTimelineEvent(event)
    }
    return saved
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

class SupabaseEsmeryRemoteDataSource(
  private val client: SupabaseClient = supabase,
) {
  suspend fun upsertProfile(profile: Profile) {
    client.from("profiles").upsert(profile)
  }

  suspend fun updateProfileLastSafeAt(userId: String, lastSafeAt: String?) {
    client.from("profiles").update(LastSafeAtUpdate(lastSafeAt)) {
      filter { eq("id", userId) }
    }
  }

  suspend fun upsertCircleMember(member: CircleMember) {
    client.from("circle_members").upsert(member)
  }

  suspend fun insertFriendRequest(request: FriendRequest) {
    client.from("friend_requests").insert(request)
  }

  suspend fun updateFriendRequestStatus(requestId: String, status: CircleStatus) {
    client.from("friend_requests").update(StatusUpdate(status)) {
      filter { eq("id", requestId) }
    }
  }

  suspend fun insertCheckIn(checkIn: CheckIn) {
    client.from("check_ins").insert(checkIn)
  }

  suspend fun insertTimelineEvent(event: TimelineEvent) {
    client.from("timeline_events").insert(event)
  }

  suspend fun insertMoment(moment: Moment) {
    client.from("moments").insert(moment)
  }

  suspend fun upsertEmergencyContact(contact: EmergencyContact) {
    client.from("emergency_contacts").upsert(contact)
  }

  suspend fun deleteEmergencyContact(contactId: String) {
    client.from("emergency_contacts").delete {
      filter { eq("id", contactId) }
    }
  }

  suspend fun upsertSafetyRhythm(rhythm: SafetyRhythm) {
    client.from("safety_rhythms").upsert(rhythm)
  }

  suspend fun upsertSubscription(subscription: SubscriptionStatus) {
    client.from("subscription_status").upsert(subscription)
  }

  suspend fun tryRemote(block: suspend SupabaseEsmeryRemoteDataSource.() -> Unit) {
    runCatching { block() }
  }
}

@Serializable
private data class LastSafeAtUpdate(
  @SerialName("last_safe_at") val lastSafeAt: String?,
)

@Serializable
private data class StatusUpdate(
  val status: CircleStatus,
)
