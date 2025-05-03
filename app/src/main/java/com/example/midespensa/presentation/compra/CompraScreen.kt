package com.example.midespensa.presentation.compra

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import com.example.midespensa.ui.theme.DarkGray


@Composable
fun CompraScreen(navController: NavController, viewModel: CompraViewModel = viewModel(),codigoDespensa: String, productoPreseleccionado: String? = null) {
    val user = viewModel.user
    val context = LocalContext.current

    val productos by viewModel.productosCompra.collectAsState()
    var isExpanded by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }

    // Cargar productos al entrar
    LaunchedEffect(key1 = codigoDespensa) {
        viewModel.cargarListaCompra(codigoDespensa)
    }
    // GESTIÓN DE LA ACCIÓN DE VOLVER A ATRÁS
    var mostrarDialogoSalir by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

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
                HeaderSection(title = "Compras")
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
        )
        {
            Spacer(Modifier.height(25.dp))

            DesplegableListaCompra(
                isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded },
                productos = productos
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DarkGray),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Añadir producto")
            }

            if (showDialog) {
                DialogoAgregarProducto(
                    nombreInicial = productoPreseleccionado.orEmpty(),
                    onConfirm = { nombre, cantidad, unidades, detalles ->
                        viewModel.agregarProducto(nombre, cantidad, unidades, detalles)
                        showDialog = false
                    },
                    onDismiss = { showDialog = false }
                )
            }

        }
    }
}

