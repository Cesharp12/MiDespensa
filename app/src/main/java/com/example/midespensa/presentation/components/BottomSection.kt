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
import com.example.midespensa.ui.theme.GreenBack
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.NavigationBarItemDefaults
import com.example.midespensa.ui.theme.GreenConfirm

@Composable
fun BottomSection(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = GreenBack,
        contentColor = Color.Black
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") },
            selected = currentRoute == "inicio",
            onClick = {
                if (currentRoute != "inicio") navController.navigate("inicio")
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenConfirm,
                selectedTextColor = GreenConfirm,
                indicatorColor = Color.Transparent
            )
        )
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
