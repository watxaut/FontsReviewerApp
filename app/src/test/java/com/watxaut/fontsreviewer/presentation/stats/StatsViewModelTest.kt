package com.watxaut.fontsreviewer.presentation.stats

import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import com.watxaut.fontsreviewer.domain.usecase.GetUserStatsUseCase
import com.watxaut.fontsreviewer.util.MainDispatcherRule
import com.watxaut.fontsreviewer.util.TestData
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getUserStatsUseCase: GetUserStatsUseCase
    private lateinit var authRepository: AuthRepository
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var viewModel: StatsViewModel

    @Before
    fun setup() {
        getUserStatsUseCase = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        supabaseClient = mockk(relaxed = true)
        
        // Mock getCurrentUser to return test user by default
        coEvery { authRepository.getCurrentUser() } returns TestData.testUser
        coEvery { getUserStatsUseCase(any()) } returns Result.success(TestData.testUserStats)
        
        viewModel = StatsViewModel(getUserStatsUseCase, authRepository, supabaseClient)
    }

    @Test
    fun `init loads stats successfully for logged in user`() = runTest {
        // When - ViewModel is initialized (happens in setup)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is StatsUiState.Success)
        val successState = state as StatsUiState.Success
        assertEquals(TestData.testUser.nickname, successState.nickname)
        assertEquals(TestData.testUserStats, successState.stats)
        coVerify(atLeast = 1) { getUserStatsUseCase(TestData.testUser.id) }
    }

    @Test
    fun `init shows not logged in state when user is null`() = runTest {
        // Given
        coEvery { authRepository.getCurrentUser() } returns null
        
        // When
        val viewModel = StatsViewModel(getUserStatsUseCase, authRepository, supabaseClient)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is StatsUiState.NotLoggedIn)
    }

    @Test
    fun `init handles error loading stats`() = runTest {
        // Given
        coEvery { getUserStatsUseCase(any()) } returns Result.failure(Exception("Failed to load stats"))
        
        // When
        val viewModel = StatsViewModel(getUserStatsUseCase, authRepository, supabaseClient)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is StatsUiState.Error)
        assertEquals("Failed to load stats", (state as StatsUiState.Error).message)
    }

    @Test
    fun `onRefresh reloads stats`() = runTest {
        // Given
        val newStats = TestData.testUserStats.copy(totalRatings = 20)
        coEvery { getUserStatsUseCase(any()) } returns Result.success(newStats)
        
        // When
        viewModel.onRefresh()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is StatsUiState.Success)
        val successState = state as StatsUiState.Success
        assertEquals(20, successState.stats.totalRatings)
    }

    @Test
    fun `stats include best fountain when available`() = runTest {
        // When - Already initialized with test data that includes best fountain
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is StatsUiState.Success)
        val stats = (state as StatsUiState.Success).stats
        assertNotNull(stats.bestFountain)
        assertEquals(TestData.testFountain, stats.bestFountain)
    }

    @Test
    fun `stats are correctly mapped`() = runTest {
        // When - Already initialized
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is StatsUiState.Success)
        val successState = state as StatsUiState.Success
        
        assertEquals(10, successState.stats.totalRatings)
        assertEquals(4.5, successState.stats.averageScore, 0.01)
    }
}
