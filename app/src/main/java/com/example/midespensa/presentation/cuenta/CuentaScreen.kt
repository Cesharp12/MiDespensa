package com.example.midespensa.presentation.cuenta

import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.example.midespensa.ui.theme.DarkGray
import com.example.midespensa.ui.theme.GreenConfirm
import java.net.URLEncoder

@Composable
fun CuentaScreen(navController: NavController, viewModel: CuentaViewModel = viewModel()) {
    val user = viewModel.user
    val nombreUsuario by viewModel.nombre.collectAsState()
    val apellidosUsuario by viewModel.apellidos.collectAsState()

    // GESTIÓN DEL BOTÓN ATRÁS
    var mostrarDialogoSalir by remember { mutableStateOf(false) }
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
//    val profileImage = profileImageUrl
//        ?: "https://ui-avatars.com/api/?name=$nombreUsuario&background=random"
    val safeName = URLEncoder.encode(nombreUsuario, "UTF-8")
    val profileImage = profileImageUrl
        ?: "https://ui-avatars.com/api/?name=$safeName&background=random"


    // Estado para la hora seleccionada (se lee inicialmente de SharedPreferences)
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var hour by remember { mutableStateOf(prefs.getInt("notif_hour", 9)) }
    var minute by remember { mutableStateOf(prefs.getInt("notif_minute", 0)) }

    // Flag que indica si el TimePickerDialog ya está en pantalla.
    var isDialogShowing by remember { mutableStateOf(false) }

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
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(25.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = profileImage,
                        ),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Gray, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = if (nombreUsuario.isBlank()) "Sin nombre" else nombreUsuario,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGray
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = if (apellidosUsuario.isBlank()) "Sin apellidos" else apellidosUsuario,
                        fontSize = 18.sp,
                        color = DarkGray
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    "Correo electrónico:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.Black
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    "Configuración de notificaciones",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.Black
                )
            }
            Spacer(Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (minute in 0..9)
                        "Hora de notificación: $hour:0$minute"
                    else
                        "Hora de notificación: $hour:$minute",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
            }

            Spacer(Modifier.height(16.dp))

            // Botón “Cambiar hora de notificación” solo habilitado cuando no haya diálogo abierto
            Button(
                onClick = {
                    if (!isDialogShowing) {
                        // Lanzamos el TimePickerDialog y marcamos que ya está en pantalla
                        val dialog = TimePickerDialog(
                            context,
                            { _, h, m ->
                                // Al aceptar:
                                hour = h
                                minute = m
                                viewModel.scheduleDailyNotification(context, h, m) {
                                    Toast.makeText(
                                        context,
                                        "Notificación diaria programada con éxito",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            hour,
                            minute,
                            true
                        )
                        dialog.setOnDismissListener {
                            // Cuando se cierra el diálogo (OK o Cancel), habilitamos de nuevo el botón
                            isDialogShowing = false
                        }
                        dialog.show()
                        isDialogShowing = true
                    }
                },
                enabled = !isDialogShowing,
                colors = ButtonDefaults.buttonColors(containerColor = GreenConfirm),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Text("Cambiar hora de notificación")
            }

            Spacer(Modifier.height(15.dp))

            Button(
                onClick = {
                    viewModel.logout {
                        navController.navigate("login") {
                            popUpTo("cuenta") { inclusive = true }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedCancel),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}