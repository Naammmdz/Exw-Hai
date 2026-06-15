package com.example.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun entitlementValidUntil(plan: SubscriptionPlan, from: LocalDateTime = LocalDateTime.now()): String? = when (plan) {
  SubscriptionPlan.Monthly -> from.plusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  SubscriptionPlan.Yearly -> from.plusDays(365).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  SubscriptionPlan.Basic -> null
}

fun planAmountVnd(plan: SubscriptionPlan): Int = when (plan) {
  SubscriptionPlan.Basic -> 0
  SubscriptionPlan.Monthly -> 49_000
  SubscriptionPlan.Yearly -> 499_000
}
