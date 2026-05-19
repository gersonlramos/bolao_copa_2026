package com.bolao.copa2026.domain.usecase

import com.bolao.copa2026.domain.repository.AuthRepository
import javax.inject.Inject

class UpdatePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(currentPassword: String, newPassword: String): Result<Unit> =
        authRepository.updatePassword(currentPassword, newPassword)
}
