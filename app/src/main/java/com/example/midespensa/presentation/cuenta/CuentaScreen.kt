package com.example.midespensa.presentation.cuenta

import android.app.TimePickerDialog
import android.content.Context
import android.util.Log
import android.widget.NumberPicker
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.midespensa.ui.theme.GreenBack
import com.example.midespensa.ui.theme.LightGraySub
import com.example.midespensa.ui.theme.RedCancel

import com.example.midespensa.presentation.components.HeaderSection
import com.example.midespensa.presentation.components.BottomSection

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.midespensa.ui.theme.DarkGray
import com.example.midespensa.ui.theme.GreenConfirm

import com.example.midespensa.notifications.NotificationWorker
import com.example.midespensa.notifications.WorkScheduler
import java.util.concurrent.TimeUnit


@Composable
fun CuentaScreen(navController: NavController, viewModel: CuentaViewModel = viewModel()) {
    val user = viewModel.user
    // Si no encuentra una foto en el user establece una por defecto
    val nombreUsuario by viewModel.nombre.collectAsState()
    val apellidosUsuario by viewModel.apellidos.collectAsState()

    // GESTIÓN DE LA ACCIÓN DE VOLVER A ATRÁS
    var mostrarDialogoSalir by remember { mutableStateOf(false) }

        // Interceptar botón "atrás" del sistema
        BackHandler {
            mostrarDialogoSalir = true
        }
        if (mostrarDialogoSalir) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoSalir = false },
                title = { Text("Confirmar cierre") },
                text = { Text("¿Estás seguro de que quieres cerrar sesión?") },
                confirmButton = {
                    TextButton(onClick = {
                        mostrarDialogoSalir = false
                        viewModel.logout {
                            navController.navigate("login") {
                                popUpTo("inicio") { inclusive = true }
                            }
                        }
                    }) {
                        Text("Cerrar sesión")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoSalir = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

    val context = LocalContext.current

    // Imagen de perfil reactiva
    val profileImageUrl by viewModel.profileImageUrl.collectAsState()
    val profileImage = profileImageUrl ?: "https://ui-avatars.com/api/?name=${nombreUsuario}&background=random"

    // Launcher para seleccionar imagen desde la galería
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                viewModel.subirImagen(it,
                    onSuccess = {
                        Toast.makeText(context, "Imagen actualizada", Toast.LENGTH_SHORT).show()
                    },
                    onError = {
                        Toast.makeText(context, "Error al subir imagen", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    )

    // Estado para la hora seleccionada
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var hour by remember { mutableStateOf(prefs.getInt("notif_hour", 9)) }
    var minute by remember { mutableStateOf(prefs.getInt("notif_minute", 0)) }
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                HeaderSection(title = "Mi Cuenta")
                HorizontalDivider(thickness = 1.5.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 1.5.dp)
                BottomSection(navController = navController)
            }
        },
        containerColor = GreenBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(32.dp, 20.dp, 32.dp, 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(25.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            )
            {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = profileImage // Esto ya incluye imagen subida o avatar por defecto
                    ),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Gray, CircleShape),
                    contentScale = ContentScale.Crop
                )

                // Nombre de usuario y email
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        nombreUsuario.ifBlank { "Sin nombre" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGray
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        apellidosUsuario.ifBlank { "Sin apellidos" },
                        fontSize = 18.sp,
                        color = DarkGray
                    )
                }

            }


            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            )
            {
                Text(
                    "Correo electrónico  ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.Black
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            )
            {
                Text(
                    user?.email ?: "Sin email",
                    fontSize = 17.sp,
                    color = LightGraySub
                )
            }

            Spacer(Modifier.height(25.dp))

            HorizontalDivider(thickness = 2.dp)

            Spacer(Modifier.height(15.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            )
            {
                Text(
                    "Configuración de notificaciones",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.Black
                )
            }
            Spacer(Modifier.height(15.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            )
            {
                Icon(
                    imageVector = Icons.Default.Info, // ← Icono típico de información
                    contentDescription = "Info",
                    tint = LightGraySub,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    "Aquí puedes configurar a qué hora recibir alertas sobre el estado de la caducidad de los productos pertenecientes a las despensas existentes.",
                    fontSize = 15.sp,
                    color = LightGraySub,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.fillMaxWidth()
                )

            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            )
            {
                Text(
                    if(minute in 0..9) "Hora de notificación: $hour:0$minute" else "Hora de notificación: $hour:$minute",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Button(
                    onClick = { showPicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenConfirm),
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                    )
                {
                    Text("Cambiar hora de notificación")
                }

                Spacer(Modifier.height(25.dp))

                Button(
                    onClick = {
                        viewModel.logout {
                            navController.navigate("login") {
                                popUpTo("cuenta") { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedCancel),
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión", color = Color.White, fontSize = 15.sp)
                }

                if (showPicker) {
                    // TimePickerDialog nativo
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            hour = h; minute = m
                            showPicker = false

                            // Guardar en prefs
                            prefs.edit()
                                .putInt("notif_hour", hour)
                                .putInt("notif_minute", minute)
                                .apply()

                            // Programar WorkManager
                            WorkScheduler.scheduleDailyNotification(context, hour, minute)
                            Toast.makeText(
                                context,
                                "Notificación diaria programada con éxito",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        hour,
                        minute,
                        true
                    ).show()
                }
            }


        }
    }
}

