package com.bolao.copa2026.domain.repository

import com.bolao.copa2026.domain.model.Match
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    fun observeMatches(): Flow<List<Match>>
    fun observeMatch(matchId: String): Flow<Match>
}
