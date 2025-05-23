package com.example.midespensa.notifications

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.Calendar

/**
 * Útil para programar una notificación diaria a una hora y minuto específico.
 *
 * Encapsula el cálculo del retraso hasta la próxima ejecución y encola
 * un OneTimeWorkRequest que se reprogramará a sí mismo tras cada ejecución.
 */
object WorkScheduler {
    private const val WORK_NAME = "daily_notification_work"

    /**
     * Programa o reprograma el worker para que se ejecute a la siguiente ocurrencia
     * de la hora y minuto especificados.
     *
     * @param context Contexto de la aplicación usado para obtener WorkManager.
     * @param hour Hora del día en formato 24h a la que debe dispararse la notificación.
     * @param minute Minuto dentro de la hora para la notificación.
     */
    fun scheduleDailyNotification(context: Context, hour: Int, minute: Int) {
        // Obtiene el momento actual y calcula cuándo será la próxima ejecución
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Si la hora ya pasó hoy, apunta al mismo horario mañana
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }

        val delay = next.timeInMillis - now.timeInMillis

        // Datos de entrada para el worker: hora y minuto
        val data = workDataOf(
            "notif_hour" to hour,
            "notif_minute" to minute
        )

        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        // Encola el trabajo de forma única, reemplazando el existente si cambia la configuración
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }
}
