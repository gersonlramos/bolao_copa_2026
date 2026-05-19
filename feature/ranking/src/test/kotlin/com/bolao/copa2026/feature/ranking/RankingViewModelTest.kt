package com.bolao.copa2026.feature.ranking

import androidx.lifecycle.SavedStateHandle
import com.bolao.copa2026.domain.model.RankingEntry
import com.bolao.copa2026.domain.model.User
import com.bolao.copa2026.domain.repository.AuthRepository
import com.bolao.copa2026.domain.usecase.GetRankingUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RankingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var getRankingUseCase: GetRankingUseCase
    private lateinit var authRepository: AuthRepository

    private fun makeEntry(userId: String, position: Int, score: Int) = RankingEntry(
        position = position, userId = userId, displayName = "User $userId", totalScore = score
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getRankingUseCase = mockk()
        authRepository = mockk()
        every { authRepository.currentUser() } returns flowOf(User("me", "Me", "me@test.com"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createVm(groupId: String = "g1"): RankingViewModel {
        val handle = SavedStateHandle(mapOf("groupId" to groupId))
        val vm = RankingViewModel(handle, getRankingUseCase, authRepository)
        backgroundScope.launch { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `ranking is sorted descending by score`() = runTest {
        val entries = listOf(
            makeEntry("a", 1, 100),
            makeEntry("b", 2, 80),
            makeEntry("c", 3, 60)
        )
        every { getRankingUseCase("g1") } returns flowOf(entries)

        val vm = createVm()
        val state = vm.uiState.first { !it.isLoading }
        assertFalse(state.isLoading)
        assertEquals(3, state.entries.size)
        assertTrue(state.entries[0].totalScore >= state.entries[1].totalScore)
        assertTrue(state.entries[1].totalScore >= state.entries[2].totalScore)
    }

    @Test
    fun `tied users share same position`() = runTest {
        val entries = listOf(
            makeEntry("a", 1, 100),
            makeEntry("b", 1, 100),
            makeEntry("c", 3, 50)
        )
        every { getRankingUseCase("g1") } returns flowOf(entries)

        val vm = createVm()
        val state = vm.uiState.first { !it.isLoading }
        assertEquals(1, state.entries[0].position)
        assertEquals(1, state.entries[1].position)
        assertEquals(3, state.entries[2].position)
    }

    @Test
    fun `current user ID is set from AuthRepository`() = runTest {
        every { getRankingUseCase("g1") } returns flowOf(emptyList())
        val vm = createVm()
        val state = vm.uiState.first { !it.isLoading }
        assertEquals("me", state.currentUserId)
    }

    @Test
    fun `error state on repository failure`() = runTest {
        every { getRankingUseCase("g1") } returns flow {
            throw RuntimeException("firestore error")
        }
        val vm = createVm()
        val state = vm.uiState.first { !it.isLoading }
        assertEquals("firestore error", state.error)
    }

    @Test
    fun `unauthenticated user has empty currentUserId`() = runTest {
        every { authRepository.currentUser() } returns flowOf(null)
        every { getRankingUseCase("g1") } returns flowOf(emptyList())
        val vm = createVm()
        val state = vm.uiState.first { !it.isLoading }
        assertEquals("", state.currentUserId)
    }
}
