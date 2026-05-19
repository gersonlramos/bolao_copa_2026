package com.bolao.copa2026.feature.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.domain.model.Bet
import com.bolao.copa2026.domain.model.Match
import com.bolao.copa2026.domain.repository.BetRepository
import com.bolao.copa2026.domain.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class MatchListUiState(
    val matches: List<Match> = emptyList(),
    val userBets: Map<String, Bet> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class MatchListViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val betRepository: BetRepository
) : ViewModel() {

    fun uiState(groupId: String): StateFlow<MatchListUiState> = 
        combine(
            matchRepository.observeMatches(),
            betRepository.observeUserBets(groupId)
        ) { matches, bets ->
            MatchListUiState(
                matches = matches,
                userBets = bets.associateBy { it.matchId },
                isLoading = false
            )
        }
        .catch { e -> emit(MatchListUiState(isLoading = false, error = e.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MatchListUiState())
}
