package com.example.midespensa.presentation.despensa

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.midespensa.R
import com.example.midespensa.data.model.Producto
import com.example.midespensa.presentation.components.BottomSection
import com.example.midespensa.presentation.components.HeaderSection
import com.example.midespensa.ui.theme.DarkGray
import com.example.midespensa.ui.theme.GreenBack
import com.example.midespensa.ui.theme.RedCancel
import com.example.midespensa.ui.theme.estadoBueno
import com.example.midespensa.ui.theme.estadoCaducado
import com.example.midespensa.ui.theme.estadoDeterioro
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt


@Composable
fun DespensaHeader(
    viewModel: DespensaViewModel = viewModel(),
    nombre: String,
    codigo: String,
    descripcion: String,
    colorHex: String,
    miembros: List<String>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val context = LocalContext.current
    val backgroundColor = Color(android.graphics.Color.parseColor(colorHex.ifBlank { "#FFCC00" })) // por defecto amarillo
    val chevronIcon = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Código de despensa: $codigo",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleExpand) {
                Icon(imageVector = chevronIcon, contentDescription = "Expandir")
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                var editando by remember { mutableStateOf(false) }
                var nuevaDescripcion by remember { mutableStateOf(descripcion) }

                if (editando) {
                    OutlinedTextField(
                        value = nuevaDescripcion,
                        onValueChange = { nuevaDescripcion = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Editar descripción") },

                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Button(
                            shape = MaterialTheme.shapes.medium,
                            onClick = {
                            viewModel.actualizarDescripcion(nuevaDescripcion) { success ->
                                if (success) editando = false ; Toast.makeText(context, "Descripción actualizada", Toast.LENGTH_SHORT).show()
                            }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B5E20), // verde oscuro
                                contentColor = Color.White
                            )) {
                            Text("Guardar")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { editando = false },
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFB00020), // rojo oscuro
                                contentColor = Color.White
                            )
                            ) {
                            Text("Cancelar")
                        }
                    }
                } else {
                    Text(descripcion)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        shape = MaterialTheme.shapes.medium,
                        onClick = {
                        nuevaDescripcion = descripcion
                        editando = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF2E2E2E), // negro clarito
                            contentColor = Color.White
                        )) {
                        Text("Editar descripción")
                    }
                }
                Spacer(Modifier.height(10.dp))
                ColorPickerSection(
                    // Error en colorDespensa
                    currentColorHex = colorHex,
                    onColorSelected = { nuevoColor ->
                        viewModel.actualizarColorDespensa(nuevoColor)
                    }
                )

                Spacer(Modifier.height(12.dp))
                Text("Miembros:", fontWeight = FontWeight.Bold)
                miembros.forEach {
                    Text("• $it")
                }
                Spacer(Modifier.height(6.dp))
            }
        }

    }
}

@Composable
fun ColorPickerSection(
    currentColorHex: String,
    onColorSelected: (String) -> Unit
) {
    val coloresDisponibles = listOf(
        "#FFF59D", // Amarillo clarito
        "#FFD180", // Naranja clarito
        "#A5D6A7", // Verde clarito
        "#90CAF9", // Azul clarito
        "#CE93D8", // Lila clarito
        "#EF9A9A", // Rojo clarito
        "#80DEEA"  // Nuevo: Celeste clarito
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        coloresDisponibles.forEach { colorHex ->
            val color = Color(android.graphics.Color.parseColor(colorHex))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color, shape = CircleShape)
                    .border(
                        width = if (colorHex == currentColorHex) 3.dp else 1.dp,
                        color = if (colorHex == currentColorHex) Color.Black else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorHex) }
            )
        }
    }
}

