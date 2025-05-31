package com.example.midespensa

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.midespensa.presentation.compra.CompraScreen
import com.example.midespensa.presentation.cuenta.CuentaScreen
import com.example.midespensa.presentation.despensa.DespensaScreen
import com.example.midespensa.presentation.inicio.InicioScreen
import com.example.midespensa.presentation.login.LoginScreen
import com.example.midespensa.presentation.receta.RecetaScreen
import com.example.midespensa.presentation.register.RegisterScreen


@Composable
fun NavigationWrapper(navHostController: NavHostController, startDestination: String){

    NavHost(
        navController = navHostController,
        startDestination = startDestination // Ruta inicial
    ) {
        composable("login") {
            LoginScreen(navController = navHostController)
        }

        composable("register") {
            RegisterScreen(navController = navHostController)
        }

        composable("cuenta") {
            CuentaScreen(navController = navHostController)
        }

        composable("inicio") {
            InicioScreen(navController = navHostController)
        }

        composable("compra") {
            CompraScreen(navController = navHostController)
        }

        composable("receta") {
            RecetaScreen(navController = navHostController)
        }

        composable(
            route = "despensa/{codigo}",
            arguments = listOf(navArgument("codigo") { type = NavType.StringType })
        ) { backStackEntry ->
            val codigo = backStackEntry.arguments?.getString("codigo") ?: ""
            DespensaScreen(navController = navHostController, codigoDespensa = codigo)
        }
    }
}