package com.example.midespensa.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.midespensa.R
import java.util.*

object NotificationTestHelper {

    @SuppressLint("MissingPermission")
    fun mostrarNotiTest(context: Context, despensa: String, producto: String, razon: String) {
        val channelId = "caducidad_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de caducidad",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.midespensa_notification_icon)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.notification_small_icon)
            .setLargeIcon(largeIcon)
            .setContentTitle("MiDespensa • $despensa")
            .setContentText("$producto $razon")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(Random().nextInt(), notification)
    }
}
