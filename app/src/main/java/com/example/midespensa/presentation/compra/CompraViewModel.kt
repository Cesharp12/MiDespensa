package com.example.midespensa.presentation.compra

import androidx.lifecycle.ViewModel
import com.example.midespensa.data.model.ProductoCompra
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CompraViewModel: ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val user: FirebaseUser? = auth.currentUser

    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }

    private val _productosCompra = MutableStateFlow<List<ProductoCompra>>(emptyList())
    val productosCompra: StateFlow<List<ProductoCompra>> = _productosCompra

    private var despensaIdActual: String? = null

    fun cargarListaCompra(codigoDespensa: String) {
        db.collection("despensas")
            .whereEqualTo("codigo", codigoDespensa)
            .get()
            .addOnSuccessListener { snap ->
                val docId = snap.documents.firstOrNull()?.id ?: return@addOnSuccessListener
                despensaIdActual = docId

                db.collection("despensas").document(docId)
                    .collection("productosCompra")
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            _productosCompra.value = snapshot.documents.mapNotNull { doc ->
                                ProductoCompra(
                                    id = doc.id,
                                    nombre = doc.getString("nombre") ?: "",
                                    cantidad = (doc.getLong("cantidad") ?: 1).toInt(),
                                    unidades = doc.getString("unidades") ?: "",
                                    detalles = doc.getString("detalles") ?: ""
                                )
                            }
                        }
                    }
            }
    }
}