@Composable
fun DespensaScreen(navController: NavController, viewModel: DespensaViewModel = viewModel(), codigoDespensa: String) {
    val focusManager = LocalFocusManager.current

    val nombreDespensa by viewModel.nombreDespensa.collectAsState()
    val descripcionDespensa by viewModel.descripcionDespensa.collectAsState()
    val colorDespensa by viewModel.colorDespensa.collectAsState()
    val miembros by viewModel.miembros.collectAsState()
    val productos by viewModel.productos.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditarDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }
    var productoAAgregar by remember { mutableStateOf<Producto?>(null) }

    var searchQuery by remember { mutableStateOf("") }

    var selectedProducto by remember { mutableStateOf<Producto?>(null) }

    LaunchedEffect(key1 = codigoDespensa) {
        viewModel.cargarDespensa(codigoDespensa)
    }

    Scaffold(
        topBar = {
            Column {
                HeaderSection(title = nombreDespensa)
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
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
        ) {
            DespensaHeader(
                nombre = nombreDespensa,
                codigo = codigoDespensa,
                descripcion = descripcionDespensa,
                colorHex = colorDespensa,
                miembros = miembros,
                isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tus productos",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGray),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("CREAR", fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(15.dp))
                    HorizontalDivider(thickness = 2.dp)
                    Spacer(Modifier.height(15.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar...") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        singleLine = true,

                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EstadoChip("Buen estado", estadoBueno)
                        EstadoChip("Consumir pronto", estadoDeterioro)
                        EstadoChip("Caducado", estadoCaducado)
                    }
                    Spacer(Modifier.height(12.dp))

                }

                val filtrados = productos.filter { it.nombre.contains(searchQuery, ignoreCase = true) }
                items(filtrados) { producto ->
                    ProductoItemSwipeable(
                        producto = producto,
                        onDelete = {
                            productoAEliminar = it
                            showDeleteDialog = true
                        },
                        onConsumirUnidad = { prod -> viewModel.consumirUnidadProducto(prod, 1) },
                        onReponerUnidad = { prod -> viewModel.reponerUnidadProducto(prod, 1) },
                        onEditClick = { prod ->
                            selectedProducto = prod
                            showEditarDialog = true
                        },
                        onAgregarListaCompra = {
                            productoAAgregar = it
                            showDeleteDialog = true
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    if (showEditarDialog && selectedProducto != null) {
        EditarProductoDialog(
            producto = selectedProducto!!,
            onConfirmEdit = { nombre, cantidad, unidad, caducidad ->
                viewModel.actualizarProducto(selectedProducto!!, nombre, cantidad, unidad, caducidad)
                showEditarDialog = false
            },
            onDismiss = { showEditarDialog = false }
        )
    }

    if (showCreateDialog) {
        CrearProductoDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { nombre, cantidad, unidad, fechaCaducidad ->
                viewModel.crearProducto(nombre, cantidad, unidad, fechaCaducidad)
                showCreateDialog = false
            }
        )
    }

    if (showDeleteDialog && productoAEliminar != null) {
        DeleteProductoDialog(
            producto = productoAEliminar!!,
            onConfirmDelete = {
                viewModel.eliminarProducto(productoAEliminar!!)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

}

@Composable
fun ProductoItem(
    producto: Producto,
    onConsumirUnidad: (Producto) -> Unit,
    onReponerUnidad: (Producto) -> Unit,
    onEditClick: (Producto) -> Unit,
    onAgregarListaCompra: (Producto) -> Unit,
    onDeleteProducto: (Producto) -> Unit
) {
    val colorFondo = when (producto.estado) {
        "Buen estado" -> estadoBueno
        "Consumir pronto" -> estadoDeterioro
        "Caducado" -> estadoCaducado
        else -> Color.LightGray
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEditClick(producto) },
        colors = CardDefaults.cardColors(containerColor = colorFondo)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Texto
            Column(
                modifier = Modifier
                    .weight(1f), // Le da espacio flexible
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = producto.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${producto.cantidad} ${producto.unidad}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Caduca: ${producto.caducidad}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Botones
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { onReponerUnidad(producto) }) {
                    Image(
                        painter = painterResource(id = R.drawable.plus_black),
                        contentDescription = "Añadir 1 unidad",
                        modifier = Modifier.size(25.dp),
                        colorFilter = ColorFilter.tint(DarkGray)
                    )
                }

                IconButton(onClick = { onConsumirUnidad(producto) }) {
                    Image(
                        painter = painterResource(id = R.drawable.minus_black),
                        contentDescription = "Eliminar 1 unidad",
                        modifier = Modifier.size(25.dp),
                        colorFilter = ColorFilter.tint(DarkGray)
                    )
                }

                IconButton(onClick = { /* TODO: acción lista de compra */ }) {
                    Image(
                        painter = painterResource(id = R.drawable.compra_black),
                        contentDescription = "Añadir a la lista de compra",
                        modifier = Modifier.size(25.dp),
                        colorFilter = ColorFilter.tint(DarkGray)
                    )
                }
            }
        }
    }

}

@Composable
fun ProductoItemSwipeable(
    producto: Producto,
    onDelete: (Producto) -> Unit,
    onConsumirUnidad: (Producto) -> Unit,
    onReponerUnidad: (Producto) -> Unit,
    onEditClick: (Producto) -> Unit,
    onAgregarListaCompra: (Producto) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 100.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Diálogo de confirmación
    if (showDeleteDialog) {
        DeleteProductoDialog(
            producto = producto,
            onConfirmDelete = {
                onDelete(producto)
                coroutineScope.launch { offsetX.snapTo(0f) }
                showDeleteDialog = false
            },
            onDismiss = {
                coroutineScope.launch {
                    offsetX.animateTo(0f)
                }
                showDeleteDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetX.value > swipeThresholdPx) {
                                showDeleteDialog = true
                            } else {
                                offsetX.animateTo(0f)
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val newOffset = (offsetX.value + dragAmount).coerceAtLeast(0f)
                        coroutineScope.launch {
                            offsetX.snapTo(newOffset)
                        }
                    }
                )
            }
    ) {
        // Fondo rojo con icono
        if (offsetX.value > 5f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RedCancel)
                    .padding(start = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar producto",
                    tint = Color.White
                )
            }
        }

        // Contenido con desplazamiento animado
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(12.dp))
        ) {
            ProductoItem(
                producto = producto,
                onConsumirUnidad = onConsumirUnidad,
                onReponerUnidad = onReponerUnidad,
                onEditClick = onEditClick,
                onDeleteProducto = {}, // El swipe se encarga
                onAgregarListaCompra = {}
            )
        }
    }
}


