package com.bolao.copa2026.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.success) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alterar senha") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.success) {
                SuccessFeedback()
            } else {
                ChangePasswordForm(
                    state = state,
                    onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
                    onNewPasswordChange = viewModel::onNewPasswordChange,
                    onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                    onSave = viewModel::save
                )
            }
        }
    }
}

@Composable
private fun ChangePasswordForm(
    state: ChangePasswordUiState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Alterar senha", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Informe sua senha atual e escolha uma nova.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        PasswordField(
            label = "Senha atual",
            value = state.currentPassword,
            onValueChange = onCurrentPasswordChange,
            error = state.currentPasswordError,
            placeholder = "••••••••"
        )

        PasswordField(
            label = "Nova senha",
            value = state.newPassword,
            onValueChange = onNewPasswordChange,
            error = state.newPasswordError,
            placeholder = "Mínimo 8 caracteres"
        )

        PasswordField(
            label = "Confirmar nova senha",
            value = state.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            error = state.confirmPasswordError,
            placeholder = "Repita a nova senha"
        )

        state.error?.let { msg ->
            Spacer(Modifier.height(4.dp))
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Salvar nova senha")
            }
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    placeholder: String
) {
    var visible by remember { mutableStateOf(false) }

    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (visible) "Ocultar" else "Mostrar"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun SuccessFeedback() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text("Senha alterada com sucesso!", style = MaterialTheme.typography.titleLarge)
        Text(
            "Voltando ao perfil...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
