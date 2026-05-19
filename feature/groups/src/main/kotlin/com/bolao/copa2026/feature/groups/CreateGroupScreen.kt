package com.bolao.copa2026.feature.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bolao.copa2026.domain.model.BetMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onGroupCreated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.created) {
        state.created?.let { group ->
            viewModel.clearCreated()
            onGroupCreated(group.id)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Criar Grupo") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nome do grupo") },
                isError = state.nameError != null,
                supportingText = { state.nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Modo de palpite", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BetMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.betMode == mode,
                        onClick = { viewModel.onBetModeChange(mode) },
                        label = { Text(if (mode == BetMode.PRE_CUP) "Pré-Copa" else "Pré-Partida") }
                    )
                }
            }

            Text("Pontuação", style = MaterialTheme.typography.labelLarge)

            listOf(
                Triple("Placar exato", state.exactScore, viewModel::onExactScoreChange),
                Triple("Vencedor + gols do vencedor", state.correctWinner, viewModel::onCorrectWinnerChange),
                Triple("Vencedor + gols do perdedor", state.correctWinnerLoser, viewModel::onCorrectWinnerLoserChange),
                Triple("Empate certo", state.correctDraw, viewModel::onCorrectDrawChange),
            ).forEach { (label, value, onChange) ->
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    label = { Text(label) },
                    isError = state.scoringError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            state.scoringError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            // Warning when exactScore < correctWinner
            val exact = state.exactScore.toIntOrNull() ?: 0
            val cw = state.correctWinner.toIntOrNull() ?: 0
            if (exact < cw) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        "Atenção: o placar exato vale menos que acertar o vencedor. Confirma?",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::create,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Criar Grupo")
            }
        }
    }
}
