package com.example.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object EsmeryRealtime {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var pollingJob: Job? = null

  suspend fun subscribeNotifications(userId: String, onChange: () -> Unit) {
    stopNotifications()
    pollingJob = scope.launch {
      while (isActive) {
        delay(10_000)
        onChange()
      }
    }
  }

  suspend fun stopNotifications() {
    pollingJob?.cancel()
    pollingJob = null
  }
}
