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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.midespensa.data.model.Despensa
import com.example.midespensa.data.model.ProductoCompra
import com.example.midespensa.ui.theme.DarkGray

@Composable
fun CompraScreen(
    navController: NavController,
    viewModel: CompraViewModel = viewModel(),
    productoPreseleccionado: String? = null
) {
    val despensas by viewModel.despensas.collectAsState()
    val productosMap by viewModel.listasCompra.collectAsState()
    val context = LocalContext.current

    var mostrarDialogoSalir by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var despensaParaAgregar by remember { mutableStateOf<Despensa?>(null) }

    val estadosExpansion = remember {
        mutableStateOf<Map<String, Boolean>>(emptyMap())
    }


    LaunchedEffect(Unit) {
        viewModel.cargarDespensasUsuario()
    }

    BackHandler { mostrarDialogoSalir = true }

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
                }) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoSalir = false }) { Text("Cancelar") }
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
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 0.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Top
        ) {

            despensas.forEach { despensa ->
                val isExpanded = estadosExpansion.value[despensa.codigo] ?: false

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp,10.dp)
                            .clickable {
                                val nuevaExpansion = estadosExpansion.value.toMutableMap()
                                nuevaExpansion[despensa.codigo] = !isExpanded
                                estadosExpansion.value = nuevaExpansion
                            }
                    ) {
                        Text(
                            text = despensa.nombre,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 8.dp)) {
                            // Menú buttons y titulo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lista de productos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row {
                                    TextButton(onClick = {
                                        viewModel.vaciarListaDespensa(despensa.codigo)
                                    }) {
                                        Text("Vaciar")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(
                                        onClick = {
                                            despensaParaAgregar = despensa
                                            showDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkGray),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text("Añadir")
                                    }
                                }
                            }

                            val productos = productosMap[despensa.codigo].orEmpty()

                            if (productos.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(thickness = 1.dp)
                                Spacer(Modifier.height(8.dp))
                            }

                            productos.forEach { producto ->
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(producto.nombre, fontWeight = FontWeight.Bold)
                                            Text("${producto.cantidad} ${producto.unidades}", fontSize = 13.sp)
                                            if (producto.detalles.isNotBlank()) {
                                                Text("${producto.detalles}", fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                        IconButton(onClick = {
                                            viewModel.eliminarProducto(despensa.codigo, producto.id)
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                        }
                                    }
                                }
                            }
                        }
                    }

//                    AnimatedVisibility(visible = isExpanded) {
//                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.SpaceBetween
//                            ) {
//                                TextButton(onClick = {
//                                    viewModel.vaciarListaDespensa(despensa.codigo)
//                                }) {
//                                    Text("Vaciar")
//                                }
//                                TextButton(onClick = {
//                                    despensaParaAgregar = despensa
//                                    showDialog = true
//                                }) {
//                                    Text("Añadir", color = Color.White)
//                                }
//                            }
//
//                            productosMap[despensa.codigo]?.forEach { producto ->
//                                OutlinedCard(
//                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
//                                    colors = CardDefaults.cardColors(containerColor = Color.White)
//                                ) {
//                                    Row(
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .padding(12.dp),
//                                        verticalAlignment = Alignment.CenterVertically
//                                    ) {
//                                        Column(modifier = Modifier.weight(1f)) {
//                                            Text(producto.nombre, fontWeight = FontWeight.Bold)
//                                            Text("${producto.cantidad} ${producto.unidades}", fontSize = 13.sp)
//                                            if (producto.detalles.isNotBlank()) {
//                                                Text("${producto.detalles}", fontSize = 12.sp, color = Color.Gray)
//                                            }
//                                        }
//                                        IconButton(onClick = {
//                                            viewModel.eliminarProducto(despensa.codigo, producto.id)
//                                        }) {
//                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.7.dp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(12.dp))
//                Spacer(modifier = Modifier.height(24.dp))
            }

            // Dialogo de agregar producto
            if (showDialog && despensaParaAgregar != null) {
                DialogoAgregarProducto(
                    nombreInicial = productoPreseleccionado.orEmpty(),
                    onConfirm = { nombre, cantidad, unidades, detalles ->
                        viewModel.agregarProducto(
                            codigoDespensa = despensaParaAgregar!!.codigo,
                            nombre = nombre,
                            cantidad = cantidad,
                            unidades = unidades,
                            detalles = detalles
                        )
                        showDialog = false
                    },
                    onDismiss = { showDialog = false }
                )
            }
        }
    }
}

@Composable
fun DesplegableListaCompra(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    productos: List<ProductoCompra>,

) {
    val icon = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF59D)) // Amarillo claro por defecto
            .clip(RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Lista de productos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleExpand) {
                Icon(imageVector = icon, contentDescription = "Expandir")
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                if (productos.isEmpty()) {
                    Text("No hay productos en esta lista", fontSize = 14.sp, color = Color.Gray)
                } else {
                    productos.forEach { producto ->
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(producto.nombre, fontWeight = FontWeight.Bold)
                                Text("${producto.cantidad} ${producto.unidades}", fontSize = 13.sp)
                                if (producto.detalles.isNotBlank()) {
                                    Text(
                                        "Detalles: ${producto.detalles}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Composable: DialogoAgregarProducto
@Composable
fun DialogoAgregarProducto(
    nombreInicial: String = "",
    onConfirm: (String, Int, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    var cantidad by remember { mutableStateOf("1") }
    var unidades by remember { mutableStateOf("unidad") }
    var detalles by remember { mutableStateOf("") }
    var errorMensaje by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo producto") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = {
                        if (it.matches(Regex("^\\d{0,10}\$"))) cantidad = it
                    },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = unidades,
                    onValueChange = {
                        if (it.matches(Regex("^[\\p{L}]{0,15}\$"))) unidades = it
                    },
                    label = { Text("Unidades") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = detalles,
                    onValueChange = { detalles = it },
                    label = { Text("Detalles") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMensaje.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorMensaje, color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cantidadInt = cantidad.toIntOrNull()
                if (nombre.isBlank() || cantidadInt == null || cantidadInt <= 0 || unidades.isBlank()) {
                    errorMensaje = "Todos los campos son obligatorios y válidos."
                    return@TextButton
                }
                errorMensaje = ""
                onConfirm(nombre.trim(), cantidadInt, unidades.trim(), detalles.trim())
            }) {
                Text("Añadir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}


