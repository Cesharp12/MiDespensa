package com.example.midespensa.presentation.login

import android.util.Patterns
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * ViewModel encargado de gestionar el estado y la lógica de autenticación del usuario.
 *
 * Mantiene los campos email y contraseña, sus flags de error y el mensaje global.
 * Además proporciona métodos para validar formato, alternar visibilidad de la contraseña
 * y realizar el proceso de sign-in con FirebaseAuth.
 */
class LoginViewModel : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var emailError by mutableStateOf(false)
    var passwordError by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var passwordVisible by mutableStateOf(false)

    // Mapeo de mensajes personalizados para errores comunes de FirebaseAuth
    private val errorMessages = mapOf(
        "The email address is badly formatted." to "El correo electrónico no tiene un formato válido.",
        "There is no user record corresponding to this identifier. The user may have been deleted." to "No existe una cuenta con este correo.",
        "The password is invalid or the user does not have a password." to "La contraseña es incorrecta.",
        "A network error (such as timeout, interrupted connection or unreachable host) has occurred." to "Error de red. Verifica tu conexión.",
        "The supplied auth credential is incorrect, malformed or has expired." to "Correo o contraseña incorrectos. Por favor, verifica tus datos"
    )

    /**
     * Valida el formato del email usando un patrón estándar.
     * @return true si el email es válido o está vacío, false en caso contrario.
     */
    fun validateEmail(): Boolean {
        emailError = email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        return !emailError
    }

    /**
     * Verifica que la contraseña tenga al menos 8 caracteres, un dígito y una mayúscula.
     * @return true si cumple los requisitos o está vacía, false si hay error.
     */
    fun validatePassword(): Boolean {
        passwordError = password.isNotEmpty() && (
                password.length < 8 ||
                        !password.any { it.isDigit() } ||
                        !password.any { it.isUpperCase() }
                )
        return !passwordError
    }


    fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
    }

    fun limpiarCampos() {
        email = ""
        password = ""
    }

    /**
     * Realiza el inicio de sesión con FirebaseAuth.
     * Comprueba campos vacíos y validaciones.
     * Mapea errores de Firebase a mensajes amigables y llama a los callbacks.
     *
     * @param auth   Instancia de FirebaseAuth para autenticar.
     * @param onSuccess  Función ejecutada al autenticarse exitosamente.
     * @param onFailure  Función ejecutada al fallar, con el mensaje de error.
     */
    fun signIn(
        auth: FirebaseAuth,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // No permitir campos vacíos
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Por favor, completa todos los campos."
            return
        }

        // Validaciones de formato
        if (!validateEmail() || !validatePassword()) {
            errorMessage = "Revisa los campos antes de continuar."
            return
        }

        // Llamada asíncrona a FirebaseAuth
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    errorMessage = null
                    onSuccess()
                } else {
                    // Mapear mensaje de excepción a texto personalizado
                    val firebaseMsg = task.exception?.localizedMessage
                    errorMessage = errorMessages[firebaseMsg] ?: "Error desconocido: $firebaseMsg"
                    onFailure(errorMessage!!)
                }
            }
    }
}
