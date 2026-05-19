package com.bolao.copa2026.feature.matches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bolao.copa2026.domain.model.Bet
import com.bolao.copa2026.domain.model.Match
import com.bolao.copa2026.domain.model.MatchStatus
import com.bolao.copa2026.ui.theme.BolaoTheme
import com.bolao.copa2026.ui.util.tlaToFlag
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BRT = ZoneId.of("America/Sao_Paulo")
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM").withZone(BRT)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(BRT)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchListScreen(
    groupId: String,
    onMatchClick: (String) -> Unit,
    onRankingClick: (String) -> Unit,
    viewModel: MatchListViewModel = hiltViewModel()
) {
    val state by remember(groupId) { viewModel.uiState(groupId) }.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Partidas") }) },
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
    val date = dateFormatter.format(match.scheduledAt)
    val time = timeFormatter.format(match.scheduledAt)

    ListItem(
        headlineContent = {
            Text(
                "$homeFlag ${match.homeTeam}  ×  ${match.awayTeam} $awayFlag",
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusLabel(match.status)
                    if (match.status == MatchStatus.FINISHED) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${match.scoreHome} – ${match.scoreAway}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    userBet?.let { bet ->
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "· Palpite: ${bet.homeGoals} x ${bet.awayGoals}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
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
private fun StatusLabel(status: MatchStatus) {
    val (label, color) = when (status) {
        MatchStatus.SCHEDULED -> "Aguardando" to MaterialTheme.colorScheme.outline
        MatchStatus.IN_PROGRESS -> "Em andamento" to MaterialTheme.colorScheme.primary
        MatchStatus.FINISHED -> "Encerrado" to MaterialTheme.colorScheme.secondary
    }
    Text(label, style = MaterialTheme.typography.labelSmall, color = color)
}

private fun fakeMatch(
    id: String, home: String, homeTla: String, away: String, awayTla: String,
    venue: String? = null, status: MatchStatus, sh: Int? = null, sa: Int? = null
) = Match(id, "A", home, homeTla, away, awayTla, venue, Instant.now(), status, sh, sa)

@Preview(showBackground = true, name = "Lista de Partidas")
@Composable
private fun MatchListPreview() {
    val matches = listOf(
        fakeMatch("1", "Brasil", "BRA", "México", "MEX", "SoFi Stadium", MatchStatus.SCHEDULED),
        fakeMatch("2", "Argentina", "ARG", "França", "FRA", "MetLife Stadium", MatchStatus.IN_PROGRESS),
        fakeMatch("3", "Alemanha", "GER", "Espanha", "ESP", "Allegiant Stadium", MatchStatus.FINISHED, 2, 1),
        fakeMatch("4", "Portugal", "POR", "Itália", "ITA", null, MatchStatus.SCHEDULED),
        fakeMatch("5", "EUA", "USA", "Japão", "JPN", "Rose Bowl", MatchStatus.FINISHED, 0, 0),
    )
    BolaoTheme {
        @OptIn(ExperimentalMaterial3Api::class)
        Scaffold(topBar = { TopAppBar(title = { Text("Partidas") }) }) { padding ->
            LazyColumn(Modifier.padding(padding)) {
                items(matches) { MatchItem(it, null, onClick = {}) }
            }
        }
    }
}
