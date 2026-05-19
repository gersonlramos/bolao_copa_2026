package com.bolao.copa2026.domain.usecase

import com.bolao.copa2026.domain.model.User
import com.bolao.copa2026.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> =
        authRepository.login(email, password)
}
