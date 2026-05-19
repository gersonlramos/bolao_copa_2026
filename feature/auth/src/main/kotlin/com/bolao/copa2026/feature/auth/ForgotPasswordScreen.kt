package com.bolao.copa2026.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bolao.copa2026.ui.theme.BolaoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar senha") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = state.emailSent,
                enter = fadeIn() + slideInVertically()
            ) {
                EmailSentSuccess(email = state.email, onBack = onBack)
            }

            AnimatedVisibility(visible = !state.emailSent) {
                ForgotPasswordForm(
                    state = state,
                    onEmailChange = viewModel::onEmailChange,
                    onSend = viewModel::sendResetEmail
                )
            }
        }
    }
}

@Composable
private fun ForgotPasswordForm(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Esqueceu sua senha?",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Digite seu e-mail e enviaremos um link para você criar uma nova senha.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "E-mail",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            placeholder = { Text("seu@email.com") },
            isError = state.emailError != null,
            supportingText = { state.emailError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        state.error?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onSend,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Enviar link de recuperação")
            }
        }
    }
}

@Composable
private fun EmailSentSuccess(email: String, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.MarkEmailRead,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "E-mail enviado!",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Enviamos um link de recuperação para:\n$email\n\nVerifique sua caixa de entrada (e a pasta de spam).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar para o login")
        }
    }
}

@Preview(showBackground = true, name = "Formulário")
@Composable
private fun ForgotPasswordFormPreview() {
    BolaoTheme {
        @OptIn(ExperimentalMaterial3Api::class)
        Scaffold(topBar = { TopAppBar(title = { Text("Recuperar senha") }) }) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                ForgotPasswordForm(
                    state = ForgotPasswordUiState(email = "joao@email.com"),
                    onEmailChange = {},
                    onSend = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Sucesso")
@Composable
private fun ForgotPasswordSuccessPreview() {
    BolaoTheme {
        @OptIn(ExperimentalMaterial3Api::class)
        Scaffold(topBar = { TopAppBar(title = { Text("Recuperar senha") }) }) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                EmailSentSuccess(email = "joao@email.com", onBack = {})
            }
        }
    }
}
