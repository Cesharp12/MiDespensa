// NotificationWorker.kt
package com.example.midespensa.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.midespensa.R
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class NotificationWorker(
    appContext: Context,
    params: WorkerParameters
): CoroutineWorker(appContext, params) {

    companion object {
        private const val CHANNEL_ID = "caducidad_channel"
        private const val CHANNEL_NAME = "Alertas de caducidad"
        private const val CHANNEL_DESC = "Notificaciones cuando un producto cambie de estado"
        private const val TAG = "NotifWorker"
    }

    override suspend fun doWork(): Result {
        // 1) Crear canal si hace falta (Android 8+)
        createNotificationChannel()

        // 2) Obtén el userId
        val userId = FirebaseAuth
            .getInstance()
            .currentUser
            ?.uid
            ?: run {
                android.util.Log.e(TAG, "No hay usuario logueado → fracaso")
                return Result.failure()
            }

        val db = FirebaseFirestore.getInstance()
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val hoy = Date()
        val ayer = Date(hoy.time - 24 * 60 * 60 * 1000)

        try {
            // 3) Lee todas las despensas del usuario
            val despensasSnap = db.collection("despensas")
                .whereArrayContains("miembros", userId)
                .get()
                .await()

            for (despDoc in despensasSnap.documents) {
                val despensaNombre = despDoc.getString("nombre") ?: "Despensa"
                val productosSnap = db.collection("despensas")
                    .document(despDoc.id)
                    .collection("productos")
                    .get()
                    .await()

                for (prodDoc in productosSnap.documents) {
                    val nombreProd = prodDoc.getString("nombre") ?: continue
                    val cadStr = prodDoc.getString("caducidad") ?: continue

                    val fechaCad = try {
                        formato.parse(cadStr)
                    } catch (_: Exception) {
                        null
                    } ?: continue

                    // 4) Calcula estados
                    val diasHoy  = (fechaCad.time - hoy.time)  / (1000 * 60 * 60 * 24)
                    val diasAyer = (fechaCad.time - ayer.time) / (1000 * 60 * 60 * 24)

                    val estadoHoy = when {
                        diasHoy <  0 -> "Caducado"
                        diasHoy <= 7 -> "Consumir pronto"
                        else         -> "Buen estado"
                    }
                    val estadoAyer = when {
                        diasAyer <  0 -> "Caducado"
                        diasAyer <= 7 -> "Consumir pronto"
                        else          -> "Buen estado"
                    }

                    // 5) Si cambió a Consumir pronto o Caducado → notifica
                    if (estadoHoy != estadoAyer &&
                        (estadoHoy == "Consumir pronto" || estadoHoy == "Caducado")
                    ) {
                        val razon = if (estadoHoy == "Caducado")
                            "ha caducado"
                        else
                            "está a punto de caducar"

                        val contenido = "$nombreProd $razon"

                        // Construye la notificación
                        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                            .setSmallIcon(R.drawable.notification_small_icon)
                            .setContentTitle(despensaNombre)
                            .setContentText(contenido)
                            .setAutoCancel(true)
                            .build()

                        // 6) Chequea permiso y lanza
                        val canPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }

                        if (canPost) {
                            // usa ID aleatorio para no sobreescribir notificaciones previas
                            val notifId = Random().nextInt()
                            NotificationManagerCompat.from(applicationContext)
                                .notify(notifId, notif)
                        } else {
                            android.util.Log.w(
                                TAG,
                                "Sin permiso POST_NOTIFICATIONS; notificación omitida para $nombreProd"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error en doWork()", e)
            return Result.failure()
        }

        // 7) Re-schedule para mañana
        val hour   = inputData.getInt("notif_hour",   9)
        val minute = inputData.getInt("notif_minute", 0)
        WorkScheduler.scheduleDailyNotification(applicationContext, hour, minute)

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = CHANNEL_DESC }
            mgr.createNotificationChannel(channel)
        }
    }
}
