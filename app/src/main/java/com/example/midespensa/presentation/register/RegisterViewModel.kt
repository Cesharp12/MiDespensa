package com.example.midespensa.presentation.register

import android.util.Log
import android.util.Patterns
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * ViewModel responsable de manejar el registro de usuarios,
 * validando campos y coordinando la interacción con Firebase.
 */
class RegisterViewModel : ViewModel() {

    // Estado de los campos del formulario
    var name by mutableStateOf("")
    var surname by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")

    // Flags para indicar errores de validación
    var nameError by mutableStateOf(false)
    var surnameError by mutableStateOf(false)
    var emailError by mutableStateOf(false)
    var passwordError by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var passwordVisible by mutableStateOf(false)

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Map de mensajes personalizados para errores comunes de FirebaseAuth
    private val errorMessages = mapOf(
        "The email address is already in use by another account." to "Este correo ya está registrado.",
        "The email address is badly formatted." to "El correo electrónico no tiene un formato válido.",
        "A network error (such as timeout, interrupted connection or unreachable host) has occurred." to "Error de red. Verifica tu conexión."
    )

    /**
     * Valida que el nombre tenga entre 2 y 30 caracteres si no está vacío.
     */
    fun validateName() {
        nameError = name.isNotEmpty() && (name.length < 2 || name.length > 30)
    }

    /**
     * Valida que los apellidos tengan entre 2 y 50 caracteres si no están vacíos.
     */
    fun validateSurname() {
        surnameError = surname.isNotEmpty() && (surname.length < 2 || surname.length > 50)
    }

    /**
     * Verifica el formato del correo electrónico usando el patrón estándar.
     */
    fun validateEmail() {
        emailError = email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Comprueba que la contraseña tenga al menos 8 caracteres, un dígito y una mayúscula.
     */
    fun validatePassword() {
        passwordError = password.isNotEmpty() && (
                password.length < 8 ||
                        !password.any { it.isDigit() } ||
                        !password.any { it.isUpperCase() }
                )
    }

    /**
     * Inicia el proceso de registro: comprueba campos, valida y crea el usuario en Firebase.
     * Si el registro en FirebaseAuth es exitoso, almacena los datos en Firestore.
     *
     * @param onSuccess Función llamada tras guardar correctamente en Firestore.
     * @param onFailure Función llamada si falla la escritura en Firestore.
     */
    fun registerUser(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Asegura que no quedan campos vacíos
        if (name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank()) {
            errorMessage = "Por favor, completa todos los campos."
            return
        }

        // Ejecuta todas las validaciones y se detiene si hay errores
        validateName()
        validateSurname()
        validateEmail()
        validatePassword()
        if (nameError || surnameError || emailError || passwordError) {
            errorMessage = "Corrige los errores antes de continuar."
            return
        }

        // Lanza la llamada a FirebaseAuth en un coroutine
        viewModelScope.launch {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Si el usuario se crea, se guardan sus datos en Firestore
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                        val userData = hashMapOf(
                            "uid" to uid,
                            "nombre" to name,
                            "apellidos" to surname,
                            "email" to email,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("usuarios").document(uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Usuario guardado correctamente")
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Error al guardar", e)
                                onFailure("Error al guardar en Firestore")
                            }
                    } else {
                        // Mapea excepciones específicas a mensajes personalizados
                        val errorMsg = when (task.exception) {
                            is FirebaseAuthUserCollisionException -> "Este correo ya está registrado."
                            is FirebaseAuthWeakPasswordException -> "Contraseña demasiado débil."
                            is FirebaseAuthInvalidCredentialsException -> "Correo inválido."
                            else -> task.exception?.localizedMessage ?: "Error desconocido"
                        }
                        errorMessage = errorMsg
                    }
                }
        }
    }

    /** Alterna la visibilidad del texto de la contraseña. */
    fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
    }

    /** Limpia el mensaje de error global. */
    fun clearError() {
        errorMessage = null
    }
}
