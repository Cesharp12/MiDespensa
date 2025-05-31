package com.example.midespensa.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.midespensa.ui.theme.GreenBack
import com.example.midespensa.ui.theme.GreenConfirm
import androidx.compose.material3.NavigationBarItemDefaults

/**
 * Barra de navegación inferior para cada pantalla con accesos a las secciones principales de la app.
 *
 * Muestra cuatro elementos: Inicio, Recetas, Compra y Cuenta. Cada uno detecta
 * la ruta activa a través del NavController y aplica estilo seleccionado.
 * Cambia de pantalla al pulsar un elemento diferente.
 *
 * @param navController Controlador de navegación usado para cambiar de ruta.
 */
@Composable
fun BottomSection(navController: NavController) {
    // Obtener la ruta actual
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = GreenBack,
        contentColor = Color.Black
    ) {
        // Elemento "Inicio"
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") },
            selected = currentRoute == "inicio",
            onClick = {
                if (currentRoute != "inicio") navController.navigate("inicio")
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenConfirm,    // Icono al seleccionar
                selectedTextColor = GreenConfirm,    // Texto al seleccionar
                indicatorColor = Color.Transparent   // Sin indicador de fondo
            )
        )

        // Elemento "Recetas"
        NavigationBarItem(
            icon = { Icon(Icons.Default.Book, contentDescription = "Recetas") },
            label = { Text("Recetas") },
            selected = currentRoute == "receta",
            onClick = {
                if (currentRoute != "receta") navController.navigate("receta")
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenConfirm,
                selectedTextColor = GreenConfirm,
                indicatorColor = Color.Transparent
            )
        )

        // Elemento "Compra"
        NavigationBarItem(
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Compra") },
            label = { Text("Compra") },
            selected = currentRoute == "compra",
            onClick = {
                if (currentRoute != "compra") navController.navigate("compra")
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenConfirm,
                selectedTextColor = GreenConfirm,
                indicatorColor = Color.Transparent
            )
        )

        // Elemento "Cuenta"
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Cuenta") },
            label = { Text("Cuenta") },
            selected = currentRoute == "cuenta",
            onClick = {
                if (currentRoute != "cuenta") navController.navigate("cuenta")
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenConfirm,
                selectedTextColor = GreenConfirm,
                indicatorColor = Color.Transparent
            )
        )
    }
}
