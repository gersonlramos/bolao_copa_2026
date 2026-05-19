package com.bolao.copa2026.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bolao.copa2026.domain.model.User
import com.bolao.copa2026.ui.theme.BolaoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.successMessage) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header with avatar
            ProfileHeader(user = state.user)

            Spacer(Modifier.height(24.dp))

            // Info section
            Text(
                "Informações",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (state.isEditingName) {
                NameEditRow(
                    value = state.nameInput,
                    onValueChange = viewModel::onNameChange,
                    onSave = viewModel::updateName,
                    onCancel = viewModel::toggleEditingName,
                    isLoading = state.isLoading
                )
            } else {
                ProfileInfoRow(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = "Nome",
                    value = state.user?.displayName ?: "—",
                    trailingContent = {
                        IconButton(onClick = viewModel::toggleEditingName) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar nome", 
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            
            ProfileInfoRow(
                icon = { Icon(Icons.Default.Email, contentDescription = null) },
                label = "E-mail",
                value = state.user?.email ?: "—"
            )

            if (viewModel.isEmailPasswordUser) {
                Spacer(Modifier.height(24.dp))

                Text(
                    "Segurança",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                ListItem(
                    headlineContent = { Text("Alterar senha") },
                    leadingContent = {
                        Icon(Icons.Default.Lock, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = onChangePassword)
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            }
        }
    }
}

@Composable
private fun NameEditRow(
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Nome") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = !isLoading
        )
        
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Check, contentDescription = "Salvar", tint = Color(0xFF4CAF50))
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: User?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UserAvatar(name = user?.displayName ?: "", size = 80)

        Text(
            text = user?.displayName ?: "Carregando...",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun UserAvatar(name: String, size: Int = 48) {
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = (size / 2.5).sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun ProfileInfoRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(value, style = MaterialTheme.typography.bodyLarge) },
        overlineContent = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingContent = {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.primary
            ) { icon() }
        },
        trailingContent = trailingContent
    )
}

@Preview(showBackground = true, name = "Perfil")
@Composable
private fun ProfilePreview() {
    BolaoTheme {
        @OptIn(ExperimentalMaterial3Api::class)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Meu Perfil") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UserAvatar(name = "Gerson Ramos", size = 80)
                    Text(
                        "Gerson Ramos",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
