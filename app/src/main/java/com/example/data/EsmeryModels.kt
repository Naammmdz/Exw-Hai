package com.example.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
  val id: String,
  @SerialName("display_name") val displayName: String,
  val email: String? = null,
  val phone: String? = null,
  @SerialName("avatar_url") val avatarUrl: String? = null,
  @SerialName("is_premium") val isPremium: Boolean = false,
  @SerialName("last_safe_at") val lastSafeAt: String? = null,
)

@Serializable
data class CircleMember(
  val id: String,
  @SerialName("owner_user_id") val ownerUserId: String,
  @SerialName("member_user_id") val memberUserId: String? = null,
  @SerialName("invited_contact") val invitedContact: String,
  val name: String,
  val relationship: String,
  val status: CircleStatus = CircleStatus.Accepted,
  @SerialName("last_safe_at") val lastSafeAt: String? = null,
)

@Serializable
enum class CircleStatus {
  @SerialName("pending") Pending,
  @SerialName("accepted") Accepted,
  @SerialName("declined") Declined,
}

@Serializable
data class FriendRequest(
  val id: String,
  @SerialName("sender_user_id") val senderUserId: String,
  @SerialName("receiver_contact") val receiverContact: String,
  val status: CircleStatus = CircleStatus.Pending,
  @SerialName("created_at") val createdAt: String,
)

@Serializable
data class CheckIn(
  val id: String,
  @SerialName("user_id") val userId: String,
  val status: String = "safe",
  val note: String? = null,
  @SerialName("created_at") val createdAt: String,
)

@Serializable
data class TimelineEvent(
  val id: String,
  @SerialName("user_id") val userId: String,
  val type: TimelineEventType,
  val title: String,
  val body: String,
  @SerialName("related_entity_id") val relatedEntityId: String? = null,
  @SerialName("created_at") val createdAt: String,
)

@Serializable
enum class TimelineEventType {
  @SerialName("check_in") CheckIn,
  @SerialName("moment") Moment,
  @SerialName("nudge") Nudge,
  @SerialName("friend_request") FriendRequest,
  @SerialName("safety_rhythm") SafetyRhythm,
  @SerialName("emergency") Emergency,
}

@Serializable
data class Moment(
  val id: String,
  @SerialName("user_id") val userId: String,
  val caption: String,
  @SerialName("image_url") val imageUrl: String,
  val visibility: String = "circle",
  @SerialName("created_at") val createdAt: String,
)

@Serializable
data class EmergencyContact(
  val id: String,
  @SerialName("user_id") val userId: String,
  val name: String,
  val contact: String,
  @SerialName("is_verified") val isVerified: Boolean = false,
  @SerialName("auto_notify") val autoNotify: Boolean = true,
)

@Serializable
data class SafetyRhythm(
  val id: String,
  @SerialName("user_id") val userId: String,
  val label: String,
  @SerialName("check_time") val checkTime: String,
  @SerialName("is_enabled") val isEnabled: Boolean = true,
)

@Serializable
data class SubscriptionStatus(
  @SerialName("user_id") val userId: String,
  val plan: SubscriptionPlan = SubscriptionPlan.Basic,
  @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
enum class SubscriptionPlan {
  @SerialName("basic") Basic,
  @SerialName("monthly") Monthly,
  @SerialName("yearly") Yearly,
}

data class EsmeryState(
  val profile: Profile,
  val circleMembers: List<CircleMember>,
  val friendRequests: List<FriendRequest>,
  val checkIns: List<CheckIn>,
  val timelineEvents: List<TimelineEvent>,
  val moments: List<Moment>,
  val emergencyContacts: List<EmergencyContact>,
  val safetyRhythms: List<SafetyRhythm>,
  val subscriptionStatus: SubscriptionStatus,
)
