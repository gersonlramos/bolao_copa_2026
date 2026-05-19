package com.bolao.copa2026.data.repository

import com.bolao.copa2026.data.mapper.toMatch
import com.bolao.copa2026.data.source.FirestoreMatchDataSource
import com.bolao.copa2026.domain.model.Match
import com.bolao.copa2026.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MatchRepositoryImpl @Inject constructor(
    private val matchDataSource: FirestoreMatchDataSource
) : MatchRepository {

    override fun observeMatches(): Flow<List<Match>> =
        matchDataSource.observeMatches().map { docs -> docs.map { it.toMatch() } }

    override fun observeMatch(matchId: String): Flow<Match> =
        matchDataSource.observeMatch(matchId).map { it!!.toMatch() }
}