//@Composable
//fun ProductoItemSwipeable(
//    producto: Producto,
//    onDelete: (Producto) -> Unit,
//    onConsumirUnidad: (Producto) -> Unit,
//    onReponerUnidad: (Producto) -> Unit,
//    onEditClick: (Producto) -> Unit,
//    onAgregarListaCompra: (Producto) -> Unit
//) {
//    val coroutineScope = rememberCoroutineScope()
//    val swipeState = rememberSwipeToDismissBoxState(
//        confirmValueChange = { value ->
//            value != SwipeToDismissBoxValue.StartToEnd // nunca se confirma (previene borrado)
//        }
//    )
//
//
//    var showDeleteDialog by remember { mutableStateOf(false) }
//    var swipeHandled by remember { mutableStateOf(false) }
//    var hasRenderedOnce by remember { mutableStateOf(false) }
//
//    // Esperamos al primer render para evitar alerta al iniciar
//    LaunchedEffect(Unit) {
//        withFrameNanos {
//            hasRenderedOnce = true
//        }
//    }
//
//    LaunchedEffect(swipeState.dismissDirection, swipeState.targetValue) {
//        if (
//            hasRenderedOnce &&
//            swipeState.dismissDirection == SwipeToDismissBoxValue.StartToEnd &&
//            swipeState.targetValue == SwipeToDismissBoxValue.StartToEnd &&
//            !swipeHandled
//        ) {
//            swipeHandled = true
//            showDeleteDialog = true
//        }
//    }
//
//
//    if (showDeleteDialog) {
//        DeleteProductoDialog(
//            producto = producto,
//            onConfirmDelete = {
//                onDelete(producto)
//                coroutineScope.launch { swipeState.reset() }
//                showDeleteDialog = false
//                swipeHandled = false
//            },
//            onDismiss = {
//                coroutineScope.launch { swipeState.reset() }
//                showDeleteDialog = false
//                swipeHandled = false
//            }
//        )
//    }
//
//    SwipeToDismissBox(
//        state = swipeState,
//        enableDismissFromStartToEnd = true,
//        enableDismissFromEndToStart = false,
//        modifier = Modifier.fillMaxWidth(),
//        backgroundContent = {
//            val backgroundColor by animateColorAsState(
//                targetValue = if (swipeState.targetValue == SwipeToDismissBoxValue.StartToEnd)
//                    RedCancel else Color.White,
//                label = "swipe-color"
//            )
//
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .clip(RoundedCornerShape(12.dp))
//                    .background(backgroundColor)
//                    .padding(start = 20.dp),
//                contentAlignment = Alignment.CenterStart
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Delete,
//                    contentDescription = "Eliminar producto",
//                    tint = Color.White
//                )
//            }
//        },
//        content = {
//            ProductoItem(
//                producto = producto,
//                onConsumirUnidad = onConsumirUnidad,
//                onReponerUnidad = onReponerUnidad,
//                onEditClick = onEditClick,
//                onDeleteProducto = {},
//                onAgregarListaCompra = {}
//            )
//        }
//    )
//}

