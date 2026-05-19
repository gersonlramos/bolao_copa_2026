package com.bolao.copa2026.domain.usecase

import com.bolao.copa2026.domain.model.AppError
import com.bolao.copa2026.domain.model.User
import com.bolao.copa2026.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        displayName: String,
        email: String,
        password: String
    ): Result<User> = authRepository.register(displayName, email, password)
}
