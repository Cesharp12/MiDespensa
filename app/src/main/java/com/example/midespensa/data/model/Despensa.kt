package com.example.midespensa.data.model

data class Despensa(
    val codigo: String = "",
    val nombre: String = "",
    val miembros: List<String> = emptyList(),
    val color: String = "",
    val descripcion: String = ""
)
