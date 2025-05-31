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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
    var codigoDespensaAbierta by remember { mutableStateOf<String?>(null) }

    var mostrarDialogoSalir by remember { mutableStateOf(false) }
    var showAgregarProductoDialog by remember { mutableStateOf(false) }
    var despensaParaAgregar by remember { mutableStateOf<Despensa?>(null) }

    // Editar info de un producto
    var productoEditando by remember { mutableStateOf<ProductoCompra?>(null) }
    var showEditarDialog by remember { mutableStateOf(false) }

    //Vaciar productos de la lista
    var showVaciarDialog by remember { mutableStateOf(false) }
    var despensaParaVaciar by remember { mutableStateOf<Despensa?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchDespensas()
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
                            popUpTo("inicio") { inclusive = true }
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
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(despensas.count()) { despensa ->
                val isExpanded = codigoDespensaAbierta == despensas.get(despensa).codigo
                val productos = productosMap[despensas.get(despensa).codigo].orEmpty()

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Cabecera de cada despensa
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 18.dp)
                            .clickable {
                                codigoDespensaAbierta =
                                    if (isExpanded) null else despensas.get(despensa).codigo
                            }
                    ) {
                        Text(
                            text = despensas.get(despensa).nombre,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded)
                                Icons.Default.KeyboardArrowUp
                            else
                                Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            // Botones Vaciar / Añadir
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
                                        if(productos.isNotEmpty()){
                                            despensaParaVaciar = despensas.get(despensa)
                                            showVaciarDialog = true
                                        }
                                    }) {
                                        Text("Vaciar")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(
                                        onClick = {
                                            despensaParaAgregar = despensas.get(despensa)
                                            showAgregarProductoDialog = true
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

                            if (productos.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(thickness = 1.dp)
                                Spacer(Modifier.height(8.dp))
                            }

                            val childState = rememberLazyListState()
                            LazyColumn(
                                state = childState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp),  // muestra hasta ~2 ítems sin pasar
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (productos.isEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No se han añadido productos a la lista", color = Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                } else{
                                items(productos.count()) { producto ->
                                        OutlinedCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable {
                                                    productoEditando = productos.get(producto)
                                                    showEditarDialog = true
                                                },
                                            colors = CardDefaults.cardColors(containerColor = Color.White)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        productos.get(producto).nombre,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        "${productos.get(producto).cantidad} ${
                                                            productos.get(
                                                                producto
                                                            ).unidades
                                                        }",
                                                        fontSize = 13.sp
                                                    )
                                                    if (productos.get(producto).detalles.isNotBlank()) {
                                                        Text(
                                                            productos.get(producto).detalles,
                                                            fontSize = 12.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                                IconButton(onClick = {
                                                    viewModel.deleteProducto(
                                                        codigoDespensa = despensas.get(despensa).codigo,
                                                        productoId = productos.get(producto).id
                                                    )
                                                }) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Eliminar"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(thickness = 0.7.dp, color = Color.LightGray)
                }
            }
        }

        // Diálogo de Añadir
        if (showAgregarProductoDialog && despensaParaAgregar != null) {
            AddProductoDialog(
                nombreInicial = productoPreseleccionado.orEmpty(),
                onConfirm = { nombre, cantidad, unidades, detalles ->
                    viewModel.addProducto(
                        codigoDespensa = despensaParaAgregar!!.codigo,
                        nombre = nombre,
                        cantidad = cantidad,
                        unidades = unidades,
                        detalles = detalles
                    )
                    showAgregarProductoDialog = false
                },
                onDismiss = { showAgregarProductoDialog = false }
            )
        }
        // Diálogo de Editar
        if (showEditarDialog && productoEditando != null) {
            EditProductoDialog(
                producto = productoEditando!!,
                onConfirm = { nuevaCantidad, nuevasUnidades, nuevosDetalles ->
                    viewModel.editProducto(
                        despensaCodigo = despensaParaAgregar!!.codigo,
                        productoId = productoEditando!!.id,
                        nuevaCantidad = nuevaCantidad,
                        nuevasUnidades = nuevasUnidades,
                        nuevosDetalles = nuevosDetalles
                    )
                    showEditarDialog = false
                },
                onDismiss = { showEditarDialog = false }
            )
        }
        // Diálogo de Vaciar lista
        if (showVaciarDialog && despensaParaVaciar != null) {
            AlertDialog(
                onDismissRequest = {
                    showVaciarDialog = false
                    despensaParaVaciar = null
                },
                title = { Text("Vaciar lista") },
                text = { Text("¿Estás seguro de que quieres vaciar la lista de “${despensaParaVaciar!!.nombre}”?") },
                confirmButton = {
                    TextButton(onClick = {
                        // Ejecutamos el vaciado
                        viewModel.emptyListaCompra(despensaParaVaciar!!.codigo)
                        showVaciarDialog = false
                        despensaParaVaciar = null
                    }) {
                        Text("Vacíar", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showVaciarDialog = false
                        despensaParaVaciar = null
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun EditProductoDialog(
    producto: ProductoCompra,
    onConfirm: (nuevaCantidad: Int, nuevasUnidades: String, nuevosDetalles: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(producto.nombre ?: "") }
    var cantidad by remember { mutableStateOf(producto.cantidad.toString()) }
    var unidades by remember { mutableStateOf(producto.unidades ?: "") }
    var detalles by remember { mutableStateOf(producto.detalles ?: "") }

    var errorCantidad by remember { mutableStateOf("") }
    var errorUnidades by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar producto") },
        text = {
            Column {

                // Nombre
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { input ->
                        nombre = input.take(30)
                    },
                    label = { Text("Nombre", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // CANTIDAD
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = {
                        if (it.matches(Regex("^\\d{0,5}$")) && it.isNotBlank() && it.toInt() <= 7) cantidad = it
                    },
                    label = { Text("Cantidad", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorCantidad.isNotEmpty()
                )

                Spacer(Modifier.height(8.dp))

                // UNIDADES (opcional)
                OutlinedTextField(
                    value = unidades,
                    onValueChange = { input ->
                        if (input.isNotBlank() && !input.matches(Regex("^[\\p{L}]{1,15}$")) && input.length <= 7) {
                            unidades = input.take(20)
                        }
                    },
                    label = { Text("Unidades (opcional)", color = Color.Gray) },
                    placeholder = { Text("ej: kg", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorUnidades.isNotEmpty()
                )

                Spacer(Modifier.height(8.dp))

                // DETALLES (opcional)
                OutlinedTextField(
                    value = detalles,
                    onValueChange = { detalles = it.take(50) },
                    label = { Text("Detalles (opcional)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cantidadInt = cantidad.toIntOrNull()
                if (cantidadInt == null || cantidadInt <= 0) {
                    errorCantidad = "La cantidad debe ser un número positivo"
                    return@TextButton
                }

                var unidadesFinal = "";
                if (unidades.isBlank() && cantidadInt > 1){
                    unidadesFinal = "unidades"
                } else if (unidades.isBlank() && cantidadInt == 1){
                    unidadesFinal = "unidad"
                } else {
                    unidadesFinal = unidades.trim()
                }

                errorCantidad = ""
                onConfirm(cantidadInt, unidadesFinal, detalles.trim())
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AddProductoDialog(
    nombreInicial: String = "",
    onConfirm: (String, Int, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    var cantidad by remember { mutableStateOf("1") }
    var unidades by remember { mutableStateOf("") }
    var detalles by remember { mutableStateOf("") }
    var errorMensaje by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo producto") },
        text = {
            Column {
                // NOMBRE
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { input ->
                        nombre = input.take(30)
                    },
                    label = { Text("Nombre", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // CANTIDAD
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d{0,10}$"))) cantidad = input.take(7)
                    },
                    label = { Text("Cantidad", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // UNIDADES
                OutlinedTextField(
                    value = unidades,
                    onValueChange = { input ->
                        if (input.matches(Regex("^[\\p{L}]{0,15}$"))) unidades = input.take(20)
                    },
                    label = { Text("Unidades (opcional)", color = Color.Gray) },
                    placeholder = { Text("ej: kg", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // DETALLES
                OutlinedTextField(
                    value = detalles,
                    onValueChange = { input ->
                        detalles = input.take(50) },
                    label = { Text("Detalles (opcional)", color = Color.Gray) },
                    singleLine = true,
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
                if (nombre.isBlank() || cantidadInt == null || cantidadInt <= 0) {
                    errorMensaje = "El nombre y la cantidad son obligatorios."
                    return@TextButton
                }

                var unidadesFinal = "";
                if (unidades.isBlank() && cantidadInt > 1){
                    unidadesFinal = "unidades"
                } else if (unidades.isBlank() && cantidadInt == 1){
                    unidadesFinal = "unidad"
                } else {
                    unidadesFinal = unidades.trim()
                }

                errorMensaje = ""
                onConfirm(nombre.trim(), cantidadInt, unidadesFinal, detalles.trim())
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




