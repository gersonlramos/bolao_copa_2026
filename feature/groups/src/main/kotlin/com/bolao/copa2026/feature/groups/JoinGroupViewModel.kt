package com.bolao.copa2026.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.domain.model.Group
import com.bolao.copa2026.domain.usecase.JoinGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinGroupUiState(
    val inviteCode: String = "",
    val isLoading: Boolean = false,
    val joined: Group? = null,
    val error: String? = null
)

@HiltViewModel
class JoinGroupViewModel @Inject constructor(
    private val joinGroupUseCase: JoinGroupUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(JoinGroupUiState())
    val uiState: StateFlow<JoinGroupUiState> = _state.asStateFlow()

    fun onCodeChange(v: String) = _state.update { it.copy(inviteCode = v.uppercase().take(8), error = null) }

    fun join() {
        val code = _state.value.inviteCode.trim()
        if (code.length != 8) {
            _state.update { it.copy(error = "Código deve ter 8 caracteres") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            joinGroupUseCase(code)
                .onSuccess { group -> _state.update { it.copy(isLoading = false, joined = group) } }
                .onFailure { e ->
                    val msg = when {
                        e.message?.contains("InvalidInviteCode") == true -> "Código inválido"
                        e.message?.contains("ExpiredInviteCode") == true -> "Código expirado"
                        e.message?.contains("AlreadyMember") == true -> "Você já é membro deste grupo"
                        e.message?.contains("GroupFull") == true -> "Grupo cheio"
                        e.message?.contains("GroupLimitReached") == true -> "Você atingiu o limite de grupos. Seja VIP para participar de mais grupos."
                        else -> "Erro ao entrar no grupo"
                    }
                    _state.update { it.copy(isLoading = false, error = msg) }
                }
        }
    }

    fun clearJoined() = _state.update { it.copy(joined = null) }
}
