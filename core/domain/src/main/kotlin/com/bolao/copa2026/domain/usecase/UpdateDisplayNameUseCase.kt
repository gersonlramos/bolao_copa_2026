package com.bolao.copa2026.domain.usecase

import com.bolao.copa2026.domain.repository.AuthRepository
import com.bolao.copa2026.domain.repository.GroupRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class UpdateDisplayNameUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(name: String): Result<Unit> {
        val user = authRepository.currentUser().firstOrNull() 
            ?: return Result.failure(Exception("Usuário não logado"))
            
        // 1. Update Profile (Auth + Users collection)
        val profileResult = authRepository.updateDisplayName(name)
        if (profileResult.isFailure) return profileResult
        
        // 2. Sync name in all groups
        return groupRepository.updateMemberDisplayNameInAllGroups(user.id, name)
    }
}
