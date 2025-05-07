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

    fun cargarDespensasUsuario() {
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

    fun agregarProducto(
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

    fun eliminarProducto(codigoDespensa: String, productoId: String) {
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

    fun vaciarListaDespensa(codigoDespensa: String) {
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
