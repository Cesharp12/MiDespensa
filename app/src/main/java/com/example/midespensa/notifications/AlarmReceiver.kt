package com.example.midespensa.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("AlarmReceiver", "🔔 Alarma recibida, disparo NotificationWorker")
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<NotificationWorker>().build())

        // y reprogramo para mañana
        val prefs  = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val hora   = prefs.getInt("hora_notif",   9)
        val minuto = prefs.getInt("minuto_notif", 0)
        AlarmHelper.reprogramarAlarma(context, hora, minuto)
    }
}
