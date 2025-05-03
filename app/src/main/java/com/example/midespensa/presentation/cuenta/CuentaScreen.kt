package com.example.midespensa.presentation.cuenta

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.midespensa.ui.theme.DarkGray


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
                                popUpTo("cuenta") { inclusive = true }
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
                    "Correo electrónico:  ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.Black
                )

                Text(
                    user?.email ?: "Sin email",
                    fontSize = 17.sp,
                    color = LightGraySub
                )
            }

//            Row(
//                modifier = Modifier
//                    .fillMaxWidth(),
//                horizontalArrangement = Arrangement.Start
//            )
//            {
//                Button(
//                    onClick = {
//                        launcher.launch("image/*")
//                    },
//                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
//                    modifier = Modifier
//                        .fillMaxWidth(),
//                    shape = MaterialTheme.shapes.medium
//                ) {
//                    Text("Cambiar foto de perfil")
//                }
//
//            }

            // TODO: Lista de despensas a las que pertenece el usuario

            Spacer(Modifier.height(25.dp))

            HorizontalDivider(thickness = 2.dp)

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
                modifier = Modifier
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

