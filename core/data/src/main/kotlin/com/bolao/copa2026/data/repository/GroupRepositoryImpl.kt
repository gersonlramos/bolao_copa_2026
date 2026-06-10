package com.bolao.copa2026.data.repository

import com.bolao.copa2026.data.mapper.toGroup
import com.bolao.copa2026.data.mapper.toMap
import com.bolao.copa2026.data.source.FirestoreAuthDataSource
import com.bolao.copa2026.data.source.FirestoreGroupDataSource
import com.bolao.copa2026.domain.model.*
import com.bolao.copa2026.domain.repository.GroupRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val groupDataSource: FirestoreGroupDataSource,
    private val authDataSource: FirestoreAuthDataSource
) : GroupRepository {

    override suspend fun createGroup(
        name: String,
        betMode: BetMode,
        scoringSystem: ScoringSystem
    ): Result<Group> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")

        val userDoc = authDataSource.getUserDocument(uid)
        val isVip = userDoc?.getBoolean("isVip") ?: false
        if (!isVip && groupDataSource.getUserGroupCount(uid) >= 1) throw AppError.GroupLimitReached

        val displayName = userDoc?.getString("displayName") ?: auth.currentUser?.displayName ?: ""
        
        val groupId = UUID.randomUUID().toString()
        val doc = groupDataSource.createGroup(
            groupId = groupId,
            name = name,
            betMode = betMode.name,
            inviteCode = generateSimpleCode(),
            adminUserId = uid,
            adminDisplayName = displayName,
            scoringSystem = scoringSystem.toMap()
        )
        doc.toGroup()
    }

    override suspend fun joinGroup(inviteCode: String): Result<Group> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")

        val userDoc = authDataSource.getUserDocument(uid)
        val isVip = userDoc?.getBoolean("isVip") ?: false
        if (!isVip && groupDataSource.getUserGroupCount(uid) >= 1) throw AppError.GroupLimitReached

        val displayName = userDoc?.getString("displayName") ?: auth.currentUser?.displayName ?: ""

        val doc = groupDataSource.getGroupByInviteCode(inviteCode)
            ?: throw AppError.InvalidInviteCode

        val group = doc.toGroup()
        if (group.memberCount >= 50) throw AppError.GroupFull

        groupDataSource.addMember(doc.id, uid, displayName)
        group.copy(memberCount = group.memberCount + 1)
    }

    override fun observeUserGroups(): Flow<List<Group>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return groupDataSource.observeUserGroups(uid).map { docs -> docs.map { it.toGroup() } }
    }

    override fun observeGroup(groupId: String): Flow<Group> =
        groupDataSource.observeGroup(groupId).map { it!!.toGroup() }

    override suspend fun updateScoringSystem(groupId: String, scoringSystem: ScoringSystem): Result<Unit> =
        runCatching {
            groupDataSource.updateScoringSystem(groupId, scoringSystem.toMap())
        }

    override suspend fun updateMemberDisplayNameInAllGroups(userId: String, newName: String): Result<Unit> = runCatching {
        groupDataSource.updateMemberDisplayNameInAllGroups(userId, newName)
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val groupSnap = groupDataSource.observeGroup(groupId).first()
        if (groupSnap?.getString("adminUserId") != uid) error("Apenas o administrador pode excluir o grupo")
        groupDataSource.deleteGroup(groupId)
    }

    private fun generateSimpleCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
}
