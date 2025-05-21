// WorkScheduler.kt
package com.example.midespensa.notifications

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.Calendar

object WorkScheduler {
    private const val WORK_NAME = "daily_notification_work"

    fun scheduleDailyNotification(context: Context, hour: Int, minute: Int) {
        // Calcula milis hasta nextRun
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = next.timeInMillis - now.timeInMillis

        // Prepara datos para el Worker
        val data = workDataOf(
            "notif_hour" to hour,
            "notif_minute" to minute
        )

        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE, // si cambian hora/min, lo reinicia
                request
            )
    }
}
