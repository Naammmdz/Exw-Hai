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
  @SerialName("receiver_user_id") val receiverUserId: String? = null,
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
  @SerialName("missed_check_in") MissedCheckIn,
  @SerialName("emergency") Emergency,
}

@Serializable
data class EsmeryNotification(
  val id: String,
  @SerialName("user_id") val userId: String,
  val type: NotificationType,
  val title: String,
  val body: String,
  @SerialName("related_entity_id") val relatedEntityId: String? = null,
  @SerialName("is_read") val isRead: Boolean = false,
  @SerialName("created_at") val createdAt: String,
)

@Serializable
enum class NotificationType {
  @SerialName("check_in_success") CheckInSuccess,
  @SerialName("friend_request") FriendRequest,
  @SerialName("gentle_nudge") GentleNudge,
  @SerialName("missed_check_in") MissedCheckIn,
  @SerialName("emergency_alert") EmergencyAlert,
  @SerialName("moment_shared") MomentShared,
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
data class SafetySettings(
  @SerialName("user_id") val userId: String,
  @SerialName("inactivity_hours") val inactivityHours: Int = 4,
  @SerialName("escalation_delay_minutes") val escalationDelayMinutes: Int = 30,
  @SerialName("location_sharing_enabled") val locationSharingEnabled: Boolean = false,
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

@Serializable
data class DeviceToken(
  val id: String,
  @SerialName("user_id") val userId: String,
  val token: String,
  val provider: String = "fcm",
  val platform: String = "android",
  @SerialName("app_version") val appVersion: String? = null,
  @SerialName("is_active") val isActive: Boolean = true,
  @SerialName("created_at") val createdAt: String,
  @SerialName("last_seen_at") val lastSeenAt: String,
)

@Serializable
data class NotificationDelivery(
  val id: String,
  @SerialName("notification_id") val notificationId: String,
  @SerialName("user_id") val userId: String,
  @SerialName("recipient_user_id") val recipientUserId: String? = null,
  @SerialName("recipient_contact") val recipientContact: String? = null,
  val channel: DeliveryChannel = DeliveryChannel.InApp,
  val status: DeliveryStatus = DeliveryStatus.Pending,
  @SerialName("error_message") val errorMessage: String? = null,
  @SerialName("created_at") val createdAt: String,
  @SerialName("updated_at") val updatedAt: String = createdAt,
)

@Serializable
enum class DeliveryChannel {
  @SerialName("in_app") InApp,
  @SerialName("push") Push,
  @SerialName("sms") Sms,
  @SerialName("email") Email,
  @SerialName("call") Call,
}

@Serializable
enum class DeliveryStatus {
  @SerialName("pending") Pending,
  @SerialName("sent") Sent,
  @SerialName("failed") Failed,
  @SerialName("read") Read,
}

@Serializable
data class AlertIncident(
  val id: String,
  @SerialName("user_id") val userId: String,
  val status: AlertIncidentStatus = AlertIncidentStatus.Active,
  val reason: String,
  @SerialName("last_safe_at") val lastSafeAt: String? = null,
  @SerialName("escalation_due_at") val escalationDueAt: String,
  @SerialName("resolved_at") val resolvedAt: String? = null,
  @SerialName("created_at") val createdAt: String,
)

@Serializable
enum class AlertIncidentStatus {
  @SerialName("active") Active,
  @SerialName("escalated") Escalated,
  @SerialName("resolved") Resolved,
  @SerialName("cancelled") Cancelled,
}

@Serializable
data class AlertJob(
  val id: String,
  @SerialName("incident_id") val incidentId: String,
  @SerialName("user_id") val userId: String,
  @SerialName("run_at") val runAt: String,
  val status: AlertJobStatus = AlertJobStatus.Scheduled,
  @SerialName("created_at") val createdAt: String,
)

@Serializable
enum class AlertJobStatus {
  @SerialName("scheduled") Scheduled,
  @SerialName("sent") Sent,
  @SerialName("cancelled") Cancelled,
  @SerialName("failed") Failed,
}

@Serializable
data class LocationShare(
  val id: String,
  @SerialName("user_id") val userId: String,
  @SerialName("incident_id") val incidentId: String? = null,
  val latitude: Double,
  val longitude: Double,
  @SerialName("accuracy_meters") val accuracyMeters: Double? = null,
  val status: LocationShareStatus = LocationShareStatus.Active,
  @SerialName("created_at") val createdAt: String,
  @SerialName("expires_at") val expiresAt: String,
)

@Serializable
enum class LocationShareStatus {
  @SerialName("active") Active,
  @SerialName("expired") Expired,
  @SerialName("revoked") Revoked,
}

@Serializable
data class PaymentOrder(
  val id: String,
  @SerialName("user_id") val userId: String,
  val provider: PaymentProvider,
  val plan: SubscriptionPlan,
  @SerialName("amount_vnd") val amountVnd: Int,
  val status: PaymentOrderStatus = PaymentOrderStatus.Pending,
  @SerialName("checkout_url") val checkoutUrl: String? = null,
  @SerialName("qr_url") val qrUrl: String? = null,
  @SerialName("reference_code") val referenceCode: String,
  @SerialName("created_at") val createdAt: String,
  @SerialName("updated_at") val updatedAt: String = createdAt,
)

@Serializable
enum class PaymentProvider {
  @SerialName("google_play") GooglePlay,
  @SerialName("sepay") SePay,
}

@Serializable
enum class PaymentOrderStatus {
  @SerialName("pending") Pending,
  @SerialName("paid") Paid,
  @SerialName("expired") Expired,
  @SerialName("cancelled") Cancelled,
  @SerialName("failed") Failed,
}

@Serializable
data class Entitlement(
  @SerialName("user_id") val userId: String,
  val plan: SubscriptionPlan = SubscriptionPlan.Basic,
  @SerialName("is_premium") val isPremium: Boolean = false,
  val source: EntitlementSource = EntitlementSource.Basic,
  @SerialName("valid_until") val validUntil: String? = null,
  @SerialName("updated_at") val updatedAt: String,
)

@Serializable
enum class EntitlementSource {
  @SerialName("basic") Basic,
  @SerialName("google_play") GooglePlay,
  @SerialName("sepay") SePay,
  @SerialName("manual") Manual,
}

@Serializable
data class AuditLog(
  val id: String,
  @SerialName("user_id") val userId: String,
  @SerialName("actor_user_id") val actorUserId: String? = null,
  val action: String,
  val metadata: String? = null,
  @SerialName("created_at") val createdAt: String,
)

data class EsmeryState(
  val profile: Profile,
  val circleMembers: List<CircleMember>,
  val friendRequests: List<FriendRequest>,
  val checkIns: List<CheckIn>,
  val timelineEvents: List<TimelineEvent>,
  val notifications: List<EsmeryNotification>,
  val moments: List<Moment>,
  val emergencyContacts: List<EmergencyContact>,
  val safetyRhythms: List<SafetyRhythm>,
  val safetySettings: SafetySettings,
  val subscriptionStatus: SubscriptionStatus,
  val deviceTokens: List<DeviceToken> = emptyList(),
  val notificationDeliveries: List<NotificationDelivery> = emptyList(),
  val alertIncidents: List<AlertIncident> = emptyList(),
  val alertJobs: List<AlertJob> = emptyList(),
  val locationShares: List<LocationShare> = emptyList(),
  val paymentOrders: List<PaymentOrder> = emptyList(),
  val entitlement: Entitlement = Entitlement(
    userId = profile.id,
    plan = subscriptionStatus.plan,
    isPremium = subscriptionStatus.plan != SubscriptionPlan.Basic,
    source = if (subscriptionStatus.plan == SubscriptionPlan.Basic) EntitlementSource.Basic else EntitlementSource.Manual,
    updatedAt = "",
  ),
  val auditLogs: List<AuditLog> = emptyList(),
)
