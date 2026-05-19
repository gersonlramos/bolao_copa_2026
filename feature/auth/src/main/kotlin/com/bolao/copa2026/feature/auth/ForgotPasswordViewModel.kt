package com.bolao.copa2026.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.common.validation.validateEmail
import com.bolao.copa2026.common.validation.ValidationResult
import com.bolao.copa2026.domain.usecase.ForgotPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val emailSent: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null, error = null) }

    fun sendResetEmail() {
        val email = _uiState.value.email.trim()
        val emailResult = validateEmail(email)
        if (!emailResult.isValid) {
            _uiState.update {
                it.copy(emailError = (emailResult as? ValidationResult.Invalid)?.message)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            forgotPasswordUseCase(email)
                .onSuccess {
                    _uiState.update { s -> s.copy(isLoading = false, emailSent = true) }
                }
                .onFailure { e ->
                    _uiState.update { s ->
                        s.copy(isLoading = false, error = "Não foi possível enviar o e-mail. Verifique o endereço e tente novamente.")
                    }
                }
        }
    }
}
