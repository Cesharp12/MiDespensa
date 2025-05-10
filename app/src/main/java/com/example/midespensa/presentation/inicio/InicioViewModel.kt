package com.example.midespensa.presentation.inicio

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.midespensa.data.model.Despensa
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// open para testeo
open class InicioViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val uid = auth.currentUser?.uid
    open val user: FirebaseUser? = auth.currentUser

    private val _despensas = MutableStateFlow<List<Despensa>>(emptyList())
    open val despensas: StateFlow<List<Despensa>> = _despensas.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    open val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchDespensas()
    }

    /** Trae en tiempo real las despensas donde eres miembro */
    private fun fetchDespensas() {
        uid ?: return
        db.collection("despensas")
            .whereArrayContains("miembros", uid)
            .addSnapshotListener { snap, exc ->
                if (exc != null) {
                    _error.value = "Error cargando despensas: ${exc.message}"
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { doc ->
                        val codigo = doc.getString("codigo") ?: return@mapNotNull null
                        val nombre = doc.getString("nombre") ?: return@mapNotNull null
                        val miembros = doc.get("miembros") as? List<String> ?: return@mapNotNull null
                        val color = doc.getString("color") ?: return@mapNotNull null
                        Despensa(codigo, nombre, miembros,color)
                    } ?: emptyList()
                _despensas.value = list
            }
    }

    /** Crea una nueva despensa con nombre y código aleatorio */
    // TODO: Implementar mas campos
    open fun createDespensa(
        nombre: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val yourUid = uid ?: return onFailure("Usuario no autenticado")
        if (nombre.isBlank()) return onFailure("El nombre no puede estar vacío")
        val codigo = List(6) {
            (('A'..'Z') + ('0'..'9')).random()
        }.joinToString("")
        val color = "#E9E9EB"
        val descripcion = "Sin descripción"
        val data = mapOf(
            "nombre" to nombre,
            "codigo" to codigo,
            "miembros" to listOf(yourUid),
            "color" to color,
            "descripcion" to descripcion
        )
        db.collection("despensas")
            .add(data)
            .addOnSuccessListener { onSuccess(); Log.d("createDespensa", "Despensa creada") }
            .addOnFailureListener { Log.e("createDespensa", "Error creando despensa", it) }
    }

    /** Únete a una despensa por código */
    open fun joinDespensa(
        code: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val yourUid = uid ?: return onFailure("Usuario no autenticado")
        if (code.isBlank()) return onFailure("Introduce un código válido")
        db.collection("despensas")
            .whereEqualTo("codigo", code.trim().uppercase())
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    onFailure("No existe ninguna despensa con ese código")
                } else {
                    // Actualiza members con arrayUnion
                    val doc = snap.documents.first()
                    db.collection("despensas").document(doc.id)
                        .update("miembros", FieldValue.arrayUnion(yourUid))
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onFailure(e.message ?: "Error uniendo") }
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Error buscando despensa")
            }
    }

    open fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }
}
