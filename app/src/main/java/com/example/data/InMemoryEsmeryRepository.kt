package com.example.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryEsmeryRepository : EsmeryRepository {
  private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  private var userId = "local-user"

  private val _state = MutableStateFlow(seedState(userId, "Alex Rivers", "alex@example.com"))
  override val state: StateFlow<EsmeryState> = _state.asStateFlow()

  override suspend fun loadForUser(userId: String, email: String?, displayName: String?) {
    this.userId = userId
    _state.value = seedState(userId, displayName ?: "Alex Rivers", email)
  }

  override suspend fun clearLocalSession() {
    userId = "local-user"
    _state.value = seedState(userId, "Alex Rivers", "alex@example.com")
  }

  override suspend fun checkIn(note: String?): CheckIn {
    val now = now()
    val checkIn = CheckIn(id = id(), userId = userId, note = note, createdAt = now)
    val event = TimelineEvent(
      id = id(),
      userId = userId,
      type = TimelineEventType.CheckIn,
      title = "Check-in confirmed",
      body = "Your circle has been notified.",
      relatedEntityId = checkIn.id,
      createdAt = now,
    )
    mutate {
      it.copy(
        profile = it.profile.copy(lastSafeAt = now),
        checkIns = listOf(checkIn) + it.checkIns,
        timelineEvents = listOf(event) + it.timelineEvents,
        circleMembers = it.circleMembers.map { member -> member.copy(lastSafeAt = now) },
      )
    }
    return checkIn
  }

  override suspend fun addFriendRequest(
    contact: String,
    name: String,
    relationship: String,
  ): FriendRequest {
    val now = now()
    val request = FriendRequest(id = id(), senderUserId = userId, receiverContact = contact, createdAt = now)
    val member = CircleMember(
      id = id(),
      ownerUserId = userId,
      invitedContact = contact,
      name = name.ifBlank { contact },
      relationship = relationship.ifBlank { "Trusted contact" },
      status = CircleStatus.Pending,
    )
    val event = TimelineEvent(
      id = id(),
      userId = userId,
      type = TimelineEventType.FriendRequest,
      title = "Circle invitation sent",
      body = "Invitation sent to ${member.name}.",
      relatedEntityId = request.id,
      createdAt = now,
    )
    mutate {
      it.copy(
        friendRequests = listOf(request) + it.friendRequests,
        circleMembers = listOf(member) + it.circleMembers,
        timelineEvents = listOf(event) + it.timelineEvents,
      )
    }
    return request
  }

  override suspend fun updateFriendRequest(requestId: String, status: CircleStatus) {
    val now = now()
    val event = TimelineEvent(
      id = id(),
      userId = userId,
      type = TimelineEventType.FriendRequest,
      title = if (status == CircleStatus.Accepted) "Circle invitation accepted" else "Circle invitation declined",
      body = "Request status updated.",
      relatedEntityId = requestId,
      createdAt = now,
    )
    mutate {
      it.copy(
        friendRequests = it.friendRequests.map { request ->
          if (request.id == requestId) request.copy(status = status) else request
        },
        circleMembers = it.circleMembers.map { member ->
          if (member.status == CircleStatus.Pending) member.copy(status = status) else member
        },
        timelineEvents = listOf(event) + it.timelineEvents,
      )
    }
  }

  override suspend fun sendNudge(memberId: String): TimelineEvent {
    val member = state.value.circleMembers.firstOrNull { it.id == memberId }
    val now = now()
    val event = TimelineEvent(
      id = id(),
      userId = userId,
      type = TimelineEventType.Nudge,
      title = "Gentle nudge sent",
      body = "A gentle reminder was sent to ${member?.name ?: "your circle member"}.",
      relatedEntityId = memberId,
      createdAt = now,
    )
    mutate { it.copy(timelineEvents = listOf(event) + it.timelineEvents) }
    return event
  }

  override suspend fun shareMoment(caption: String, imageUrl: String): Moment {
    val now = now()
    val moment = Moment(id = id(), userId = userId, caption = caption, imageUrl = imageUrl, createdAt = now)
    val event = TimelineEvent(
      id = id(),
      userId = userId,
      type = TimelineEventType.Moment,
      title = "Moment shared",
      body = caption,
      relatedEntityId = moment.id,
      createdAt = now,
    )
    mutate { it.copy(moments = listOf(moment) + it.moments, timelineEvents = listOf(event) + it.timelineEvents) }
    return moment
  }

  override suspend fun saveEmergencyContact(contact: EmergencyContact): EmergencyContact {
    val saved = contact.copy(id = contact.id.ifBlank { id() }, userId = userId)
    val now = now()
    val event = TimelineEvent(
      id = id(),
      userId = userId,
      type = TimelineEventType.Emergency,
      title = "Emergency contact saved",
      body = "${saved.name} is ready for escalation stubs.",
      relatedEntityId = saved.id,
      createdAt = now,
    )
    mutate {
      val remaining = it.emergencyContacts.filterNot { item -> item.id == saved.id }
      it.copy(emergencyContacts = listOf(saved) + remaining, timelineEvents = listOf(event) + it.timelineEvents)
    }
    return saved
  }

  override suspend fun deleteEmergencyContact(contactId: String) {
    mutate { it.copy(emergencyContacts = it.emergencyContacts.filterNot { contact -> contact.id == contactId }) }
  }

  override suspend fun saveSafetyRhythm(rhythm: SafetyRhythm): SafetyRhythm {
    val saved = rhythm.copy(id = rhythm.id.ifBlank { id() }, userId = userId)
    val now = now()
    val event = TimelineEvent(
      id = id(),
      userId = userId,
      type = TimelineEventType.SafetyRhythm,
      title = "Safety rhythm updated",
      body = "${saved.label} at ${saved.checkTime}.",
      relatedEntityId = saved.id,
      createdAt = now,
    )
    mutate {
      val remaining = it.safetyRhythms.filterNot { item -> item.id == saved.id }
      it.copy(safetyRhythms = (listOf(saved) + remaining).sortedBy { item -> item.checkTime }, timelineEvents = listOf(event) + it.timelineEvents)
    }
    return saved
  }

  override suspend fun updateSubscription(plan: SubscriptionPlan): SubscriptionStatus {
    val subscription = SubscriptionStatus(userId = userId, plan = plan, isActive = true)
    mutate { it.copy(subscriptionStatus = subscription, profile = it.profile.copy(isPremium = plan != SubscriptionPlan.Basic)) }
    return subscription
  }

  private fun mutate(block: (EsmeryState) -> EsmeryState) {
    _state.value = block(_state.value)
  }

  private fun seedState(userId: String, displayName: String, email: String?): EsmeryState {
    val morning = "2026-05-27T08:00:00"
    return EsmeryState(
      profile = Profile(id = userId, displayName = displayName, email = email, lastSafeAt = morning),
      circleMembers = listOf(
        CircleMember(id = "member-sarah", ownerUserId = userId, invitedContact = "sarah@example.com", name = "Sarah", relationship = "Best friend", lastSafeAt = morning),
        CircleMember(id = "member-mom", ownerUserId = userId, invitedContact = "+84901234567", name = "Mom", relationship = "Family", lastSafeAt = "2026-05-27T07:30:00"),
      ),
      friendRequests = listOf(
        FriendRequest(id = "request-lucas", senderUserId = userId, receiverContact = "lucas@example.com", createdAt = morning),
      ),
      checkIns = listOf(CheckIn(id = "checkin-seed", userId = userId, createdAt = morning)),
      timelineEvents = listOf(
        TimelineEvent(id = "event-checkin", userId = userId, type = TimelineEventType.CheckIn, title = "Morning check-in", body = "Automatic safety heartbeat sent.", createdAt = morning),
        TimelineEvent(id = "event-moment", userId = userId, type = TimelineEventType.Moment, title = "Moment shared", body = "Morning coffee ritual", createdAt = "2026-05-27T09:15:00"),
      ),
      moments = listOf(
        Moment(id = "moment-coffee", userId = userId, caption = "Morning coffee ritual", imageUrl = PRESET_IMAGES.first(), createdAt = "2026-05-27T09:15:00"),
      ),
      emergencyContacts = listOf(
        EmergencyContact(id = "contact-mom", userId = userId, name = "Mom", contact = "+84901234567", isVerified = true),
      ),
      safetyRhythms = listOf(
        SafetyRhythm(id = "rhythm-wakeup", userId = userId, label = "Wakeup Check", checkTime = "08:00"),
        SafetyRhythm(id = "rhythm-bedtime", userId = userId, label = "Bedtime Check", checkTime = "22:00"),
      ),
      subscriptionStatus = SubscriptionStatus(userId = userId),
    )
  }

  private fun now(): String = LocalDateTime.now().format(formatter)
  private fun id(): String = UUID.randomUUID().toString()
}

val PRESET_IMAGES = listOf(
  "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=800&q=80",
  "https://images.unsplash.com/photo-1513001900722-370f803f498d?auto=format&fit=crop&w=800&q=80",
  "https://images.unsplash.com/photo-1494438639946-1ebd1d20bf85?auto=format&fit=crop&w=800&q=80",
)
