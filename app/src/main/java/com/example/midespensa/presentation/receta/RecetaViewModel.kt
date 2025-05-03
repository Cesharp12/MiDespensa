package com.example.midespensa.presentation.receta

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class RecetaViewModel: ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val user: FirebaseUser? = auth.currentUser

    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }
}