package com.bolao.copa2026.domain.repository

import com.bolao.copa2026.domain.model.RankingEntry
import kotlinx.coroutines.flow.Flow

interface RankingRepository {
    fun observeRanking(groupId: String): Flow<List<RankingEntry>>
}
