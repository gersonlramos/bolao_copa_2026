package com.bolao.copa2026.domain.usecase

import com.bolao.copa2026.domain.model.ScoringSystem
import com.bolao.copa2026.domain.repository.GroupRepository
import javax.inject.Inject

class RecalculateGroupScoresUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupId: String, scoringSystem: ScoringSystem): Result<Unit> =
        groupRepository.updateScoringSystem(groupId, scoringSystem)
}
