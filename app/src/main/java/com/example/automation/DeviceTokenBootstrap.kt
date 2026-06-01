package com.example.automation

import com.example.data.EsmeryRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object DeviceTokenBootstrap {
  suspend fun registerFirebaseToken(repository: EsmeryRepository) {
    runCatching {
      val token = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
          .addOnSuccessListener { continuation.resume(it) }
          .addOnFailureListener { continuation.resume("") }
      }
      if (token.isNotBlank()) repository.registerDeviceToken(token)
    }
  }
}
