package com.example.midespensa.data.model

data class Receta (
    val label: String = "",
    val image: String = "",
    val url: String = "",
    val yield: Int = 0,
    val ingredientLines: List<String> = emptyList()
)