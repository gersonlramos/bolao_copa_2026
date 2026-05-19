package com.bolao.copa2026

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.domain.repository.AuthRepository
import com.bolao.copa2026.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    val isLoggedIn: Flow<Boolean> = authRepository.currentUser().map { it != null }

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }
}
