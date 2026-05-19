package com.bolao.copa2026.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.domain.model.User
import com.bolao.copa2026.domain.repository.AuthRepository
import com.bolao.copa2026.domain.usecase.UpdateDisplayNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val nameInput: String = "",
    val isEditingName: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val updateDisplayNameUseCase: UpdateDisplayNameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val isEmailPasswordUser: Boolean = authRepository.isEmailPasswordUser()

    init {
        viewModelScope.launch {
            authRepository.currentUser().collect { user ->
                _uiState.update { it.copy(user = user, nameInput = user?.displayName ?: "") }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(nameInput = value) }
    }

    fun toggleEditingName() {
        _uiState.update { it.copy(isEditingName = !it.isEditingName, error = null) }
    }

    fun updateName() {
        val newName = _uiState.value.nameInput.trim()
        if (newName.isBlank()) {
            _uiState.update { it.copy(error = "O nome não pode estar vazio") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            updateDisplayNameUseCase(newName)
                .onSuccess {
                    _uiState.update { 
                        it.copy(isLoading = false, isEditingName = false, successMessage = "Nome atualizado com sucesso!") 
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = "Erro ao atualizar nome: ${e.message}") }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
