package com.example

import com.example.data.CircleStatus
import com.example.data.InMemoryEsmeryRepository
import com.example.data.SafetyRhythm
import com.example.data.SubscriptionPlan
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}
