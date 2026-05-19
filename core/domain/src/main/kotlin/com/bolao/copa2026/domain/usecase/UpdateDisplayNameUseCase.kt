package com.bolao.copa2026.domain.usecase

import com.bolao.copa2026.domain.repository.AuthRepository
import javax.inject.Inject

class UpdateDisplayNameUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(name: String): Result<Unit> =
        authRepository.updateDisplayName(name)
}
