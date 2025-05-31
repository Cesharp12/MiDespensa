package com.example.midespensa.presentation.login

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.midespensa.ui.theme.DarkGray
import com.example.midespensa.ui.theme.GreenBack
import com.example.midespensa.ui.theme.GreenConfirm
import com.example.midespensa.ui.theme.LightGraySub
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.viewmodel.compose.viewModel


/**
 * Pantalla de inicio de sesión que muestra campos de correo y contraseña,
 * gestiona el foco, validaciones y navega al resto de la app.
 *
 * Usa FirebaseAuth para el proceso de autenticación y muestra Toasts según el resultado.
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    // Al entrar, reiniciar los campos del ViewModel
    LaunchedEffect(Unit) {
        viewModel.cleanFields()
    }

    val context = LocalContext.current
    // Detecta si está en modo preview para evitar usar Firebase en Inspector
    val auth = if (!LocalInspectionMode.current) FirebaseAuth.getInstance() else null
    val focusManager = LocalFocusManager.current

    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    // Maneja botón físico atrás para salir de la app
    BackHandler {
        (context as? android.app.Activity)?.finish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GreenBack)
            .verticalScroll(rememberScrollState())
            .imePadding()
            // Tap fuera de los TextFields oculta teclado
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                focusManager.clearFocus()
            }
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("MiDespensa", fontSize = 35.sp, color = DarkGray, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(70.dp))
        Text("¡Bienvenido!", fontSize = 20.sp, color = DarkGray, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(15.dp))
        Text("Introduce tus datos para iniciar sesión", fontSize = 15.sp, color = DarkGray)
        Spacer(Modifier.height(15.dp))

        // Campo de correo con validación
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = {
                viewModel.errorMessage = null
                viewModel.email = it
                viewModel.validateEmail()
            },
            placeholder = { Text("Correo Electrónico", color = LightGraySub) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Correo") },
            isError = viewModel.emailError,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocus)
                .onFocusChanged { viewModel.validateEmail() },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        if (viewModel.emailError) {
            Text("Correo electrónico inválido", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(Modifier.height(15.dp))

        // Campo de contraseña con toggle de visibilidad
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = {
                viewModel.errorMessage = null
                viewModel.password = it
                viewModel.validatePassword()
            },
            placeholder = { Text("Contraseña", color = LightGraySub) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Contraseña") },
            trailingIcon = {
                IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                    Icon(
                        imageVector = if (viewModel.passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (viewModel.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = viewModel.passwordError,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocus)
                .onFocusChanged { viewModel.validatePassword() },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        if (viewModel.passwordError) {
            Text("Debe tener 8 caracteres, una mayúscula y un número", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))

        // Mostrar mensaje de error general si existe
        viewModel.errorMessage?.let { msg ->
            Text(msg, color = Color.Red, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }

        // Botón que invoca signIn de ViewModel y navega al inicio en caso de éxito
        Button(
            onClick = {
                focusManager.clearFocus()
                auth?.let {
                    viewModel.signIn(
                        auth = it,
                        onSuccess = {
                            Toast.makeText(context, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                            navController.navigate("inicio")
                        },
                        onFailure = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = GreenConfirm),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small
        ) {
            Text("Iniciar Sesión", color = Color.White, fontSize = 15.sp)
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(thickness = 2.dp)
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("¿Primera vez en MiDespensa?", color = DarkGray)
        }

        Spacer(Modifier.height(15.dp))

        // Botón para navegar a pantalla de registro
        Button(
            onClick = {
                focusManager.clearFocus()
                navController.navigate("register")
            },
            colors = ButtonDefaults.buttonColors(containerColor = DarkGray),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small
        ) {
            Text("Registrarse ahora", color = Color.White, fontSize = 15.sp)
        }
    }
}

