package com.example.core.entitlement

import com.example.data.EsmeryState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object EntitlementGate {
  const val BASIC_EMERGENCY_CONTACT_LIMIT = 2
  const val BASIC_CIRCLE_MEMBER_LIMIT = 3

  fun isPremiumActive(state: EsmeryState): Boolean {
    val entitlement = state.entitlement
    if (!entitlement.isPremium) return false
    val validUntil = entitlement.validUntil ?: return true
    return runCatching {
      LocalDateTime.parse(validUntil.take(19), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .isAfter(LocalDateTime.now())
    }.getOrDefault(true)
  }

  fun canAddEmergencyContact(state: EsmeryState): Boolean =
    isPremiumActive(state) || state.emergencyContacts.size < BASIC_EMERGENCY_CONTACT_LIMIT

  fun canUseSmartDetection(state: EsmeryState): Boolean = isPremiumActive(state)

  fun circleMemberLimit(state: EsmeryState): Int =
    if (isPremiumActive(state)) Int.MAX_VALUE else BASIC_CIRCLE_MEMBER_LIMIT

  fun canAddCircleMember(state: EsmeryState): Boolean {
    val acceptedCount = state.circleMembers.count { it.status == com.example.data.CircleStatus.Accepted }
    return acceptedCount < circleMemberLimit(state)
  }
}
