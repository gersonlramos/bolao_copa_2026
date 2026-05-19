package com.bolao.copa2026.feature.bets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.domain.model.Bet
import com.bolao.copa2026.domain.model.BetWithUser
import com.bolao.copa2026.domain.model.Match
import com.bolao.copa2026.domain.model.MatchStatus
import com.bolao.copa2026.domain.repository.BetRepository
import com.bolao.copa2026.domain.repository.MatchRepository
import com.bolao.copa2026.domain.usecase.SaveBetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BetUiState(
    val match: Match? = null,
    val homeGoalsInput: String = "",
    val awayGoalsInput: String = "",
    val existingBet: Bet? = null,
    val allBets: List<BetWithUser> = emptyList(),
    val isDeadlinePassed: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class BetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val matchRepository: MatchRepository,
    private val betRepository: BetRepository,
    private val saveBetUseCase: SaveBetUseCase
) : ViewModel() {

    private val groupId: String = savedStateHandle["groupId"] ?: ""
    private val matchId: String = savedStateHandle["matchId"] ?: ""

    private val _state = MutableStateFlow(BetUiState())
    val uiState: StateFlow<BetUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            matchRepository.observeMatch(matchId).collect { match ->
                val deadlinePassed = match.status != MatchStatus.SCHEDULED
                _state.update { it.copy(match = match, isDeadlinePassed = deadlinePassed, isLoading = false) }
            }
        }
        viewModelScope.launch {
            betRepository.observeUserBets(groupId).collect { bets ->
                val myBet = bets.firstOrNull { it.matchId == matchId }
                _state.update { s ->
                    s.copy(
                        existingBet = myBet,
                        homeGoalsInput = myBet?.homeGoals?.toString() ?: s.homeGoalsInput,
                        awayGoalsInput = myBet?.awayGoals?.toString() ?: s.awayGoalsInput
                    )
                }
            }
        }
        viewModelScope.launch {
            betRepository.observeBetsForMatch(groupId, matchId).collect { bets ->
                _state.update { it.copy(allBets = bets) }
            }
        }
    }

    fun onHomeGoalsChange(v: String) {
        if (v.length <= 2 && v.all { it.isDigit() }) _state.update { it.copy(homeGoalsInput = v) }
    }

    fun onAwayGoalsChange(v: String) {
        if (v.length <= 2 && v.all { it.isDigit() }) _state.update { it.copy(awayGoalsInput = v) }
    }

    fun saveBet() {
        val homeGoals = _state.value.homeGoalsInput.toIntOrNull() ?: run {
            _state.update { it.copy(error = "Gols inválidos") }; return
        }
        val awayGoals = _state.value.awayGoalsInput.toIntOrNull() ?: run {
            _state.update { it.copy(error = "Gols inválidos") }; return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            saveBetUseCase(groupId, matchId, homeGoals, awayGoals)
                .onSuccess { _state.update { s -> s.copy(isSaving = false, saved = true) } }
                .onFailure { e ->
                    val msg = if (e.message?.contains("BetDeadlinePassed") == true)
                        "Prazo encerrado para esta partida"
                    else "Erro ao salvar palpite"
                    _state.update { s -> s.copy(isSaving = false, error = msg) }
                }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
    fun clearSaved() = _state.update { it.copy(saved = false) }
}
