package com.bolao.copa2026.feature.matches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bolao.copa2026.domain.model.Bet
import com.bolao.copa2026.domain.model.Match
import com.bolao.copa2026.domain.model.MatchStatus
import com.bolao.copa2026.ui.theme.BolaoTheme
import com.bolao.copa2026.ui.util.tlaToFlag
import com.bolao.copa2026.ui.util.tlaToName
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BRT = ZoneId.of("America/Sao_Paulo")
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM").withZone(BRT)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(BRT)

private val ChipShape = RoundedCornerShape(50)

// Status chip colors
private val ColorScheduledBg   = Color(0xFFEEEEEE)
private val ColorScheduledText = Color(0xFF757575)
private val ColorActiveBg      = Color(0xFFE8F5E9)
private val ColorActiveText    = Color(0xFF2E7D32)
private val ColorFinishedBg    = Color(0xFFE3F2FD)
private val ColorFinishedText  = Color(0xFF1565C0)
private val ColorAlertBg       = Color(0xFFFFEBEE)
private val ColorAlertText     = Color(0xFFC62828)

// Bet chip colors
private val ColorBetBg   = Color(0xFF006B3C)
private val ColorBetText = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchListScreen(
    groupId: String,
    onMatchClick: (String) -> Unit,
    onRankingClick: (String) -> Unit,
    onGroupDeleted: () -> Unit = {},
    viewModel: MatchListViewModel = hiltViewModel()
) {
    val state by remember(groupId) { viewModel.uiState(groupId) }.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val (isDeleting, deleted) = deleteState

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) {
        if (deleted) onGroupDeleted()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Excluir grupo") },
            text = { Text("Tem certeza que deseja excluir o grupo \"${state.groupName}\"? Essa ação é irreversível e todos os palpites serão perdidos.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.deleteGroup(groupId) }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.groupName.ifEmpty { "Partidas" }) },
                actions = {
                    if (state.isAdmin) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Excluir grupo",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onRankingClick(groupId) },
                icon = { Icon(Icons.Default.List, contentDescription = null) },
                text = { Text("Ranking") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isDeleting -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.matches) { match ->
                        MatchItem(
                            match = match,
                            userBet = state.userBets[match.id],
                            onClick = { onMatchClick(match.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchItem(match: Match, userBet: Bet?, onClick: () -> Unit) {
    val homeFlag = tlaToFlag(match.homeTeamTla)
    val awayFlag = tlaToFlag(match.awayTeamTla)
    val homeName = tlaToName(match.homeTeamTla, match.homeTeam)
    val awayName = tlaToName(match.awayTeamTla, match.awayTeam)
    val date = dateFormatter.format(match.scheduledAt)
    val time = timeFormatter.format(match.scheduledAt)

    ListItem(
        headlineContent = {
            Text(
                "$homeFlag $homeName  ×  $awayName $awayFlag",
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "$date • $time (BRT)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!match.venue.isNullOrBlank()) {
                    Text(
                        "📍 ${match.venue}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusChip(match.status)
                    if (match.status == MatchStatus.FINISHED) {
                        Text(
                            "${match.scoreHome} – ${match.scoreAway}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorFinishedText
                        )
                    }
                    userBet?.let { bet ->
                        val label = if (match.status == MatchStatus.FINISHED && bet.score != null)
                            "${bet.homeGoals} x ${bet.awayGoals}  •  +${bet.score} pts"
                        else
                            "${bet.homeGoals} x ${bet.awayGoals}"
                        BetChip(label)
                    }
                }
            }
        },
        trailingContent = {
            if (match.status == MatchStatus.SCHEDULED) {
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Dar palpite",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Ver detalhes"
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}

@Composable
private fun StatusChip(status: MatchStatus) {
    val (label, bg, fg) = when (status) {
        MatchStatus.SCHEDULED  -> Triple("Aguardando",   ColorScheduledBg, ColorScheduledText)
        MatchStatus.IN_PROGRESS -> Triple("Em andamento", ColorActiveBg,    ColorActiveText)
        MatchStatus.FINISHED   -> Triple("Encerrado",    ColorFinishedBg,  ColorFinishedText)
        MatchStatus.POSTPONED  -> Triple("Adiado",       ColorAlertBg,     ColorAlertText)
        MatchStatus.CANCELLED  -> Triple("Cancelado",    ColorAlertBg,     ColorAlertText)
        MatchStatus.SUSPENDED  -> Triple("Interrompido", ColorAlertBg,     ColorAlertText)
    }
    Surface(shape = ChipShape, color = bg) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun BetChip(score: String) {
    Surface(shape = ChipShape, color = ColorBetBg) {
        Text(
            "Palpite: $score",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = ColorBetText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

private fun fakeMatch(
    id: String, home: String, homeTla: String, away: String, awayTla: String,
    venue: String? = null, status: MatchStatus, sh: Int? = null, sa: Int? = null
) = Match(id, "A", home, homeTla, away, awayTla, venue, Instant.now(), status, sh, sa)

@Preview(showBackground = true, name = "Lista de Partidas")
@Composable
private fun MatchListPreview() {
    val fakeBet = Bet("b1", "u1", "3", 2, 1, 7, null, Instant.now(), Instant.now())
    val matches = listOf(
        fakeMatch("1", "Brasil", "BRA", "Marrocos", "MAR", "SoFi Stadium", MatchStatus.SCHEDULED),
        fakeMatch("2", "Argentina", "ARG", "França", "FRA", "MetLife Stadium", MatchStatus.IN_PROGRESS),
        fakeMatch("3", "Alemanha", "GER", "Espanha", "ESP", "Allegiant Stadium", MatchStatus.FINISHED, 2, 1),
        fakeMatch("4", "Portugal", "POR", "Itália", "ITA", null, MatchStatus.POSTPONED),
        fakeMatch("5", "EUA", "USA", "Japão", "JPN", "Rose Bowl", MatchStatus.CANCELLED),
    )
    BolaoTheme {
        @OptIn(ExperimentalMaterial3Api::class)
        Scaffold(topBar = { TopAppBar(title = { Text("Partidas") }) }) { padding ->
            LazyColumn(Modifier.padding(padding)) {
                items(matches) { match ->
                    MatchItem(
                        match = match,
                        userBet = if (match.id == "1") fakeBet else null,
                        onClick = {}
                    )
                }
            }
        }
    }
}
