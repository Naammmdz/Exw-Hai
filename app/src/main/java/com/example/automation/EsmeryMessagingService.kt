package com.example.automation

import com.example.EsmeryServices
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EsmeryMessagingService : FirebaseMessagingService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onNewToken(token: String) {
    serviceScope.launch {
      EsmeryServices.repository.registerDeviceToken(token)
    }
  }

  override fun onMessageReceived(message: RemoteMessage) {
    EsmeryNotificationChannels.ensure(this)
    val title = message.notification?.title ?: message.data["title"] ?: "ESMERY"
    val body = message.notification?.body ?: message.data["body"] ?: "You have a new safety update."
    EsmeryNotificationChannels.showSafetyNotification(this, title, body)
  }
}
