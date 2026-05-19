package com.bolao.copa2026.domain.usecase

import com.bolao.copa2026.domain.model.BetMode
import com.bolao.copa2026.domain.model.Group
import com.bolao.copa2026.domain.model.ScoringSystem
import com.bolao.copa2026.domain.repository.GroupRepository
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(
        name: String,
        betMode: BetMode,
        scoringSystem: ScoringSystem
    ): Result<Group> = groupRepository.createGroup(name, betMode, scoringSystem)
}
