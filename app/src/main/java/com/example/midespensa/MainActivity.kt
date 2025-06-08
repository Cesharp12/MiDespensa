package com.example.midespensa

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import com.example.midespensa.notifications.NotificationWorker
import com.google.firebase.auth.FirebaseAuth
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import com.example.midespensa.notifications.WorkScheduler
import com.example.midespensa.presentation.receta.RecetaViewModel
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {

    private lateinit var navHostController: NavHostController

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        super.applyOverrideConfiguration(
            overrideConfiguration?.apply {
                fontScale = 1f
            }
        )
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
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
            var modelsReady by remember { mutableStateOf(false) }
            navHostController = rememberNavController()

            LaunchedEffect(Unit) {
                // Aquí llamas a una función suspend que descarga ambos modelos
                try {
                    RecetaViewModel().downloadAllModels()
                } catch(e: Exception) {
                    Log.e("MainActivity", "No se pudieron descargar modelos", e)
                }
                modelsReady = true
            }

            if (!modelsReady) {
                // Mientras esperas, bloqueo total con spinner
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            // un verde oscuro, por ejemplo MaterialTheme.primary o un hex
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Descargando paquetes",
                            color = Color(0xFF1B5E20),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

            } else {
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
}
