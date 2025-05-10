package com.example.midespensa.presentation.despensa

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.midespensa.data.model.Producto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DespensaViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val user: FirebaseUser? = auth.currentUser

    private val _nombre = MutableStateFlow("")
    val nombre: StateFlow<String> = _nombre.asStateFlow()

    private val _apellidos = MutableStateFlow("")
    val apellidos: StateFlow<String> = _apellidos.asStateFlow()

    private val _profileImageUrl = MutableStateFlow(user?.photoUrl?.toString())
    val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

    private val _codigoDespensa = MutableStateFlow("")
    val codigoDespensa: StateFlow<String> = _codigoDespensa.asStateFlow()

    private val _nombreDespensa = MutableStateFlow("")
    val nombreDespensa: StateFlow<String> = _nombreDespensa.asStateFlow()

    private val _descripcionDespensa = MutableStateFlow("")
    val descripcionDespensa: StateFlow<String> = _descripcionDespensa.asStateFlow()

    private val _colorDespensa = MutableStateFlow("")
    val colorDespensa: StateFlow<String> = _colorDespensa.asStateFlow()

    private val _miembros = MutableStateFlow<List<String>>(emptyList())
    val miembros: StateFlow<List<String>> = _miembros.asStateFlow()

    private val _productos = MutableStateFlow<List<Producto>>(emptyList())
    val productos: StateFlow<List<Producto>> = _productos.asStateFlow()

    private val _nombreProducto = MutableStateFlow("")
    val nombreProducto: StateFlow<String> = _nombreProducto.asStateFlow()

    private val _estadoProducto = MutableStateFlow("")
    val estadoProducto: StateFlow<String> = _estadoProducto.asStateFlow()

    private val _caducidadProducto = MutableStateFlow("")
    val caducidadProducto: StateFlow<String> = _caducidadProducto.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var despensaId: String? = null

    fun cargarDespensa(codigo: String) {
        db.collection("despensas")
            .whereEqualTo("codigo", codigo)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents.first()
                    despensaId = document.id
                    _codigoDespensa.value = document.getString("codigo") ?: ""
                    _nombreDespensa.value = document.getString("nombre") ?: ""
                    _descripcionDespensa.value = document.getString("descripcion") ?: ""
                    _colorDespensa.value = document.getString("color") ?: ""

                    val codigoMiembros = document.get("miembros") as? List<String> ?: emptyList()
                    val nombres = mutableListOf<String>()
                    codigoMiembros.forEach { uid ->
                        db.collection("usuarios").document(uid).get()
                            .addOnSuccessListener { userDoc ->
                                val nombre = userDoc.getString("nombre") ?: "Usuario"
                                val apellidos = userDoc.getString("apellidos") ?: ""
                                nombres.add("$nombre $apellidos")
                                _miembros.value = nombres.toList()
                            }
                    }

                    cargarProductos()
                }
            }
    }

    private fun cargarProductos() {
        val despensa = despensaId ?: return
        db.collection("despensas").document(despensa).collection("productos")
            .get()
            .addOnSuccessListener { result ->
                val lista = result.map { doc ->
                    Producto(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        cantidad = (doc.getLong("cantidad") ?: 1L).toInt(),
                        unidad = doc.getString("unidad") ?: "unidad",
                        estado = calcularEstadoDesdeFecha(doc.getString("caducidad") ?: ""),
                        caducidad = doc.getString("caducidad") ?: ""
                    )
                }
                _productos.value = lista
            }
            .addOnFailureListener { e ->
                _error.value = "Error al cargar productos"
            }
    }

    fun crearProducto(nombre: String, cantidad: Int, unidad: String, caducidad: String) {
        val despensa = despensaId ?: return

        val estado = calcularEstadoDesdeFecha(caducidad)

        val nuevoProducto = hashMapOf(
            "nombre" to nombre,
            "cantidad" to cantidad,
            "unidad" to unidad,
            "estado" to estado,
            "caducidad" to caducidad
        )

        db.collection("despensas").document(despensa).collection("productos")
            .add(nuevoProducto)
            .addOnSuccessListener {
                cargarProductos()
            }
            .addOnFailureListener {
                _error.value = "Error al crear producto"
            }
    }

    // Nuevo método para consumir varias unidades
    fun consumirUnidadProducto(producto: Producto, cantidadConsumir: Int) {
        val despensa = despensaId ?: return
        val nuevaCantidad = producto.cantidad - cantidadConsumir

        if (nuevaCantidad >= 0) {
            db.collection("despensas").document(despensa)
                .collection("productos").document(producto.id)
                .update("cantidad", nuevaCantidad)
                .addOnSuccessListener { cargarProductos() }
                .addOnFailureListener { _error.value = "Error al consumir unidades" }
        }
    }

    // Nuevo método para reponer unidades
    fun reponerUnidadProducto(producto: Producto, cantidadReponer: Int) {
        val despensa = despensaId ?: return
        val nuevaCantidad = producto.cantidad + cantidadReponer

        db.collection("despensas").document(despensa)
            .collection("productos").document(producto.id)
            .update("cantidad", nuevaCantidad)
            .addOnSuccessListener { cargarProductos() }
            .addOnFailureListener { _error.value = "Error al reponer unidades" }
    }

    fun actualizarProducto(producto: Producto, nombre: String, cantidad: Int, unidad: String, caducidad: String) {
        val despensa = despensaId ?: return

        val estado = calcularEstadoDesdeFecha(caducidad)

        db.collection("despensas").document(despensa)
            .collection("productos").document(producto.id)
            .update(
                mapOf(
                    "nombre" to nombre,
                    "cantidad" to cantidad,
                    "unidad" to unidad,
                    "caducidad" to caducidad,
                    "estado" to estado
                )
            )
            .addOnSuccessListener {
                cargarProductos()
            }
            .addOnFailureListener {
                _error.value = "Error al actualizar producto"
            }
    }

    fun eliminarProducto(producto: Producto) {
        val despensa = despensaId ?: return
        db.collection("despensas").document(despensa)
            .collection("productos").document(producto.id)
            .delete()
            .addOnSuccessListener { cargarProductos() }
            .addOnFailureListener { _error.value = "Error al eliminar producto" }
    }

    fun agregarProductoListaCompra(producto: Producto, cantidadAReponer: Int) {
        // ESTO LO HAREMOS MAS ADELANTE
        // Aquí harías la lógica para añadir el producto a la lista de compra (otra colección en Firestore probablemente)
        Log.d(
            "DespensaViewModel",
            "Añadiendo ${producto.nombre} x$cantidadAReponer a la lista de compra"
        )
    }

    private fun calcularEstadoDesdeFecha(fechaCaducidad: String): String {
        return try {
            val formato = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            val fecha = formato.parse(fechaCaducidad)
            val hoy = Date()
            val diff = fecha.time - hoy.time
            val dias = diff / (1000 * 60 * 60 * 24)

            when {
                dias < 0 -> "Caducado"
                dias <= 7 -> "Consumir pronto"
                else -> "Buen estado"
            }
        } catch (e: Exception) {
            "Buen estado"
        }
    }


    fun actualizarDescripcion(nuevaDescripcion: String, onResult: (Boolean) -> Unit) {
        db.collection("despensas")
            .whereEqualTo("codigo", _codigoDespensa.value)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val docId = result.documents.first().id
                    db.collection("despensas").document(docId)
                        .update("descripcion", nuevaDescripcion)
                        .addOnSuccessListener {
                            _descripcionDespensa.value = nuevaDescripcion
                            onResult(true)
                        }
                        .addOnFailureListener {
                            onResult(false)
                        }
                }
            }
    }

    fun actualizarColorDespensa(colorHex: String) {
        val codigo = _codigoDespensa.value
        if (codigo.isBlank()) return

        db.collection("despensas")
            .whereEqualTo("codigo", codigo)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val docId = result.documents.first().id
                    db.collection("despensas").document(docId)
                        .update("color", colorHex)
                        .addOnSuccessListener {
                            _colorDespensa.value = colorHex
                        }
                }
            }
    }

}