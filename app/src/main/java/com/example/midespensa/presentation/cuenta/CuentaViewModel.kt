package com.example.midespensa.presentation.cuenta

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.midespensa.notifications.WorkScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CuentaViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val user: FirebaseUser? = auth.currentUser

    private val _nombre = MutableStateFlow("")
    private val _apellidos = MutableStateFlow("")
    private val _profileImageUrl = MutableStateFlow<String?>(user?.photoUrl?.toString())

    val nombre: StateFlow<String> = _nombre.asStateFlow()
    val apellidos: StateFlow<String> = _apellidos.asStateFlow()
    val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

    init {
        fetchNombreApellidosUsuario()
    }

    /**
     * Obtiene el nombre y apellidos del usuario actual desde Firestore
     * y actualiza los valores internos del ViewModel.
     *
     * No devuelve nada. Solo actualiza los StateFlow internos.
     */
    private fun fetchNombreApellidosUsuario() {
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

    /**
     * Guarda la hora y minuto seleccionados en las preferencias compartidas
     * y programa una notificación diaria usando WorkManager.
     *
     * @param context Contexto necesario para acceder a SharedPreferences y programar la notificación.
     * @param hora Hora seleccionada para la notificación diaria.
     * @param minuto Minuto seleccionado para la notificación diaria.
     * @param onSuccess Función callback que se ejecuta después de programar correctamente la notificación.
     */
    fun scheduleDailyNotification(context: Context, hora: Int, minuto: Int, onSuccess: () -> Unit) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("notif_hour", hora)
            .putInt("notif_minute", minuto)
            .apply()

        // Programar notificación
        WorkScheduler.scheduleDailyNotification(context, hora, minuto)

        // Callback para notificar
        onSuccess()
    }


    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }
}
