package com.example.data

import com.example.supabase
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

object EsmeryRealtime {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var notificationJob: Job? = null

  suspend fun subscribeNotifications(userId: String, onChange: () -> Unit) {
    stopNotifications()
    supabase.realtime.connect()
    val channel = supabase.channel("notifications-$userId")
    channel.subscribe(blockUntilSubscribed = true)
    notificationJob = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
      table = "notifications"
      filter("user_id", FilterOperator.EQ, userId)
    }.onEach {
      onChange()
    }.launchIn(scope)
  }

  suspend fun stopNotifications() {
    notificationJob?.cancel()
    notificationJob = null
    runCatching {
      supabase.realtime.removeAllChannels()
      supabase.realtime.disconnect()
    }
  }
}
