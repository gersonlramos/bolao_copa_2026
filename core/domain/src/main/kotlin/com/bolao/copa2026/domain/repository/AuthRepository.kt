package com.bolao.copa2026.domain.repository

import com.bolao.copa2026.domain.model.LoginBlockStatus
import com.bolao.copa2026.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun register(displayName: String, email: String, password: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun loginWithGoogle(idToken: String): Result<User>
    suspend fun logout()
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun updateDisplayName(name: String): Result<Unit>
    fun isEmailPasswordUser(): Boolean
    fun currentUser(): Flow<User?>
    suspend fun checkLoginBlock(email: String): LoginBlockStatus
}
