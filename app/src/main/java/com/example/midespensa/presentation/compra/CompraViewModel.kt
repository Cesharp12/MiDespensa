package com.example.midespensa.presentation.compra

import androidx.lifecycle.ViewModel
import com.example.midespensa.data.model.Despensa
import com.example.midespensa.data.model.ProductoCompra
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CompraViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val user: FirebaseUser? = auth.currentUser

    private val _despensas = MutableStateFlow<List<Despensa>>(emptyList())
    val despensas: StateFlow<List<Despensa>> = _despensas.asStateFlow()

    private val _listasCompra = MutableStateFlow<Map<String, List<ProductoCompra>>>(emptyMap())
    val listasCompra: StateFlow<Map<String, List<ProductoCompra>>> = _listasCompra.asStateFlow()

    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }

    /**
     * Recupera todas las despensas a las que pertenece el usuario actual,
     * así como sus productos de compra, y actualiza los estados correspondientes.
     *
     * No devuelve nada. Actualiza los StateFlow internos con los datos obtenidos.
     */
    fun fetchDespensas() {
        val uid = user?.uid ?: return

        db.collection("despensas")
            .whereArrayContains("miembros", uid)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.documents.mapNotNull { doc ->
                    val codigo = doc.getString("codigo") ?: return@mapNotNull null
                    val nombre = doc.getString("nombre") ?: return@mapNotNull null
                    val miembros = doc.get("miembros") as? List<String> ?: emptyList()
                    val color = doc.getString("color") ?: "#E9E9EB"
                    Despensa(codigo, nombre, miembros, color)
                }
                _despensas.value = lista

                lista.forEach { despensa ->
                    val despensaId = result.documents.firstOrNull { it.getString("codigo") == despensa.codigo }?.id
                        ?: return@forEach

                    db.collection("despensas").document(despensaId)
                        .collection("productosCompra")
                        .addSnapshotListener { snapshot, _ ->
                            if (snapshot != null) {
                                val productos = snapshot.documents.mapNotNull { doc ->
                                    ProductoCompra(
                                        id = doc.id,
                                        nombre = doc.getString("nombre") ?: "",
                                        cantidad = (doc.getLong("cantidad") ?: 1).toInt(),
                                        unidades = doc.getString("unidades") ?: "",
                                        detalles = doc.getString("detalles") ?: ""
                                    )
                                }
                                val map = _listasCompra.value.toMutableMap()
                                map[despensa.codigo] = productos
                                _listasCompra.value = map
                            }
                        }
                }
            }
    }

    /**
     * Añade un nuevo producto a la lista de compra de una despensa específica.
     *
     * @param codigoDespensa Código identificador de la despensa.
     * @param nombre Nombre del producto.
     * @param cantidad Cantidad del producto.
     * @param unidades Unidades de medida del producto.
     * @param detalles Información adicional opcional sobre el producto.
     */
    fun addProducto(
        codigoDespensa: String,
        nombre: String,
        cantidad: Int,
        unidades: String,
        detalles: String
    ) {
        db.collection("despensas")
            .whereEqualTo("codigo", codigoDespensa)
            .get()
            .addOnSuccessListener { snap ->
                val despensaId = snap.documents.firstOrNull()?.id ?: return@addOnSuccessListener

                val nuevo = hashMapOf(
                    "nombre" to nombre,
                    "cantidad" to cantidad,
                    "unidades" to unidades,
                    "detalles" to detalles
                )

                db.collection("despensas")
                    .document(despensaId)
                    .collection("productosCompra")
                    .add(nuevo)
            }
    }

    /**
     * Edita los datos de un producto ya existente en una lista de compra.
     *
     * @param despensaCodigo Código de la despensa a la que pertenece el producto.
     * @param productoId ID único del producto a editar.
     * @param nuevaCantidad Nueva cantidad del producto.
     * @param nuevasUnidades Nuevas unidades de medida.
     * @param nuevosDetalles Nuevos detalles u observaciones del producto.
     */
    fun editProducto(
        despensaCodigo: String,
        productoId: String,
        nuevoNombre: String,
        nuevaCantidad: Int,
        nuevasUnidades: String,
        nuevosDetalles: String
    ) {
        db.collection("despensas")
            .whereEqualTo("codigo", despensaCodigo)
            .get()
            .addOnSuccessListener { snap ->
                val despensaId = snap.documents.firstOrNull()?.id ?: return@addOnSuccessListener
                db.collection("despensas")
                    .document(despensaId)
                    .collection("productosCompra")
                    .document(productoId)
                    .update(
                        mapOf(
                            "nombre" to nuevoNombre,
                            "cantidad" to nuevaCantidad,
                            "unidades" to nuevasUnidades,
                            "detalles" to nuevosDetalles
                        )
                    )
                    .addOnSuccessListener {
                        // opcional: aquí podrías volver a llamar a fetchDespensas()
                        // fetchDespensas()
                    }
            }
    }


    /**
     * Elimina un producto específico de la lista de compra de una despensa.
     *
     * @param codigoDespensa Código de la despensa.
     * @param productoId ID del producto que se desea eliminar.
     */
    fun deleteProducto(codigoDespensa: String, productoId: String) {
        db.collection("despensas")
            .whereEqualTo("codigo", codigoDespensa)
            .get()
            .addOnSuccessListener { snap ->
                val despensaId = snap.documents.firstOrNull()?.id ?: return@addOnSuccessListener

                db.collection("despensas")
                    .document(despensaId)
                    .collection("productosCompra")
                    .document(productoId)
                    .delete()
            }
    }

    /**
     * Vacía por completo la lista de productos de compra de una despensa,
     * eliminando todos los documentos de la subcolección correspondiente.
     *
     * @param codigoDespensa Código de la despensa cuya lista se desea vaciar.
     */
    fun emptyListaCompra(codigoDespensa: String) {
        db.collection("despensas")
            .whereEqualTo("codigo", codigoDespensa)
            .get()
            .addOnSuccessListener { snap ->
                val despensaId = snap.documents.firstOrNull()?.id ?: return@addOnSuccessListener

                db.collection("despensas")
                    .document(despensaId)
                    .collection("productosCompra")
                    .get()
                    .addOnSuccessListener { productos ->
                        productos.documents.forEach { doc ->
                            doc.reference.delete()
                        }
                    }
            }
    }
}
