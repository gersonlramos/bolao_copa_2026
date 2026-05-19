package com.bolao.copa2026.feature.bets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bolao.copa2026.domain.model.MatchStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetScreen(
    onBack: () -> Unit,
    viewModel: BetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            isEditing = false
            snackbarHostState.showSnackbar("Palpite salvo!")
            viewModel.clearSaved()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = {
                Text(state.match?.let { "${it.homeTeam} x ${it.awayTeam}" } ?: "Palpite")
            })
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val match = state.match ?: return@Column

            if (state.isDeadlinePassed) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (match.status == MatchStatus.FINISHED)
                            "Partida encerrada — ${match.scoreHome} x ${match.scoreAway}"
                        else
                            "Prazo encerrado para palpites",
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // Team names header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(match.homeTeam, style = MaterialTheme.typography.titleMedium)
                Text("x", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(match.awayTeam, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(32.dp))

            val existingBet = state.existingBet
            val showForm = existingBet == null || (isEditing && !state.isDeadlinePassed)

            if (showForm) {
                // Input form
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.homeGoalsInput,
                        onValueChange = viewModel::onHomeGoalsChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        singleLine = true
                    )
                    Text(
                        "  ×  ",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    OutlinedTextField(
                        value = state.awayGoalsInput,
                        onValueChange = viewModel::onAwayGoalsChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isEditing && existingBet != null) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                    }
                    Button(
                        onClick = viewModel::saveBet,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (existingBet != null) "Salvar alteração" else "Salvar palpite")
                        }
                    }
                }
            } else if (existingBet != null) {
                // View mode — show saved bet as clean score
                Text(
                    "Seu palpite",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        existingBet.homeGoals.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "  ×  ",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        existingBet.awayGoals.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (!state.isDeadlinePassed) {
                    Spacer(Modifier.height(32.dp))
                    OutlinedButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Editar palpite")
                    }
                }
            }
        }
    }
}
