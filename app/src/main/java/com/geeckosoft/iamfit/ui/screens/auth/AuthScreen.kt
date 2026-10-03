package com.geeckosoft.iamfit.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Login / registro contra el backend (Sanctum). Al autenticarse, el token queda
 * cifrado en [com.geeckosoft.iamfit.data.TokenStore] y [onAuthenticated] abre la app.
 */
@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(),
) {
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    // Credenciales del usuario demo sembrado en local; en producción se capturan.
    var email by remember { mutableStateOf("demo@iamfit.local") }
    var password by remember { mutableStateOf("password") }

    val state = viewModel.state

    LaunchedEffect(state) {
        if (state is AuthUiState.Success) onAuthenticated()
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("IAm-fit", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(4.dp))
            Text(
                if (isRegister) "Crea tu cuenta" else "Entra para sincronizar tu día",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))

            if (isRegister) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; viewModel.resetError() },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; viewModel.resetError() },
                label = { Text("Correo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; viewModel.resetError() },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state is AuthUiState.Error) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "❌ ${state.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (isRegister) {
                        viewModel.register(name.trim(), email.trim(), password)
                    } else {
                        viewModel.login(email.trim(), password)
                    }
                },
                enabled = state !is AuthUiState.Loading &&
                    email.isNotBlank() && password.isNotBlank() &&
                    (!isRegister || name.isNotBlank()),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isRegister) "Crear cuenta" else "Entrar")
                }
            }

            TextButton(
                onClick = {
                    isRegister = !isRegister
                    viewModel.resetError()
                },
            ) {
                Text(if (isRegister) "Ya tengo cuenta" else "Crear una cuenta nueva")
            }

            Text(
                "Backend: ${com.geeckosoft.iamfit.BuildConfig.API_BASE_URL}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
