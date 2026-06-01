package com.example.automation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.EsmeryServices

class SafetyCheckWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
  override suspend fun doWork(): Result {
    return runCatching {
      EsmeryNotificationChannels.ensure(applicationContext)
      EsmeryServices.repository.refresh()
      val event = EsmeryServices.repository.evaluateMissedCheckIns()
      if (event != null) {
        EsmeryNotificationChannels.showSafetyNotification(
          context = applicationContext,
          title = event.title,
          body = event.body,
        )
      }
      Result.success()
    }.getOrElse {
      Result.retry()
    }
  }
}
