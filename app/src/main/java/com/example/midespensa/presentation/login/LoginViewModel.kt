package com.example.midespensa.presentation.login

import android.util.Patterns
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class LoginViewModel : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var emailError by mutableStateOf(false)
    var passwordError by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var passwordVisible by mutableStateOf(false)

    private val errorMessages = mapOf(
        "The email address is badly formatted." to "El correo electrónico no tiene un formato válido.",
        "There is no user record corresponding to this identifier. The user may have been deleted." to "No existe una cuenta con este correo.",
        "The password is invalid or the user does not have a password." to "La contraseña es incorrecta.",
        "A network error (such as timeout, interrupted connection or unreachable host) has occurred." to "Error de red. Verifica tu conexión.",
        "The supplied auth credential is incorrect, malformed or has expired." to "Correo o contraseña incorrectos. Por favor, verifica tus datos"
    )

    fun validateEmail(): Boolean {
        emailError = email.isNotEmpty() && (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
        return !emailError
    }

    fun validatePassword(): Boolean {
        passwordError = password.isNotEmpty() &&
                (password.length < 8 || !password.any { it.isDigit() } || !password.any { it.isUpperCase() })
        return !passwordError
    }

    fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
    }

    fun limpiarCampos() {
        email = ""
        password = ""
    }

    fun signIn(
        auth: FirebaseAuth,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Por favor, completa todos los campos."
            return
        }

        if (!validateEmail() || !validatePassword()) {
            errorMessage = "Revisa los campos antes de continuar."
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    errorMessage = null
                    onSuccess()
                } else {
                    val firebaseMsg = task.exception?.localizedMessage
                    errorMessage = errorMessages[firebaseMsg] ?: "Error desconocido: $firebaseMsg"
                    onFailure(errorMessage!!)
                }
            }
    }
}