package com.example.midespensa.presentation.cuenta

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CuentaViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val user: FirebaseUser? = auth.currentUser

    private val _nombre = MutableStateFlow("")
    private val _apellidos = MutableStateFlow("")
    private val _profileImageUrl = MutableStateFlow<String?>(user?.photoUrl?.toString())

    val nombre: StateFlow<String> = _nombre.asStateFlow()
    val apellidos: StateFlow<String> = _apellidos.asStateFlow()
    val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

    init {
        obtenerNombreYApellidos()
    }

    private fun obtenerNombreYApellidos() {
        val uid = user?.uid ?: return

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    _nombre.value = document.getString("nombre") ?: ""
                    _apellidos.value = document.getString("apellidos") ?: ""
                } else {
                    Log.e("CuentaViewModel", "No existe el documento de usuario")
                }
            }
            .addOnFailureListener {
                Log.e("CuentaViewModel", "Error al obtener datos del usuario", it)
            }
    }

    fun subirImagen(uri: Uri, onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        val uid = user?.uid ?: return
        val imagenRef = storage.reference.child("fotos_perfil/$uid.jpg")

        imagenRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Error al subir imagen")
                }
                imagenRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                actualizarFotoEnPerfil(downloadUri.toString())
                _profileImageUrl.value = downloadUri.toString()
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("CuentaViewModel", "Error al subir imagen: ${e.message}", e)
                onError(e)
            }
    }

    private fun actualizarFotoEnPerfil(nuevaUrl: String) {
        val perfilUpdate = UserProfileChangeRequest.Builder()
            .setPhotoUri(Uri.parse(nuevaUrl))
            .build()

        user?.updateProfile(perfilUpdate)
            ?.addOnSuccessListener {
                Log.d("CuentaViewModel", "Foto de perfil actualizada correctamente")
            }
            ?.addOnFailureListener {
                Log.e("CuentaViewModel", "Error al actualizar foto de perfil", it)
            }
    }

    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }
}
