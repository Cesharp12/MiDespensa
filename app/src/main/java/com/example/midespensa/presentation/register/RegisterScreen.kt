package com.example.midespensa.presentation.register

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.midespensa.ui.theme.*

/**
 * Pantalla de registro de usuario.
 *
 * Muestra un formulario con campos de nombre, apellidos, email y contraseña,
 * aplica validaciones y maneja el registro mediante el ViewModel.
 *
 */
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // FocusRequesters para mover el foco de un campo al siguiente
    val nameFocus = remember { FocusRequester() }
    val surnameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    // Capturar back del sistema para volver a login y limpiar la pila
    BackHandler {
        navController.navigate("login") {
            popUpTo("register") { inclusive = true }
            launchSingleTop = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GreenBack)
            .verticalScroll(rememberScrollState())
            .imePadding()  // Añade padding para que el contenido no quede tras el teclado
            .pointerInput(Unit) {
                // Tap fuera de campos limpia el foco y oculta teclado
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Crear cuenta", fontSize = 28.sp, color = DarkGray, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(70.dp))
        Text("¡Únete a nosotros!", fontSize = 16.sp, color = DarkGray)
        Spacer(Modifier.height(15.dp))

        // Campo de nombre con validación al cambiar y al perder foco
        OutlinedTextField(
            value = viewModel.name,
            onValueChange = {
                viewModel.name = it
                viewModel.validateName()
                viewModel.clearError()
            },
            placeholder = { Text("Nombre", color = LightGraySub) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            isError = viewModel.nameError,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocus)
                .onFocusChanged { viewModel.validateName() },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { surnameFocus.requestFocus() })
        )
        if (viewModel.nameError) {
            Text("Debe tener entre 2 y 30 caracteres", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(Modifier.height(15.dp))

        // Campo de apellidos similar al anterior
        OutlinedTextField(
            value = viewModel.surname,
            onValueChange = {
                viewModel.surname = it
                viewModel.validateSurname()
                viewModel.clearError()
            },
            placeholder = { Text("Apellidos", color = LightGraySub) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            isError = viewModel.surnameError,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(surnameFocus)
                .onFocusChanged { viewModel.validateSurname() },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { emailFocus.requestFocus() })
        )
        if (viewModel.surnameError) {
            Text("Debe tener entre 2 y 50 caracteres", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(Modifier.height(15.dp))

        // Campo de correo
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = {
                viewModel.email = it
                viewModel.validateEmail()
                viewModel.clearError()
            },
            placeholder = { Text("Correo Electrónico", color = LightGraySub) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            isError = viewModel.emailError,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocus)
                .onFocusChanged { viewModel.validateEmail() },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() })
        )
        if (viewModel.emailError) {
            Text("Correo electrónico inválido", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(Modifier.height(15.dp))

        // Campo de contraseña con visibilidad toggle
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = {
                viewModel.password = it
                viewModel.validatePassword()
                viewModel.clearError()
            },
            placeholder = { Text("Contraseña", color = LightGraySub) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                    val visibilityIcon = if (viewModel.passwordVisible)
                        Icons.Default.Visibility else Icons.Default.VisibilityOff
                    Icon(imageVector = visibilityIcon, contentDescription = null)
                }
            },
            visualTransformation = if (viewModel.passwordVisible)
                VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            isError = viewModel.passwordError,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocus)
                .onFocusChanged { viewModel.validatePassword() }
        )
        if (viewModel.passwordError) {
            Text(
                "Debe tener 8 caracteres, una mayúscula y un número",
                color = Color.Red,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(thickness = 2.dp)
        Spacer(Modifier.height(20.dp))

        // Mostrar mensaje de error general si ya existe
        viewModel.errorMessage?.let {
            Text(it, color = Color.Red, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }

        // Botón de registro: llama al ViewModel y muestra Toast según resultado
        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.registerUsuario(
                    onSuccess = {
                        Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        navController.navigate("login")
                    },
                    onFailure = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = GreenConfirm),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small
        ) {
            Text("Registrarse", color = Color.White, fontSize = 15.sp)
        }

        // Enlace para volver al login sin registrar
        TextButton(onClick = {
            navController.navigate("login")
            focusManager.clearFocus()
        }) {
            Text(
                "Volver al inicio de sesión",
                color = DarkGray,
                fontSize = 14.sp,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}
