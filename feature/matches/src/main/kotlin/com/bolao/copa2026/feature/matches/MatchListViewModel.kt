package com.bolao.copa2026.feature.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.domain.model.Bet
import com.bolao.copa2026.domain.model.Match
import com.bolao.copa2026.domain.repository.AuthRepository
import com.bolao.copa2026.domain.repository.BetRepository
import com.bolao.copa2026.domain.repository.GroupRepository
import com.bolao.copa2026.domain.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MatchListUiState(
    val matches: List<Match> = emptyList(),
    val userBets: Map<String, Bet> = emptyMap(),
    val groupName: String = "",
    val isAdmin: Boolean = false,
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MatchListViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val betRepository: BetRepository,
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    fun uiState(groupId: String): StateFlow<MatchListUiState> = combine(
        matchRepository.observeMatches(),
        betRepository.observeUserBets(groupId),
        groupRepository.observeGroup(groupId),
        authRepository.currentUser()
    ) { matches, bets, group, user ->
        MatchListUiState(
            matches = matches,
            userBets = bets.associateBy { it.matchId },
            groupName = group.name,
            isAdmin = user?.id == group.adminUserId,
            isLoading = false
        )
    }
        .catch { e -> emit(MatchListUiState(isLoading = false, error = e.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MatchListUiState())

    private val _deleteState = MutableStateFlow<Pair<Boolean, Boolean>>(false to false) // isDeleting to deleted
    val deleteState: StateFlow<Pair<Boolean, Boolean>> = _deleteState.asStateFlow()

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            _deleteState.value = true to false
            groupRepository.deleteGroup(groupId)
                .onSuccess { _deleteState.value = false to true }
                .onFailure { _deleteState.value = false to false }
        }
    }
}
