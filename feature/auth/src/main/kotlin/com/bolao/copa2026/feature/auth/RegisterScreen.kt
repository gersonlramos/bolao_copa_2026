package com.bolao.copa2026.feature.auth

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.bolao.copa2026.feature.auth.R
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
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Dark overlay
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF000000).copy(alpha = 0.65f)))

        // Form content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Criar conta",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFFFB700)
            )
            Text(
                "Copa 2026",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(40.dp))

            // Nome
            Text(
                "Nome de exibição",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.displayName,
                onValueChange = onDisplayNameChange,
                placeholder = { Text("Como você quer ser chamado", color = Color.White.copy(alpha = 0.4f)) },
                isError = state.displayNameError != null,
                supportingText = { state.displayNameError?.let { Text(it, color = Color(0xFFFF6B6B)) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = darkFieldColors(isError = state.displayNameError != null)
            )
            Spacer(Modifier.height(12.dp))

            // E-mail
            Text(
                "E-mail",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                placeholder = { Text("seu@email.com", color = Color.White.copy(alpha = 0.4f)) },
                isError = state.emailError != null,
                supportingText = { state.emailError?.let { Text(it, color = Color(0xFFFF6B6B)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = darkFieldColors(isError = state.emailError != null)
            )
            Spacer(Modifier.height(12.dp))

            // Senha
            var passwordVisible by remember { mutableStateOf(false) }
            Text(
                "Senha",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                placeholder = { Text("Mínimo 8 caracteres", color = Color.White.copy(alpha = 0.4f)) },
                isError = state.passwordError != null,
                supportingText = { state.passwordError?.let { Text(it, color = Color(0xFFFF6B6B)) } },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = darkFieldColors(isError = state.passwordError != null)
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Cadastrar", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onGoogleLogin,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Text("Cadastrar com Google")
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Já tenho conta — Fazer login", color = Color.White.copy(alpha = 0.8f))
            }
        }

        // Snackbar at bottom
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
        )
    }
}

@Composable
private fun darkFieldColors(isError: Boolean = false) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = if (isError) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.8f),
    unfocusedBorderColor = if (isError) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.4f),
    cursorColor = Color.White,
    focusedContainerColor = Color.White.copy(alpha = 0.08f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
    errorBorderColor = Color(0xFFFF6B6B),
    errorTextColor = Color.White,
    errorContainerColor = Color.White.copy(alpha = 0.08f)
)

@Preview(showBackground = true, name = "Cadastro vazio")
@Composable
private fun RegisterPreview() {
    BolaoTheme { RegisterContent(state = AuthUiState()) }
}
