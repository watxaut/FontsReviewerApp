package com.watxaut.fontsreviewer.presentation.leaderboard

import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import com.watxaut.fontsreviewer.domain.usecase.GetLeaderboardUseCase
import com.watxaut.fontsreviewer.util.MainDispatcherRule
import com.watxaut.fontsreviewer.util.TestData
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getLeaderboardUseCase: GetLeaderboardUseCase
    private lateinit var authRepository: AuthRepository
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var viewModel: LeaderboardViewModel

    @Before
    fun setup() {
        getLeaderboardUseCase = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        supabaseClient = mockk(relaxed = true)
        
        // Mock getCurrentUser to return null by default
        coEvery { authRepository.getCurrentUser() } returns null
        coEvery { getLeaderboardUseCase() } returns Result.success(TestData.testLeaderboardEntries)
        
        viewModel = LeaderboardViewModel(getLeaderboardUseCase, authRepository, supabaseClient)
    }

    @Test
    fun `init loads leaderboard successfully`() = runTest {
        // When - ViewModel is initialized (happens in setup)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is LeaderboardUiState.Success)
        val successState = state as LeaderboardUiState.Success
        assertEquals(3, successState.entries.size)
        assertNull(successState.currentUserNickname)
        coVerify(atLeast = 1) { getLeaderboardUseCase() }
    }

    @Test
    fun `init loads leaderboard with current user nickname`() = runTest {
        // Given
        val testUser = TestData.testUser
        coEvery { authRepository.getCurrentUser() } returns testUser
        coEvery { getLeaderboardUseCase() } returns Result.success(TestData.testLeaderboardEntries)
        
        // When
        val viewModel = LeaderboardViewModel(getLeaderboardUseCase, authRepository, supabaseClient)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is LeaderboardUiState.Success)
        val successState = state as LeaderboardUiState.Success
        assertEquals(testUser.nickname, successState.currentUserNickname)
    }

    @Test
    fun `init handles empty leaderboard`() = runTest {
        // Given
        coEvery { getLeaderboardUseCase() } returns Result.success(emptyList())
        
        // When
        val viewModel = LeaderboardViewModel(getLeaderboardUseCase, authRepository, supabaseClient)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is LeaderboardUiState.Empty)
    }

    @Test
    fun `init handles error`() = runTest {
        // Given
        coEvery { getLeaderboardUseCase() } returns Result.failure(Exception("Failed to load"))
        
        // When
        val viewModel = LeaderboardViewModel(getLeaderboardUseCase, authRepository, supabaseClient)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is LeaderboardUiState.Error)
        assertEquals("Failed to load", (state as LeaderboardUiState.Error).message)
    }

    @Test
    fun `onRefresh reloads leaderboard`() = runTest {
        // Given
        val newEntries = TestData.testLeaderboardEntries.take(2)
        coEvery { getLeaderboardUseCase() } returns Result.success(newEntries)
        
        // When
        viewModel.onRefresh()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is LeaderboardUiState.Success)
        val successState = state as LeaderboardUiState.Success
        assertEquals(2, successState.entries.size)
    }

    @Test
    fun `leaderboard entries are in correct order`() = runTest {
        // Given/When - Already initialized with test data
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is LeaderboardUiState.Success)
        val entries = (state as LeaderboardUiState.Success).entries
        
        // Verify ranks are in ascending order
        assertEquals(1, entries[0].rank)
        assertEquals(2, entries[1].rank)
        assertEquals(3, entries[2].rank)
    }
}
