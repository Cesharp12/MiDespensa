package com.example.midespensa

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.midespensa.ui.theme.MiDespensaTheme
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.midespensa.notifications.AlarmHelper
import com.example.midespensa.notifications.NotificationConfig
import com.example.midespensa.notifications.NotificationWorker
import com.google.firebase.auth.FirebaseAuth
import android.provider.Settings
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import com.example.midespensa.notifications.WorkScheduler
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {

    private lateinit var navHostController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pedir permiso de POST_NOTIFICATIONS si Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // 2) Programar la primera ejecución con la hora guardada
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val h = prefs.getInt("notif_hour", 9)
        val m = prefs.getInt("notif_minute", 0)
        WorkScheduler.scheduleDailyNotification(this, h, m)


        enableEdgeToEdge()
        setContent {
            navHostController = rememberNavController()
            MiDespensaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ){
                    val navController = rememberNavController()

                    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
                    val initialRoute = if (isLoggedIn) "inicio" else "login"

                    NavigationWrapper(navHostController = navController, startDestination = initialRoute)
                }
            }
        }
    }
}
