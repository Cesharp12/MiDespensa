package com.example.midespensa.notifications

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationConfig {
    private const val UNIQUE_WORK_NAME = "caducidad_check"

    /** Guarda la hora en prefs y reprogrma el WorkManager diario. */
    fun scheduleDailyWorkerAtHour(context: Context, hour24: Int) {
        Log.d(TAG, "scheduleDailyWorkerAtHour() called with hour24=$hour24") // ← LOG
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putInt("notif_hour", hour24)
            .apply()

        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY,  hour24)
            set(Calendar.MINUTE,       0)
            set(Calendar.SECOND,       0)
            set(Calendar.MILLISECOND,  0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }

        val delayMillis = nextRun.timeInMillis - now.timeInMillis
        val delayMinutes = TimeUnit.MILLISECONDS
            .toMinutes(nextRun.timeInMillis - now.timeInMillis)

        Log.d(TAG, "  now=$now, nextRun=$nextRun, delayMillis=$delayMillis, delayMinutes=$delayMinutes") // ← LOG

        val request = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
            .also { Log.d(TAG, "  enqueued PeriodicWorkRequest with id=${request.id}") }
    }

    /** Llama en onCreate para arrancar con la hora ya guardada (por defecto 9). */
    fun initialize(context: Context) {
        val h = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getInt("notif_hour", 9)
        Log.d(TAG, "initialize() - loaded notif_hour=$h") // ← LOG
        scheduleDailyWorkerAtHour(context, h)
    }
}
