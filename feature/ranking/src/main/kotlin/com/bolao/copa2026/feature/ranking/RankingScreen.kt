package com.bolao.copa2026.feature.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bolao.copa2026.domain.model.RankingEntry
import com.bolao.copa2026.ui.theme.BolaoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    groupId: String,
    viewModel: RankingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ranking") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (state.rankingStale) {
                        Text(
                            "⚠ desatualizado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, modifier = Modifier.align(Alignment.Center))
                state.entries.isEmpty() -> Text(
                    "Nenhuma pontuação ainda",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.entries) { entry ->
                        RankingRow(
                            entry = entry,
                            isCurrentUser = entry.userId == state.currentUserId
                        )
                    }
                }
            }
        }
    }
}

private fun medalFor(position: Int): String? = when (position) {
    1 -> "🥇"
    2 -> "🥈"
    3 -> "🥉"
    else -> null
}

@Composable
private fun RankingRow(entry: RankingEntry, isCurrentUser: Boolean) {
    val medal = medalFor(entry.position)
    val isTopThree = entry.position <= 3

    val bgColor = when {
        isCurrentUser -> MaterialTheme.colorScheme.primaryContainer
        isTopThree    -> MaterialTheme.colorScheme.surfaceVariant
        else          -> MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position / medal
        Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            if (medal != null) {
                Text(medal, fontSize = 22.sp)
            } else {
                Text(
                    "${entry.position}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Name
        Text(
            text = if (isCurrentUser) "${entry.displayName} (você)" else entry.displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCurrentUser || isTopThree) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        // Score
        Text(
            "${entry.totalScore} pts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Preview(showBackground = true, name = "Ranking")
@Composable
private fun RankingPreview() {
    val entries = listOf(
        RankingEntry(1, "u1", "Alice Silva", 85),
        RankingEntry(2, "u2", "Bruno Costa", 70),
        RankingEntry(3, "u3", "Carol Mendes", 65),
        RankingEntry(4, "me", "Você", 55),
        RankingEntry(5, "u5", "Eduardo Lima", 30),
    )
    BolaoTheme {
        @OptIn(ExperimentalMaterial3Api::class)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Ranking") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            LazyColumn(Modifier.padding(padding)) {
                items(entries) { RankingRow(it, isCurrentUser = it.userId == "me") }
            }
        }
    }
}
