package com.bolao.copa2026.domain.usecase

import com.bolao.copa2026.domain.model.User
import com.bolao.copa2026.domain.repository.AuthRepository
import javax.inject.Inject

class LoginWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<User> =
        authRepository.loginWithGoogle(idToken)
}
