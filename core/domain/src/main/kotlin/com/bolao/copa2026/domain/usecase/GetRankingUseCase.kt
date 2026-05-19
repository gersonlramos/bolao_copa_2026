package com.bolao.copa2026.domain.usecase

import com.bolao.copa2026.domain.model.RankingEntry
import com.bolao.copa2026.domain.repository.RankingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRankingUseCase @Inject constructor(
    private val rankingRepository: RankingRepository
) {
    operator fun invoke(groupId: String): Flow<List<RankingEntry>> =
        rankingRepository.observeRanking(groupId)
}
