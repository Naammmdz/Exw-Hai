package com.example.automation

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SafetyAutomationScheduler {
  private const val WORK_NAME = "esmery-safety-check"

  fun schedule(context: Context) {
    val request = PeriodicWorkRequestBuilder<SafetyCheckWorker>(2, TimeUnit.HOURS)
      .addTag(WORK_NAME)
      .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      request,
    )
  }
}
