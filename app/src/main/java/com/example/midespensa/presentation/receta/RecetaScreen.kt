package com.example.midespensa.presentation.receta

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.midespensa.presentation.components.BottomSection
import com.example.midespensa.presentation.components.HeaderSection
import com.example.midespensa.ui.theme.GreenBack
import com.example.midespensa.ui.theme.GreenConfirm
import com.example.midespensa.ui.theme.RedCancel

@Composable
fun RecetaScreen(
    navController: NavController,
    viewModel: RecetaViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Estados
    val recetas by viewModel.recetas
    val favs by viewModel.favorites
    val showTrad by viewModel.showTranslated
    val showFavs by viewModel.showFavorites
    val isLoading by viewModel.isLoading

    var searchQuery by remember { mutableStateOf("") }
    var mostrarDialogoSalir by remember { mutableStateOf(false) }

    BackHandler { mostrarDialogoSalir = true }
    if (mostrarDialogoSalir) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSalir = false },
            title = { Text("Confirmar cierre") },
            text = { Text("¿Estás seguro de que quieres cerrar sesión?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoSalir = false
                    viewModel.logout {
                        navController.navigate("inicio") { popUpTo("inicio") { inclusive = true } }
                    }
                }) { Text("Cerrar sesión") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoSalir = false }) { Text("Cancelar") } }
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
                Divider(thickness = 1.5.dp)
                BottomSection(navController)
            }
        },
        containerColor = GreenBack
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
        ) {
            // Cabecera: idioma o favoritos + switch
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFBBDEFB))
                    .padding(18.dp,6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = when {
                        showTrad -> "Recetas en español"
                        else -> "Recetas en idioma original"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
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

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(Modifier.height(60.dp))

                Text(
                    "Buscar recetas",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(Modifier.height(15.dp))
                HorizontalDivider(thickness = 2.dp)

                Spacer(Modifier.height(15.dp))

                // Buscador
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    label = { Text("Buscar") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()) viewModel.buscarRecetas(searchQuery)
                            focusManager.clearFocus()
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotBlank() && !showFavs) viewModel.buscarRecetas(searchQuery)
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                )

                Spacer(Modifier.height(10.dp))
                var showFavsButtonColor by remember { mutableStateOf(GreenConfirm) }
                // Botón favoritos
                Button(
                    onClick = { viewModel.toggleShowFavorites() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = showFavsButtonColor,
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    if (!showFavs){
                        showFavsButtonColor = GreenConfirm
                        Text("Mostrar recetas favoritas")
                    }else{
                        showFavsButtonColor = RedCancel
                        Text("Ocultar recetas favoritas")
                    }

                }

                Spacer(Modifier.height(8.dp))

                // Contenido: loading o lista
                Crossfade(targetState = isLoading) { loading ->
                    if (loading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        val listToShow = if (showFavs) favs else recetas
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(listToShow.count()) { idx ->
                                val receta = listToShow[idx]
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedCard(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                receta.label,
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = { viewModel.toggleFavorite(receta) }) {
                                                val isFav = favs.any { it.url == receta.url }
                                                Icon(
                                                    imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = null,
                                                    tint = if (isFav) Color(0xFFFFC107) else Color.Gray
                                                )
                                            }
                                        }
                                        // Texto ingredientes
                                        if(viewModel.showTranslated.value){
                                            Text(
                                                if(receta.yield == 1){
                                                    "Para una persona"
                                                }else{
                                                    "Para ${receta.yield} personas"
                                                },
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        } else{
                                            Text(
                                                if(receta.yield == 1){
                                                    "For one person"
                                                }else{
                                                    "For ${receta.yield} people"
                                                },
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))

                                        Text(if(viewModel.showTranslated.value) "Ingredientes:" else "Ingredients:")

                                        receta.ingredientLines.forEach {
                                            Text("• $it", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                ,
                                            horizontalArrangement = Arrangement.Center
                                        ){
                                            Button(onClick = {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(receta.url)))
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    contentColor = Color.White
                                                ),
                                                shape = MaterialTheme.shapes.medium,
                                            ) {
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