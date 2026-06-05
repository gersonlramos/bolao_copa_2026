package com.bolao.copa2026.feature.auth

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.bolao.copa2026.ui.theme.BolaoTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(state.navigateToHome) {
        if (state.navigateToHome) {
            viewModel.clearNavigation()
            onNavigateToHome()
        }
    }

    LaunchedEffect(state.systemError) {
        state.systemError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    RegisterContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onRegister = viewModel::register,
        onGoogleLogin = {
            scope.launch {
                handleGoogleSignIn(context, viewModel::onGoogleSignInResult, snackbarHostState)
            }
        },
        onNavigateToLogin = onNavigateToLogin
    )
}

private suspend fun handleGoogleSignIn(
    context: Context,
    onIdTokenReceived: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val credentialManager = CredentialManager.create(context)
    val webClientId = "98850868123-teo35hgnm4334qtceq1el4fseebdav6g.apps.googleusercontent.com"

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(context = context, request = request)
        val credential = result.credential
        if (credential is androidx.credentials.CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            onIdTokenReceived(googleIdTokenCredential.idToken)
        }
    } catch (e: GetCredentialException) {
        Log.e("RegisterScreen", "Google Sign In Error", e)
        snackbarHostState.showSnackbar("Erro no login com Google: ${e.message}")
    }
}

@Composable
fun RegisterContent(
    state: AuthUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onDisplayNameChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onRegister: () -> Unit = {},
    onGoogleLogin: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Cadastro", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))

            Text(
                "Nome de exibição",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.displayName,
                onValueChange = onDisplayNameChange,
                placeholder = { Text("Como você quer ser chamado") },
                isError = state.displayNameError != null,
                supportingText = { state.displayNameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            Text(
                "E-mail",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
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
            Spacer(Modifier.height(12.dp))

            var passwordVisible by remember { mutableStateOf(false) }
            Text(
                "Senha",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                placeholder = { Text("Mínimo 8 caracteres") },
                isError = state.passwordError != null,
                supportingText = { state.passwordError?.let { Text(it) } },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Cadastrar")
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onGoogleLogin,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                Text("Cadastrar com Google")
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Já tenho conta — Fazer login")
            }
        }
    }
}

@Preview(showBackground = true, name = "Cadastro vazio")
@Composable
private fun RegisterPreview() {
    BolaoTheme { RegisterContent(state = AuthUiState()) }
}
