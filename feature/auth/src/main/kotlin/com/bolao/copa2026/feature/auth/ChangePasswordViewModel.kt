package com.bolao.copa2026.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.common.validation.validatePassword
import com.bolao.copa2026.common.validation.ValidationResult
import com.bolao.copa2026.domain.usecase.UpdatePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val updatePasswordUseCase: UpdatePasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onCurrentPasswordChange(v: String) = _uiState.update { it.copy(currentPassword = v, currentPasswordError = null, error = null) }
    fun onNewPasswordChange(v: String)     = _uiState.update { it.copy(newPassword = v, newPasswordError = null) }
    fun onConfirmPasswordChange(v: String) = _uiState.update { it.copy(confirmPassword = v, confirmPasswordError = null) }

    fun save() {
        val s = _uiState.value

        val currentErr = if (s.currentPassword.isBlank()) "Informe a senha atual" else null
        val newErr = (validatePassword(s.newPassword) as? ValidationResult.Invalid)?.message
        val confirmErr = if (s.newPassword != s.confirmPassword) "As senhas não coincidem" else null

        if (currentErr != null || newErr != null || confirmErr != null) {
            _uiState.update {
                it.copy(
                    currentPasswordError = currentErr,
                    newPasswordError = newErr,
                    confirmPasswordError = confirmErr
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            updatePasswordUseCase(s.currentPassword, s.newPassword)
                .onSuccess {
                    _uiState.update { st -> st.copy(isLoading = false, success = true) }
                }
                .onFailure { e ->
                    val msg = when {
                        e.message?.contains("InvalidCredentials") == true ||
                        e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                            "Senha atual incorreta"
                        else -> "Erro ao alterar senha. Tente novamente."
                    }
                    _uiState.update { st -> st.copy(isLoading = false, error = msg) }
                }
        }
    }
}
