package com.example.midespensa.presentation.receta

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.midespensa.ui.theme.GreenBack


import com.example.midespensa.presentation.components.HeaderSection
import com.example.midespensa.presentation.components.BottomSection

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import com.example.midespensa.data.model.Receta
import com.example.midespensa.ui.theme.GreenConfirm


@Composable
fun RecetaScreen(navController: NavController, viewModel: RecetaViewModel = viewModel()) {
    val user = viewModel.user
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Recetas
    val recetas by viewModel.recetas
    var searchQuery by remember { mutableStateOf("") }
    val showTrad by viewModel.showTranslated
        // favoritos
    val favs by viewModel.favorites


    // Animación de carga
    val isLoading by viewModel.isLoading

    // GESTIÓN DE LA ACCIÓN DE VOLVER A ATRÁS
    var mostrarDialogoSalir by remember { mutableStateOf(false) }

    // Interceptar botón "atrás" del sistema
    BackHandler {
        mostrarDialogoSalir = true
    }
    if (mostrarDialogoSalir) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSalir = false },
            title = { Text("Confirmar cierre") },
            text = { Text("¿Estás seguro de que quieres cerrar sesión?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoSalir = false
                    viewModel.logout {
                        navController.navigate("login") {
                            popUpTo("cuenta") { inclusive = true }
                        }
                    }
                }) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoSalir = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                HeaderSection(title = "Recetas")
                HorizontalDivider(thickness = 1.5.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 1.5.dp)
                BottomSection(navController = navController)
            }
        },
        containerColor = GreenBack
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            FavoritesDropdown(favorites = viewModel.favorites.value)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 20.dp)
                    ,
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(70.dp))

                Text(
                    "Buscar recetas",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(Modifier.height(15.dp))
                HorizontalDivider(thickness = 2.dp)
                Spacer(Modifier.height(15.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    label = { Text("Buscar") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.buscarRecetas(searchQuery)
                            }
                            focusManager.clearFocus()
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.buscarRecetas(searchQuery)
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                )

                // Toggle switch
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (showTrad) "Volver a idioma original" else "Traducir a español",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black)
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = showTrad,
                        onCheckedChange = { viewModel.toggleTranslation() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GreenBack,
                            checkedTrackColor = GreenConfirm,
                            uncheckedThumbColor = Color.DarkGray,
                            uncheckedTrackColor = Color.LightGray
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    Crossfade(targetState = isLoading) { loading ->
                        if (loading) {
                            // Animación de carga centrada
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            // Cuando termine el loading se muestra la lista
                            LazyColumn(Modifier.fillMaxSize()) {
                                items(recetas.count()) { receta ->
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(recetas.get(receta).label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                                IconButton(onClick = { viewModel.toggleFavorite(recetas.get(receta)) }) {
                                                    Icon(
                                                        imageVector = if (favs.any { it.url == recetas.get(receta).url }) Icons.Default.Star else Icons.Default.StarBorder,
                                                        contentDescription = "Añadir a favoritos",
                                                        tint = if (favs.any { it.url == recetas.get(receta).url })
                                                              Color(0xFFFFC107)
                                                        else
                                                            Color.Gray
                                                    )


                                                }
                                            }
                                            Text("Para ${recetas.get(receta).yield} personas", style = MaterialTheme.typography.bodySmall)
                                            Spacer(Modifier.height(8.dp))
                                            Text("Ingredientes:")
                                            recetas.get(receta).ingredientLines.forEach {
                                                Text("• $it", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Button(onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(recetas.get(receta).url))
                                                context.startActivity(intent)
                                            }) {
                                                Text("Enlace a la receta completa")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesDropdown(favorites: List<Receta>) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF59D))
                .clickable { expanded = !expanded }
                .padding(horizontal = 32.dp, vertical = 20.dp)
        ) {
            Text("Favoritos", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp) // limita espacio
                    .padding(horizontal = 32.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                favorites.forEach { receta ->
                    Text(receta.label, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }

}