@Composable
fun EditarProductoDialog(
    producto: Producto,
    onConfirmEdit: (String, Int, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(producto.nombre) }
    var cantidad by remember { mutableStateOf(producto.cantidad) }
    var unidad by remember { mutableStateOf(producto.unidad) }
    var fechaCaducidad by remember { mutableStateOf(producto.caducidad) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar producto") },
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
                    value = cantidad.toString(),
                    onValueChange = { cantidad = it.toIntOrNull() ?: 1 },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = unidad,
                    onValueChange = { unidad = it },
                    label = { Text("Unidad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = fechaCaducidad,
                    onValueChange = { fechaCaducidad = it },
                    label = { Text("Fecha caducidad (dd/MM/yy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirmEdit(nombre.trim(), cantidad, unidad.trim(), fechaCaducidad.trim())
            }) {
                Text("Guardar cambios")
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
fun CrearProductoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var cantidadInput by remember { mutableStateOf("1") } // input como string
    var unidad by remember { mutableStateOf("unidad") }
    var fechaCaducidad by remember { mutableStateOf("") }

    var errorMensajeBottom by remember { mutableStateOf("") }
    var errorMensajeNombre by remember { mutableStateOf("") }
    var errorMensajeUnidad by remember { mutableStateOf("") }

    val maxNombre = 30
    val maxUnidad = 15
    val maxCantidadDigits = 10

    fun esFechaValida(fecha: String): Boolean {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formato.isLenient = false
            formato.parse(fecha) != null // Si no lanza excepción, es válida
        } catch (e: Exception) {
            false
        }
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir nuevo producto") },
        text = {
            Column {
                // NOMBRE
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        errorMensajeNombre = if (it.length > maxNombre) {
                            "El nombre no puede superar los $maxNombre caracteres."
                        } else ""
                    },
                    label = { Text("Nombre") },
                    singleLine = true,
                    isError = errorMensajeNombre.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMensajeNombre.isNotEmpty()) {
                    Text(errorMensajeNombre, color = Color.Red, fontSize = 12.sp)
                }

                Spacer(Modifier.height(8.dp))

                // CANTIDAD
                OutlinedTextField(
                    value = cantidadInput,
                    onValueChange = { input ->
                        if (input.length <= maxCantidadDigits && input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                            cantidadInput = input
                        }
                    },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // UNIDAD
                OutlinedTextField(
                    value = unidad,
                    onValueChange = {
                        unidad = it
                        errorMensajeUnidad = if (!it.matches(Regex("^[\\p{L}]{0,$maxUnidad}$"))
                            ) {
                            "Solo se permiten letras (máx. $maxUnidad)"
                        } else ""
                    },
                    label = { Text("Unidad (ej: kg, litros, paquetes...)") },
                    singleLine = true,
                    isError = errorMensajeUnidad.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMensajeUnidad.isNotEmpty()) {
                    Text(errorMensajeUnidad, color = Color.Red, fontSize = 12.sp)
                }

                Spacer(Modifier.height(8.dp))

                // FECHA
                OutlinedTextField(
                    value = fechaCaducidad,
                    onValueChange = { fechaCaducidad = it },
                    label = { Text("Fecha caducidad (dd/MM/yyyy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMensajeBottom.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorMensajeBottom, color = Color.Red)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cantidadNum = cantidadInput.toDoubleOrNull() ?: -1.0

                if (nombre.isBlank() || unidad.isBlank() || cantidadNum <= 0) {
                    errorMensajeBottom = "Completa todos los campos correctamente."
                    return@TextButton
                }
                if (nombre.length > maxNombre) {
                    errorMensajeNombre = "El nombre es demasiado largo (máx. $maxNombre caracteres)."
                    return@TextButton
                }
                if (!unidad.matches(Regex("^[a-zA-Z]{1,$maxUnidad}\$"))) {
                    errorMensajeUnidad = "Unidad inválida. Solo letras (máx. $maxUnidad)."
                    return@TextButton
                }
                if (!esFechaValida(fechaCaducidad)) {
                    errorMensajeBottom = "La fecha debe estar en formato válido y no ser pasada."
                    return@TextButton
                }

                errorMensajeBottom = ""
                errorMensajeNombre = ""
                errorMensajeUnidad = ""

                onConfirm(nombre.trim(), cantidadNum.toInt(), unidad.trim(), fechaCaducidad.trim())
            }) {
                Text("Crear")
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
fun DeleteProductoDialog(
    producto: Producto,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar producto") },
        text = {
            Text("¿Seguro que quieres eliminar '${producto.nombre}' de tu despensa? Esta acción no se puede deshacer.")
        },
        confirmButton = {
            TextButton(onClick = onConfirmDelete) {
                Text("Eliminar", color = Color.Red)
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
fun EstadoChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp)
    }
}



