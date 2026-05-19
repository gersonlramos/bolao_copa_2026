package com.bolao.copa2026.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.domain.model.BetMode
import com.bolao.copa2026.domain.model.Group
import com.bolao.copa2026.domain.model.ScoringSystem
import com.bolao.copa2026.domain.usecase.CreateGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateGroupUiState(
    val name: String = "",
    val betMode: BetMode = BetMode.PRE_CUP,
    val exactScore: String = "10",
    val correctWinner: String = "7",
    val correctWinnerLoser: String = "5",
    val correctDraw: String = "4",
    val nameError: String? = null,
    val scoringError: String? = null,
    val isLoading: Boolean = false,
    val created: Group? = null,
    val error: String? = null
)

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _state.asStateFlow()

    fun onNameChange(v: String) = _state.update { it.copy(name = v, nameError = null) }
    fun onBetModeChange(v: BetMode) = _state.update { it.copy(betMode = v) }
    fun onExactScoreChange(v: String) = _state.update { it.copy(exactScore = v, scoringError = null) }
    fun onCorrectWinnerChange(v: String) = _state.update { it.copy(correctWinner = v, scoringError = null) }
    fun onCorrectWinnerLoserChange(v: String) = _state.update { it.copy(correctWinnerLoser = v, scoringError = null) }
    fun onCorrectDrawChange(v: String) = _state.update { it.copy(correctDraw = v, scoringError = null) }

    fun create() {
        val s = _state.value
        val name = s.name.trim()
        if (name.isEmpty() || name.length > 50) {
            _state.update { it.copy(nameError = "Nome deve ter entre 1 e 50 caracteres") }
            return
        }

        val exact = s.exactScore.toIntOrNull() ?: run {
            _state.update { it.copy(scoringError = "Pontuações inválidas") }; return
        }
        val cw = s.correctWinner.toIntOrNull() ?: run {
            _state.update { it.copy(scoringError = "Pontuações inválidas") }; return
        }
        val cwl = s.correctWinnerLoser.toIntOrNull() ?: run {
            _state.update { it.copy(scoringError = "Pontuações inválidas") }; return
        }
        val cd = s.correctDraw.toIntOrNull() ?: run {
            _state.update { it.copy(scoringError = "Pontuações inválidas") }; return
        }

        val scoringSystem = runCatching { ScoringSystem(exact, cw, cwl, cd) }.getOrElse {
            _state.update { it.copy(scoringError = "Pontos devem estar entre 0 e 999") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            createGroupUseCase(name, s.betMode, scoringSystem)
                .onSuccess { group -> _state.update { it.copy(isLoading = false, created = group) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun clearCreated() = _state.update { it.copy(created = null) }
}
