package com.example.midespensa.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.midespensa.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

/**
 * Worker encargado de comprobar el estado de caducidad de los productos en Firestore y
 * generar notificaciones diarias cuando un producto cambie de estado a "Consumir pronto" o "Caducado".
 *
 * Al ejecutarse, lee las despensas del usuario, compara las fechas y, si corresponde,
 * muestra una notificación personalizada con el nombre de la despensa y el producto.
 * Luego reprograma la ejecución para el día siguiente a la misma hora.
 */
class NotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    // miembro estático
    companion object {
        private const val CHANNEL_ID = "caducidad_channel"
        private const val CHANNEL_NAME = "Alertas de caducidad"
        private const val CHANNEL_DESC = "Notificaciones cuando un producto cambie de estado"
        private const val TAG = "NotifWorker"
    }

    /**
     * Punto de entrada del Worker: crea el canal, consulta Firestore y envía notificaciones.
     *
     * @return Result.success() si se completa, Result.failure() en caso de error o sin usuario.
     */
    override suspend fun doWork(): Result {
        // Asegurarse de que el canal de notificaciones existe
        createNotificationChannel()

        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: run {
                android.util.Log.e(TAG, "No hay usuario logueado → Worker abortado")
                return Result.failure()
            }

        val db = FirebaseFirestore.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = Date()
        val yesterday = Date(today.time - 24 * 60 * 60 * 1000)

        try {
            // Recuperar todas las despensas donde el usuario es miembro
            val despensas = db.collection("despensas")
                .whereArrayContains("miembros", userId)
                .get()
                .await()

            for (despDoc in despensas.documents) {
                val despensaName = despDoc.getString("nombre") ?: "Despensa"

                // Obtener productos de cada despensa
                val products = db.collection("despensas")
                    .document(despDoc.id)
                    .collection("productos")
                    .get()
                    .await()

                for (prodDoc in products.documents) {
                    val productName = prodDoc.getString("nombre") ?: continue
                    val expiryStr = prodDoc.getString("caducidad") ?: continue

                    // Parsear fecha; si falla, saltar este producto
                    val expiryDate = try {
                        dateFormat.parse(expiryStr)
                    } catch (_: Exception) {
                        null
                    } ?: continue

                    // Calcular la diferencia en días entre hoy/ayer y la fecha de caducidad
                    val diffToday = (expiryDate.time - today.time) / (1000 * 60 * 60 * 24)
                    val diffYesterday = (expiryDate.time - yesterday.time) / (1000 * 60 * 60 * 24)

                    // Determinar estados según días restantes
                    val statusToday = when {
                        diffToday < 0 -> "Caducado"
                        diffToday <= 7 -> "Consumir pronto"
                        else -> "Buen estado"
                    }
                    val statusYesterday = when {
                        diffYesterday < 0 -> "Caducado"
                        diffYesterday <= 7 -> "Consumir pronto"
                        else -> "Buen estado"
                    }

                    // Solo notificar si hay cambio a un estado relevante
                    if (statusToday != statusYesterday &&
                        (statusToday == "Consumir pronto" || statusToday == "Caducado")) {

                        val reason = if (statusToday == "Caducado") "ha caducado" else "está a punto de caducar"
                        val content = "$productName $reason"

                        val largeIcon = BitmapFactory.decodeResource(
                            applicationContext.resources,
                            R.drawable.midespensa_notification_icon
                        )

                        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                            .setSmallIcon(R.drawable.notification_small_icon)
                            .setLargeIcon(largeIcon)
                            .setContentTitle(despensaName)
                            .setContentText(content)
                            .setAutoCancel(true)
                            .build()

                        // Verificar permiso de notificaciones antes de notificar
                        val canPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else true

                        if (canPost) {
                            val notifId = Random().nextInt()
                            NotificationManagerCompat.from(applicationContext)
                                .notify(notifId, notification)
                        } else {
                            android.util.Log.w(TAG, "Sin permiso POST_NOTIFICATIONS; omitida para $productName")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al procesar notificaciones", e)
            return Result.failure()
        }

        // Reprogramar la tarea para el día siguiente
        val hour = inputData.getInt("notif_hour", 9)
        val minute = inputData.getInt("notif_minute", 0)
        WorkScheduler.scheduleDailyNotification(applicationContext, hour, minute)

        return Result.success()
    }

    /**
     * Crea el canal de notificaciones si la API lo requiere.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = CHANNEL_DESC }
            manager.createNotificationChannel(channel)
        }
    }
}
