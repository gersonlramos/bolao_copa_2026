package com.bolao.copa2026.domain.usecase

import com.bolao.copa2026.domain.model.Group
import com.bolao.copa2026.domain.repository.GroupRepository
import javax.inject.Inject

class JoinGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(inviteCode: String): Result<Group> =
        groupRepository.joinGroup(inviteCode)
}
