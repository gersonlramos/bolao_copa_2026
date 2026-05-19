package com.bolao.copa2026.feature.matches

import app.cash.turbine.test
import com.bolao.copa2026.domain.model.Match
import com.bolao.copa2026.domain.model.MatchStatus
import com.bolao.copa2026.domain.repository.MatchRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MatchListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var matchRepository: MatchRepository

    private fun makeMatch(status: MatchStatus, scoreHome: Int? = null, scoreAway: Int? = null) = Match(
        id = "m1", groupStage = "A",
        homeTeam = "Brasil", awayTeam = "Argentina",
        scheduledAt = Instant.now(), status = status,
        scoreHome = scoreHome, scoreAway = scoreAway
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        matchRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() {
        every { matchRepository.observeMatches() } returns flowOf(emptyList())
        val vm = MatchListViewModel(matchRepository)
        // After flowOf emits, loading becomes false
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `SCHEDULED status is reflected in match list`() = runTest {
        val match = makeMatch(MatchStatus.SCHEDULED)
        every { matchRepository.observeMatches() } returns flowOf(listOf(match))
        val vm = MatchListViewModel(matchRepository)
        assertEquals(MatchStatus.SCHEDULED, vm.uiState.value.matches.first().status)
    }

    @Test
    fun `status transitions SCHEDULED to IN_PROGRESS to FINISHED`() = runTest {
        val m1 = makeMatch(MatchStatus.SCHEDULED)
        val m2 = makeMatch(MatchStatus.IN_PROGRESS)
        val m3 = makeMatch(MatchStatus.FINISHED, 2, 1)

        every { matchRepository.observeMatches() } returnsMany listOf(
            flowOf(listOf(m1)),
            flowOf(listOf(m2)),
            flowOf(listOf(m3))
        )

        // Verify each state independently
        val vm1 = MatchListViewModel(matchRepository)
        assertEquals(MatchStatus.SCHEDULED, vm1.uiState.value.matches.first().status)

        val vm2 = MatchListViewModel(matchRepository)
        assertEquals(MatchStatus.IN_PROGRESS, vm2.uiState.value.matches.first().status)

        val vm3 = MatchListViewModel(matchRepository)
        assertEquals(MatchStatus.FINISHED, vm3.uiState.value.matches.first().status)
        assertEquals(2, vm3.uiState.value.matches.first().scoreHome)
        assertEquals(1, vm3.uiState.value.matches.first().scoreAway)
    }

    @Test
    fun `empty matches list is handled`() {
        every { matchRepository.observeMatches() } returns flowOf(emptyList())
        val vm = MatchListViewModel(matchRepository)
        assertTrue(vm.uiState.value.matches.isEmpty())
    }

    @Test
    fun `error state is set on repository exception`() {
        every { matchRepository.observeMatches() } returns kotlinx.coroutines.flow.flow { throw RuntimeException("network error") }
        val vm = MatchListViewModel(matchRepository)
        assertEquals("network error", vm.uiState.value.error)
    }
}
