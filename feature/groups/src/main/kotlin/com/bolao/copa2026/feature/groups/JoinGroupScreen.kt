package com.bolao.copa2026.feature.groups

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(
    onGroupJoined: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: JoinGroupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.joined) {
        state.joined?.let { group ->
            viewModel.clearJoined()
            onGroupJoined(group.id)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Entrar em Grupo") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Código de convite", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.inviteCode,
                onValueChange = viewModel::onCodeChange,
                label = { Text("Código (8 caracteres)") },
                isError = state.error != null,
                supportingText = { state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = viewModel::join,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Entrar")
            }
        }
    }
}
