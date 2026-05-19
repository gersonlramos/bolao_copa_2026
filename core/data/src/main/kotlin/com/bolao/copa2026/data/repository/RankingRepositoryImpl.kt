package com.bolao.copa2026.data.repository

import com.bolao.copa2026.data.mapper.toRankingEntry
import com.bolao.copa2026.data.source.FirestoreRankingDataSource
import com.bolao.copa2026.domain.model.RankingEntry
import com.bolao.copa2026.domain.repository.RankingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RankingRepositoryImpl @Inject constructor(
    private val rankingDataSource: FirestoreRankingDataSource
) : RankingRepository {

    override fun observeRanking(groupId: String): Flow<List<RankingEntry>> =
        rankingDataSource.observeRanking(groupId).map { docs ->
            // Docs come pre-sorted by Firestore (totalScore DESC)
            var position = 1
            docs.mapIndexed { index, doc ->
                val prev = if (index > 0) docs[index - 1].getLong("totalScore") ?: 0L else Long.MAX_VALUE
                val curr = doc.getLong("totalScore") ?: 0L
                if (curr < prev) position = index + 1
                doc.toRankingEntry(position)
            }
        }
}
