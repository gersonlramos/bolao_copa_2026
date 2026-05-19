package com.bolao.copa2026.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.common.validation.*
import com.bolao.copa2026.domain.model.AppError
import com.bolao.copa2026.domain.usecase.LoginUserUseCase
import com.bolao.copa2026.domain.usecase.LoginWithGoogleUseCase
import com.bolao.copa2026.domain.usecase.RegisterUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val displayNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val systemError: String? = null,
    val navigateToHome: Boolean = false,
    val blockMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val registerUseCase: RegisterUserUseCase,
    private val loginUseCase: LoginUserUseCase,
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onDisplayNameChange(value: String) = _uiState.update { it.copy(displayName = value, displayNameError = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, emailError = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, passwordError = null) }

    fun register() {
        val state = _uiState.value
        val nameResult = validateDisplayName(state.displayName)
        val emailResult = validateEmail(state.email)
        val passResult = validatePassword(state.password)

        if (!nameResult.isValid || !emailResult.isValid || !passResult.isValid) {
            _uiState.update {
                it.copy(
                    displayNameError = (nameResult as? ValidationResult.Invalid)?.message,
                    emailError = (emailResult as? ValidationResult.Invalid)?.message,
                    passwordError = (passResult as? ValidationResult.Invalid)?.message
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, systemError = null) }
            registerUseCase(state.displayName, state.email, state.password)
                .onSuccess { _uiState.update { s -> s.copy(isLoading = false, navigateToHome = true) } }
                .onFailure { e -> _uiState.update { s -> s.copy(isLoading = false, systemError = e.toUserMessage()) } }
        }
    }

    fun login() {
        val state = _uiState.value
        val emailResult = validateEmail(state.email)
        val passResult = validatePassword(state.password)

        if (!emailResult.isValid || !passResult.isValid) {
            _uiState.update {
                it.copy(
                    emailError = (emailResult as? ValidationResult.Invalid)?.message,
                    passwordError = (passResult as? ValidationResult.Invalid)?.message
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, systemError = null, blockMessage = null) }
            loginUseCase(state.email, state.password)
                .onSuccess { _uiState.update { s -> s.copy(isLoading = false, navigateToHome = true) } }
                .onFailure { e ->
                    val blockMsg = if (e.message?.contains("AccountBlocked") == true) {
                        "Conta bloqueada. Tente novamente mais tarde."
                    } else null
                    _uiState.update { s ->
                        s.copy(
                            isLoading = false,
                            systemError = if (blockMsg == null) e.toUserMessage() else null,
                            blockMessage = blockMsg
                        )
                    }
                }
        }
    }

    fun onGoogleSignInResult(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, systemError = null) }
            loginWithGoogleUseCase(idToken)
                .onSuccess { _uiState.update { s -> s.copy(isLoading = false, navigateToHome = true) } }
                .onFailure { e -> _uiState.update { s -> s.copy(isLoading = false, systemError = e.toUserMessage()) } }
        }
    }

    fun clearNavigation() = _uiState.update { it.copy(navigateToHome = false) }
    fun clearError() = _uiState.update { it.copy(systemError = null) }

    private fun Throwable.toUserMessage(): String = when {
        message?.contains("EmailAlreadyRegistered") == true -> "Este e-mail já está cadastrado"
        message?.contains("InvalidCredentials") == true -> "E-mail ou senha incorretos"
        message?.contains("NetworkUnavailable") == true -> "Sem conexão com a internet"
        else -> "Erro: ${this.localizedMessage ?: "Ocorreu um erro. Tente novamente."}"
    }
}
