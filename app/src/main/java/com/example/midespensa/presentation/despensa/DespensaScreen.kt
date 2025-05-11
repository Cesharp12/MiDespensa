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

import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import com.example.midespensa.ui.theme.GreenConfirm
import kotlinx.coroutines.delay


@Composable
fun DespensaHeader(
    viewModel: DespensaViewModel = viewModel(),
    nombre: String,
    codigo: String,
    descripcion: String,
    colorHex: String,
    miembros: List<String>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onLeave: () -> Unit
) {
    val context = LocalContext.current
    val backgroundColor = Color(android.graphics.Color.parseColor(colorHex.ifBlank { "#FFCC00" })) // por defecto amarillo
    val chevronIcon = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    // Gestor para copiar al portapapeles
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

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

            IconButton(onClick = {
                clipboardManager.setText(AnnotatedString(codigo))
                Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
            }) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar código")
            }

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
                        onValueChange = { if (it.length <= 40) nuevaDescripcion = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Editar descripción") }
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
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onLeave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedCancel,
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Abandonar despensa")
                }
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
        "#80DEEA"  // Celeste clarito
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
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showAddToCompraDialog by remember { mutableStateOf(false) }

    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }
    var productoAComprar by remember { mutableStateOf<Producto?>(null) }

    var searchQuery by remember { mutableStateOf("") }

    var selectedProducto by remember { mutableStateOf<Producto?>(null) }

    // SnackBar tip eliminar
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = "Desliza un producto para eliminarlo",
                withDismissAction = false,
                duration = SnackbarDuration.Indefinite
            )
        }

        progress = 0f
        while (progress < 1f) {
            progress += 0.005f
            delay(17)
        }

        // Ahora que la barra terminó, descartamos el snackbar
        snackbarHostState.currentSnackbarData?.dismiss()
    }

    // Evento para cargar los productos al entrar a la página
    LaunchedEffect(key1 = codigoDespensa) {
        viewModel.cargarDespensa(codigoDespensa)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier
                        .padding(15.dp)
                        .background(Color(0xFFEDF2FB), RoundedCornerShape(12.dp)),
                    containerColor = Color(0xFFBBDEFB),
                    contentColor = Color(0xFF0D47A1)
                ) {
                    Column(
                        modifier = Modifier
                        .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = Color(0xFF0D47A1),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Desliza un producto para eliminarlo")
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(50)),
                            color = Color(0xFF0D47A1),
                        )
                    }
                }
            }
        },
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
                onToggleExpand = { isExpanded = !isExpanded },
                onLeave = { showLeaveDialog = true }
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
                        onAgregarListaCompra = { prod ->
                            productoAComprar = prod
                            showAddToCompraDialog = true
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    // EDITAR PRODUCTO
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

    // CREAR PRODUCTO
    if (showCreateDialog) {
        CrearProductoDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { nombre, cantidad, unidad, fechaCaducidad ->
                viewModel.crearProducto(nombre, cantidad, unidad, fechaCaducidad)
                showCreateDialog = false
            }
        )
    }

    // ELIMINAR PRODUCTO
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
    // ABANDONAR DESPENSA
    if (showLeaveDialog) {
        AbandonarDespensaDialog(
            despensaNombre = nombreDespensa,
            onConfirm = {
                showLeaveDialog = false
                viewModel.abandonarDespensa(codigoDespensa)
                navController.popBackStack()
            },
            onDismiss = {
                showLeaveDialog = false
            }
        )
    }

    //AGREGAR PRODUCTO A LISTA
    if (showAddToCompraDialog && productoAComprar != null) {
        var cantidad by remember { mutableStateOf("1") }
        var unidades by remember { mutableStateOf("") }
        var detalles by remember { mutableStateOf("") }
        var errorCantidad by remember { mutableStateOf("") }
        var errorUnidades by remember { mutableStateOf("") }

        AgregarAListaCompraDialog(
            producto = productoAComprar!!,
            cantidad = cantidad,
            unidades = unidades,
            detalles = detalles,
            onCantidadChange = { cantidad = it },
            onUnidadesChange = { unidades = it },
            onDetallesChange = { detalles = it },
            errorCantidad = errorCantidad,
            errorUnidades = errorUnidades,
            onConfirm = {
                val cantidadFinal = cantidad.toIntOrNull()
                var valid = true

                if (cantidadFinal == null || cantidadFinal <= 0) {
                    errorCantidad = "Introduce una cantidad válida."
                    valid = false
                }

                if (unidades.isNotBlank() && !unidades.matches(Regex("^[\\p{L}]{1,15}$"))) {
                    errorUnidades = "Solo letras (máx. 15)."
                    valid = false
                } else {
                    errorUnidades = ""
                }

                if (!valid) return@AgregarAListaCompraDialog

                val unidadesFinal = if (unidades.isBlank()) "unidades" else unidades.trim()

                viewModel.agregarProductoListaCompra(
                    producto = productoAComprar!!,
                    cantidadAReponer = cantidadFinal,
                    unidades = unidadesFinal,
                    detalles = detalles.trim()
                )

                showAddToCompraDialog = false
            },
            onDismiss = { showAddToCompraDialog = false }
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

                if(producto.caducidad.isNotBlank()){
                    Text(
                        text = "Caduca: ${producto.caducidad}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

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

                IconButton(onClick = { onAgregarListaCompra(producto) }) {
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetX.value > swipeThresholdPx) {
                                onDelete(producto) // Llama al padre, no abras diálogo aquí
                                offsetX.snapTo(0f) // Resetea swipe
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
                onAgregarListaCompra = onAgregarListaCompra
            )
        }
    }
}

@Composable
fun EditarProductoDialog(
    producto: Producto,
    onConfirmEdit: (String, Int, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(producto.nombre) }
    var cantidadInput by remember { mutableStateOf(producto.cantidad.toString()) }
    var unidad by remember { mutableStateOf(producto.unidad) }
    var fechaCaducidad by remember { mutableStateOf(producto.caducidad) }

    var errorMensaje by remember { mutableStateOf("") }

    fun esFechaValida(fecha: String): Boolean {
        return try {
            if (fecha.isBlank()) return true
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formato.isLenient = false
            formato.parse(fecha) != null
        } catch (e: Exception) {
            false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar producto") },
        text = {
            Column {
                // NOMBRE
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // CANTIDAD
                OutlinedTextField(
                    value = cantidadInput,
                    onValueChange = {
                        if (it.length <= 7 && it.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                            cantidadInput = it
                        }
                    },
                    label = { Text("Cantidad", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // UNIDAD (opcional)
                OutlinedTextField(
                    value = unidad,
                    onValueChange = { unidad = it },
                    label = { Text("Unidad (ej: kg)", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // FECHA (opcional)
                OutlinedTextField(
                    value = fechaCaducidad,
                    onValueChange = { fechaCaducidad = it },
                    label = { Text("Fecha caducidad (dd/MM/yyyy)", color = Color.Gray) },
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
                val cantidad = cantidadInput.toIntOrNull()
                if (nombre.isBlank() || cantidad == null || cantidad <= 0) {
                    errorMensaje = "El nombre y la cantidad son obligatorios y válidos."
                    return@TextButton
                }

                if (!esFechaValida(fechaCaducidad)) {
                    errorMensaje = "La fecha debe tener un formato válido (dd/MM/yyyy)."
                    return@TextButton
                }

                val unidadFinal = if (unidad.isBlank()) "unidades" else unidad.trim()
                val fechaFinal = fechaCaducidad.trim()

                errorMensaje = ""
                onConfirmEdit(nombre.trim(), cantidad, unidadFinal, fechaFinal)
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
    var cantidadInput by remember { mutableStateOf("1") }
    var unidad by remember { mutableStateOf("") } // ← ahora empieza vacía
    var fechaCaducidad by remember { mutableStateOf("") }

    var errorMensajeBottom by remember { mutableStateOf("") }
    var errorMensajeNombre by remember { mutableStateOf("") }

    val maxNombre = 30

    fun esFechaValida(fecha: String): Boolean {
        return try {
            if (fecha.isBlank()) return true // ← si está en blanco, es válida
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formato.isLenient = false
            formato.parse(fecha) != null
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
                    label = { Text("Nombre", color = Color.Gray) },
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
                        if (input.length <= 7 && input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                            cantidadInput = input
                        }
                    },
                    label = { Text("Cantidad", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // UNIDAD (opcional)
                OutlinedTextField(
                    value = unidad,
                    onValueChange = {
                        unidad = it
                    },
                    label = { Text("Unidad (ej: kg)", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // FECHA (opcional)
                OutlinedTextField(
                    value = fechaCaducidad,
                    onValueChange = { fechaCaducidad = it },
                    label = { Text("Fecha caducidad (dd/MM/yyyy)", color = Color.Gray) },
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

                if (nombre.isBlank() || cantidadNum <= 0.0) {
                    errorMensajeBottom = "El nombre y la cantidad son obligatorios y válidos."
                    return@TextButton
                }

                if (nombre.length > maxNombre) {
                    errorMensajeNombre = "El nombre es demasiado largo (máx. $maxNombre)."
                    return@TextButton
                }

                if (!esFechaValida(fechaCaducidad)) {
                    errorMensajeBottom = "La fecha debe tener un formato válido (dd/MM/yyyy)."
                    return@TextButton
                }

                val unidadFinal = if (unidad.isBlank()) "unidades" else unidad.trim()
                val fechaFinal = fechaCaducidad.trim()

                errorMensajeBottom = ""
                errorMensajeNombre = ""

                onConfirm(nombre.trim(), cantidadNum.toInt(), unidadFinal, fechaFinal)
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
fun AgregarAListaCompraDialog(
    producto: Producto,
    cantidad: String,
    unidades: String,
    detalles: String,
    onCantidadChange: (String) -> Unit,
    onUnidadesChange: (String) -> Unit,
    onDetallesChange: (String) -> Unit,
    errorCantidad: String,
    errorUnidades: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir a lista de compra") },
        text = {
            Column {
                Text("Producto: ${producto.nombre}")
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = cantidad,
                    onValueChange = onCantidadChange,
                    label = { Text("Cantidad a reponer", color = Color.Gray) },
                    isError = errorCantidad.isNotEmpty(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorCantidad.isNotEmpty()) {
                    Text(errorCantidad, color = Color.Red, fontSize = 12.sp)
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = unidades,
                    onValueChange = onUnidadesChange,
                    label = { Text("Unidades", color = Color.Gray) },
                    isError = errorUnidades.isNotEmpty(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorUnidades.isNotEmpty()) {
                    Text(errorUnidades, color = Color.Red, fontSize = 12.sp)
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = detalles,
                    onValueChange = onDetallesChange,
                    label = { Text("Detalles (opcional)", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
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

@Composable
fun AbandonarDespensaDialog(
    despensaNombre: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abandonar despensa") },
        text = { Text("¿Estás seguro de que quieres abandonar la despensa \"$despensaNombre\"? Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Sí, abandonar")
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



