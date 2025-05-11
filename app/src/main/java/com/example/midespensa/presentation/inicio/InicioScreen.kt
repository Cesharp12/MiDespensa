package com.example.midespensa.presentation.inicio

import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.midespensa.data.model.Despensa
import com.example.midespensa.ui.theme.DarkGray
import com.example.midespensa.ui.theme.GreenConfirm

@Composable
fun InicioScreen(navController: NavController, viewModel: InicioViewModel = viewModel()) {
    val user = viewModel.user
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val despensas by viewModel.despensas.collectAsState()
    val errorMsg by viewModel.error.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }

    // GESTIÓN DE LA ACCIÓN DE VOLVER A ATRÁS
    var mostrarDialogoSalir by remember { mutableStateOf(false) }

    // Limitar a 8 la cantidad de despensas
    var showLimitDialog by remember { mutableStateOf(false) }
    var limitDialogMessage by remember { mutableStateOf("") }

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
                HeaderSection(title = "Inicio")
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
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 32.dp, vertical = 20.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // STICKY TOP SECTION (Unirse a despensa)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(15.dp))

                    Text(
                        "Unirse a despensa",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(15.dp))
                    HorizontalDivider(thickness = 2.dp)
                    Spacer(Modifier.height(15.dp))
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it },
                        placeholder = { Text("Código único") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()

                            val code = joinCode.trim().uppercase()
                            val despensaByCodigo = despensas.firstOrNull { it.codigo == code }

                            when {
                                despensas.size >= 8 -> {
                                    limitDialogMessage = "No puedes unirte a más de 8 despensas"
                                    showLimitDialog = true
                                }
                                despensas.any { it.codigo == code } -> {
                                    if (despensaByCodigo != null) {
                                        limitDialogMessage = "Ya perteneces a la despensa '${despensaByCodigo.nombre}'"
                                    }
                                    showLimitDialog = true
                                }
                                else -> {
                                    viewModel.joinDespensa(
                                        code,
                                        onSuccess = {
                                            joinCode = ""
                                            Toast.makeText(context, "¡Nueva despensa añadida!", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { msg ->
                                            Toast.makeText(context, "Error al unirse: $msg", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenConfirm),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("UNIRSE", color = Color.White)
                    }

                    errorMsg?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = Color.Red)
                    }
                }

                Spacer(Modifier.height(25.dp))

                // SCROLLABLE AREA - Tus despensas
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Tus despensas",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        Button(
                            onClick = {
                                if (despensas.size >= 8) {
                                    limitDialogMessage = "No puedes crear más de 8 despensas"
                                    showLimitDialog = true
                                } else {
                                    showCreateDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGray),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("CREAR", fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(thickness = 2.dp)
                    Spacer(Modifier.height(20.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (despensas.isEmpty()) {
                            item {
                                Text("No perteneces a ninguna aún", color = Color.Gray)
                            }
                        } else {
                            items(despensas.count()) { d ->
                                DespensaItem(despensas.get(d)) {
                                    navController.navigate("despensa/${despensas.get(d).codigo}")
                                }
                            }
                        }

                        item { Spacer(Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }

    // — Dialog para crear despensa
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nueva despensa") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        if (it.length <= 22) newName = it
                    },
                    placeholder = { Text("Nombre de la despensa", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createDespensa(newName.trim(),
                        onSuccess = {
                            newName = ""
                            showCreateDialog = false
                        },
                        onFailure = { /* Toast error */ }
                    )
                }) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    // Avisar de que se ha llegado al maximo permitido de despensas
    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text("Error al unirse") },
            text = { Text(limitDialogMessage) },
            confirmButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

}

@Composable
private fun DespensaItem(d: Despensa, onClick: () -> Unit) {

    val backgroundColor = try {
        Log.d("CardColor", "Valor de color: '${d.color}'")
        Color(android.graphics.Color.parseColor(d.color))
    } catch (e: Exception) {
        Color(0xFFE9E9EB) // Fallback si falla el código
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            contentAlignment = Alignment.Center
        ) {
            // Nombre centrado
            Text(
                text = d.nombre,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center)
            )

            // Icono + número a la derecha
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person, // o usa Icons.Filled.Group si tienes más de uno
                    contentDescription = "Usuarios",
                    tint = Color.DarkGray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = d.miembros.size.toString(), // ← Número de participantes
                    color = Color.DarkGray,
                    fontSize = 16.sp
                )
            }
        }
    }
}
