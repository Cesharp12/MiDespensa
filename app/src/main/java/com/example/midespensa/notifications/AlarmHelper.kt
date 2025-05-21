package com.example.midespensa.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.AlarmManagerCompat
import java.util.*

object AlarmHelper {
    fun reprogramarAlarma(context: Context, hora: Int, minuto: Int) {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            // esta es la versión “compatible” pero que bajo permiso exact alarm nativo
            // lanzará SecurityException si no está concedido
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                am,
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pi
            )
            Log.d("AlarmHelper", "Alarma EXACTA programada para: ${cal.time}")
        } catch (sec: SecurityException) {
            Log.w("AlarmHelper", "No permitido exact alarms, uso setExact()", sec)
            // fallback a setExact() (tal vez menos reliably en Doze, pero al menos dispara)
            am.setExact(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pi
            )
        }
    }
}
