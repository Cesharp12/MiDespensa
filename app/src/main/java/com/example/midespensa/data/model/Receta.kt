package com.example.midespensa.data.model

data class Receta (
    val label: String,
    val image: String,
    val ingredientLines: List<String>,
    val url: String,
    val yield: Int
)