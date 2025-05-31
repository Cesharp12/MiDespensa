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

class InicioViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val uid = auth.currentUser?.uid
    val user: FirebaseUser? = auth.currentUser

    private val _despensas = MutableStateFlow<List<Despensa>>(emptyList())
    val despensas: StateFlow<List<Despensa>> = _despensas.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchDespensas()
    }

    /**
     * Escucha en tiempo real las despensas del usuario autenticado y actualiza el estado interno.
     * Solo incluye las despensas donde el usuario está en la lista de miembros.
     *
     * No retorna valor, pero actualiza el `StateFlow` _despensas o _error según el resultado.
     */
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

    /**
     * Crea una nueva despensa con un nombre dado y la añade al usuario actual como miembro.
     *
     * @param nombre Nombre de la nueva despensa.
     * @param onSuccess Callback que se ejecuta si la creación fue exitosa.
     * @param onFailure Callback con mensaje de error en caso de fallo.
     */
    fun createDespensa(
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

    /**
     * Permite al usuario unirse a una despensa existente mediante un código alfanumérico.
     * Verifica que el usuario no esté ya en la despensa y que no se haya superado el límite de miembros.
     *
     * @param code Código de la despensa.
     * @param onSuccess Callback al unirse correctamente.
     * @param onFailure Callback con mensaje de error en caso de fallo o validación.
     */
    fun joinDespensa(
        code: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val yourUid = uid ?: return onFailure("Usuario no autenticado")
        if (code.isBlank()) return onFailure("Introduce un código válido")

        // Busca la despensa por su código
        db.collection("despensas")
            .whereEqualTo("codigo", code.trim().uppercase())
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    // Código incorrecto
                    onFailure("Código de despensa inválido")
                } else {
                    val doc = snap.documents.first()
                    val nombre = doc.getString("nombre") ?: "—"
                    val miembros = doc.get("miembros") as? List<String> ?: emptyList()

                    when {
                        // Comprueba si ya pertenece a la despensa
                        miembros.contains(yourUid) -> {
                            onFailure("Ya perteneces a la despensa '$nombre'")
                        }
                        // Comprueba si la despensa ya alcanzó el límite de miembros
                        miembros.size >= 8 -> {
                            onFailure("La despensa '$nombre' ha llegado al límite de miembros")
                        }
                        else -> {

                            db.collection("despensas").document(doc.id)
                                .update("miembros", FieldValue.arrayUnion(yourUid))
                                .addOnSuccessListener { onSuccess() }
                                .addOnFailureListener { e ->
                                    onFailure(e.message ?: "Error uniendo a la despensa")
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Error buscando despensa")
            }
    }

    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }
}
