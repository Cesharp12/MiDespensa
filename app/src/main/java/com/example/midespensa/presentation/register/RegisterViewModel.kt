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

class RegisterViewModel : ViewModel() {

    var name by mutableStateOf("")
    var surname by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")

    var nameError by mutableStateOf(false)
    var surnameError by mutableStateOf(false)
    var emailError by mutableStateOf(false)
    var passwordError by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var passwordVisible by mutableStateOf(false)

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val errorMessages = mapOf(
        "The email address is already in use by another account." to "Este correo ya está registrado.",
        "The email address is badly formatted." to "El correo electrónico no tiene un formato válido.",
        "A network error (such as timeout, interrupted connection or unreachable host) has occurred." to "Error de red. Verifica tu conexión."
    )

    // Validadores
    fun validateName() {
        nameError = name.isNotEmpty() && (name.length < 2 || name.length > 30)
    }

    fun validateSurname() {
        surnameError = surname.isNotEmpty() && (surname.length < 2 || surname.length > 40)
    }

    fun validateEmail() {
        emailError = email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validatePassword() {
        passwordError = password.isNotEmpty() && (
                password.length < 8 ||
                        !password.any { it.isDigit() } ||
                        !password.any { it.isUpperCase() }
                )
    }

    fun registerUser(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank()) {
            errorMessage = "Por favor, completa todos los campos."
            return
        }

        validateName()
        validateSurname()
        validateEmail()
        validatePassword()

        if (nameError || surnameError || emailError || passwordError) {
            errorMessage = "Corrige los errores antes de continuar."
            return
        }

        viewModelScope.launch {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
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
                        val errorMsg = when (task.exception) {
                            is FirebaseAuthUserCollisionException -> "Este correo ya está registrado."
                            is FirebaseAuthWeakPasswordException -> "Contraseña demasiado débil."
                            is FirebaseAuthInvalidCredentialsException -> "Correo inválido."
                            else -> task.exception?.localizedMessage ?: "Error desconocido"
                        }
                        errorMessage = errorMsg
                        //onFailure(errorMsg)
                    }
                }
        }
    }


    fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
    }

    fun clearError() {
        errorMessage = null
    }
}
