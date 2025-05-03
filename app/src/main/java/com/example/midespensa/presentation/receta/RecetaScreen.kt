package com.example.midespensa.presentation.receta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.midespensa.ui.theme.GreenBack


import com.example.midespensa.presentation.components.HeaderSection
import com.example.midespensa.presentation.components.BottomSection

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext


@Composable
fun RecetaScreen(navController: NavController, viewModel: RecetaViewModel = viewModel()) {
    val user = viewModel.user
    val context = LocalContext.current

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

    Scaffold(
        topBar = {
            Column {
                HeaderSection(title = "Recetas")
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
        )
        {
            Spacer(Modifier.height(25.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            )
            {

            }

        }
    }
}

