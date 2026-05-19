package com.bolao.copa2026.feature.ranking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.domain.model.RankingEntry
import com.bolao.copa2026.domain.repository.AuthRepository
import com.bolao.copa2026.domain.usecase.GetRankingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class RankingUiState(
    val entries: List<RankingEntry> = emptyList(),
    val currentUserId: String = "",
    val isLoading: Boolean = true,
    val rankingStale: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RankingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRankingUseCase: GetRankingUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val groupId: String = savedStateHandle["groupId"] ?: ""

    val uiState: StateFlow<RankingUiState> = getRankingUseCase(groupId)
        .combine(authRepository.currentUser()) { entries, user ->
            RankingUiState(
                entries = entries,
                currentUserId = user?.id ?: "",
                isLoading = false
            )
        }
        .catch { e -> emit(RankingUiState(isLoading = false, error = e.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RankingUiState())
}
