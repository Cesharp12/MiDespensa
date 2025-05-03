package com.example.midespensa.presentation.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.midespensa.ui.theme.DarkGray
import com.example.midespensa.ui.theme.GreenBack
import com.example.midespensa.ui.theme.GreenConfirm
import com.example.midespensa.ui.theme.LightGraySub
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel = viewModel()) {

    // Cuando entras a esta pantalla, limpia los campos del ViewModel
    LaunchedEffect(Unit) {
        viewModel.limpiarCampos()
    }

    val context = LocalContext.current
    val auth = if (!LocalInspectionMode.current) FirebaseAuth.getInstance() else null
    val focusManager = LocalFocusManager.current

    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    val email = viewModel.email
    val password = viewModel.password
    val emailError = viewModel.emailError
    val passwordError = viewModel.passwordError
    val errorMessage = viewModel.errorMessage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GreenBack)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                focusManager.clearFocus()
            }
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("MiDespensa", fontSize = 35.sp, color = DarkGray, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(70.dp))
        Text("¡Bienvenido!", fontSize = 20.sp, color = DarkGray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(15.dp))
        Text("Introduce tus datos para iniciar sesión", fontSize = 15.sp, color = DarkGray)
        Spacer(modifier = Modifier.height(15.dp))

        // Campo de Email
        OutlinedTextField(
            value = email,
            onValueChange = {
                viewModel.errorMessage = null
                viewModel.email = it
                viewModel.validateEmail()
            },
            placeholder = { Text("Correo Electrónico", color = LightGraySub) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Correo") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            isError = emailError,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocus)
                .onFocusChanged {
                    viewModel.validateEmail()
                }
        )
        if (emailError) {
            Text("Correo electrónico inválido", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(15.dp))

        // Campo de Contraseña
        OutlinedTextField(
            value = password,
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
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }),
            isError = passwordError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
                .focusRequester(passwordFocus)
                .onFocusChanged {
                    viewModel.validatePassword()
                }
        )
        if (passwordError) {
            Text("Debe tener 8 caracteres, una mayúscula y un número", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mensaje de error general
        errorMessage?.let {
            Text(it, color = Color.Red, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Botón iniciar sesión
        Button(
            onClick = {
                focusManager.clearFocus()
                auth?.let {
                    viewModel.signIn(
                        auth = it,
                        onSuccess = {
                            // Mensaje flotante del sistema
                            Toast.makeText(context, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                            navController.navigate("cuenta")
                        },
                        onFailure = {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = GreenConfirm),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        ) {
            Text("Iniciar Sesión", color = Color.White, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(thickness = 2.dp)
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("¿Primera vez en MiDespensa?", color = DarkGray)
        }

        Spacer(modifier = Modifier.height(15.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                navController.navigate("register")
            },
            colors = ButtonDefaults.buttonColors(containerColor = DarkGray),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        ) {
            Text("Registrarse ahora", color = Color.White, fontSize = 15.sp)
        }
    }
}

@Preview(showBackground = true, widthDp = 430, heightDp = 930)
@Composable
fun LoginPreview() {
    val context = LocalContext.current
    CompositionLocalProvider(LocalContext provides context) {
        LoginScreen(navController = rememberNavController())
    }
}

