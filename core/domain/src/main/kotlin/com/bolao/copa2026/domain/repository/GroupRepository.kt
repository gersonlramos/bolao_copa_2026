package com.bolao.copa2026.domain.repository

import com.bolao.copa2026.domain.model.BetMode
import com.bolao.copa2026.domain.model.Group
import com.bolao.copa2026.domain.model.ScoringSystem
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    suspend fun createGroup(name: String, betMode: BetMode, scoringSystem: ScoringSystem): Result<Group>
    suspend fun joinGroup(inviteCode: String): Result<Group>
    fun observeUserGroups(): Flow<List<Group>>
    fun observeGroup(groupId: String): Flow<Group>
    suspend fun updateScoringSystem(groupId: String, scoringSystem: ScoringSystem): Result<Unit>
    suspend fun updateMemberDisplayNameInAllGroups(userId: String, newName: String): Result<Unit>
}
