package com.example.midespensa.presentation.despensa

import androidx.lifecycle.ViewModel
import com.example.midespensa.data.model.Producto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DespensaViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _nombre = MutableStateFlow("")
    val nombre: StateFlow<String> = _nombre.asStateFlow()

    private val _codigoDespensa = MutableStateFlow("")

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

    private val _error = MutableStateFlow<String?>(null)

    private var despensaId: String? = null

    /**
     * Carga la información de una despensa por su código y actualiza los estados correspondientes.
     * También carga los miembros y productos asociados a la despensa.
     *
     * @param codigo Código identificador de la despensa.
     */
    fun loadDespensa(codigo: String) {
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

                    fetchProductos()
                }
            }
    }

    /**
     * Obtiene todos los productos de la despensa actual y actualiza el estado con la lista.
     * No retorna valor, pero actualiza `_productos` o `_error`.
     */
    private fun fetchProductos() {
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
                        estado = calculateEstadoProducto(doc.getString("caducidad") ?: ""),
                        caducidad = doc.getString("caducidad") ?: ""
                    )
                }
                _productos.value = lista
            }
            .addOnFailureListener { e ->
                _error.value = "Error al cargar productos"
            }
    }

    /**
     * Crea un nuevo producto en la despensa actual y actualiza la lista al terminar.
     *
     * @param nombre Nombre del producto.
     * @param cantidad Cantidad inicial.
     * @param unidad Unidad de medida.
     * @param caducidad Fecha de caducidad en formato dd/MM/yy.
     */
    fun createProducto(nombre: String, cantidad: Int, unidad: String, caducidad: String) {
        val despensa = despensaId ?: return

        val estado = calculateEstadoProducto(caducidad)

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
                fetchProductos()
            }
            .addOnFailureListener {
                _error.value = "Error al crear producto"
            }
    }

    /**
     * Disminuye la cantidad de un producto en la despensa actual.
     * Solo actualiza si la nueva cantidad es mayor o igual a 0.
     *
     * @param producto Producto al que se le descontará cantidad.
     * @param cantidadConsumir Cantidad a restar.
     */
    fun decreaseCantidadProducto(producto: Producto, cantidadConsumir: Int) {
        val despensa = despensaId ?: return
        val nuevaCantidad = producto.cantidad - cantidadConsumir

        if (nuevaCantidad >= 0) {
            db.collection("despensas").document(despensa)
                .collection("productos").document(producto.id)
                .update("cantidad", nuevaCantidad)
                .addOnSuccessListener { fetchProductos() }
                .addOnFailureListener { _error.value = "Error al consumir unidades" }
        }
    }

    /**
     * Aumenta la cantidad de un producto en la despensa actual.
     *
     * @param producto Producto al que se le sumará cantidad.
     * @param cantidadReponer Cantidad a añadir.
     */
    fun increaseCantidadProducto(producto: Producto, cantidadReponer: Int) {
        val despensa = despensaId ?: return
        val nuevaCantidad = producto.cantidad + cantidadReponer

        db.collection("despensas").document(despensa)
            .collection("productos").document(producto.id)
            .update("cantidad", nuevaCantidad)
            .addOnSuccessListener { fetchProductos() }
            .addOnFailureListener { _error.value = "Error al reponer unidades" }
    }

    /**
     * Actualiza los campos de un producto en la despensa actual.
     *
     * @param producto Producto a modificar.
     * @param nombre Nuevo nombre.
     * @param cantidad Nueva cantidad.
     * @param unidad Nueva unidad.
     * @param caducidad Nueva fecha de caducidad.
     */
    fun updateProducto(
        producto: Producto,
        nombre: String,
        cantidad: Int,
        unidad: String,
        caducidad: String
    ) {
        val despensa = despensaId ?: return

        val estado = calculateEstadoProducto(caducidad)

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
                fetchProductos()
            }
            .addOnFailureListener {
                _error.value = "Error al actualizar producto"
            }
    }

    /**
     * Elimina un producto de la despensa actual.
     *
     * @param producto Producto a eliminar.
     */
    fun deleteProduct(producto: Producto) {
        val despensa = despensaId ?: return
        db.collection("despensas").document(despensa)
            .collection("productos").document(producto.id)
            .delete()
            .addOnSuccessListener { fetchProductos() }
            .addOnFailureListener { _error.value = "Error al eliminar producto" }
    }

    /**
     * Añade un producto a la lista de la compra de la despensa actual.
     *
     * @param producto Producto base.
     * @param cantidadAReponer Cantidad que se desea reponer.
     * @param unidades Unidades asociadas.
     * @param detalles Información adicional.
     */
    fun addProductToListaCompra(
        producto: Producto,
        cantidadAReponer: Int?,
        unidades: String,
        detalles: String
    ) {
        val despensaId = despensaId ?: return
        val nuevo = hashMapOf(
            "nombre" to producto.nombre,
            "cantidad" to cantidadAReponer,
            "unidades" to unidades,
            "detalles" to detalles
        )
        db.collection("despensas")
            .document(despensaId)
            .collection("productosCompra")
            .add(nuevo)
    }

    /**
     * Permite salir de una despensa, eliminando el UID del usuario de su lista de miembros.
     *
     * @param codigo Código identificador de la despensa.
     */
    fun leaveDespensa(codigo: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("despensas")
            .whereEqualTo("codigo", codigo)
            .get()
            .addOnSuccessListener { snap ->
                val doc = snap.documents.firstOrNull() ?: return@addOnSuccessListener
                db.collection("despensas")
                    .document(doc.id)
                    .update("miembros", FieldValue.arrayRemove(uid))
            }
    }

    /**
     * Calcula el estado de un producto (caducado, consumir pronto, buen estado) a partir de la fecha de caducidad.
     *
     * @param fechaCaducidad Fecha en formato dd/MM/yy.
     * @return Estado del producto como cadena.
     */
    private fun calculateEstadoProducto(fechaCaducidad: String): String {
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

    /**
     * Actualiza la descripción de la despensa actual en Firestore.
     *
     * @param nuevaDescripcion Nueva descripción a guardar.
     * @param onResult Callback que indica si la operación fue exitosa.
     */
    fun updateDespensaDescription(nuevaDescripcion: String, onResult: (Boolean) -> Unit) {
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

    /**
     * Cambia el color representativo de la despensa actual.
     *
     * @param colorHex Código hexadecimal del nuevo color.
     */
    fun updateDespensaColor(colorHex: String) {
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