package com.bolao.copa2026.domain.usecase

import com.bolao.copa2026.domain.repository.AuthRepository
import javax.inject.Inject

class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> =
        authRepository.sendPasswordResetEmail(email)
}
