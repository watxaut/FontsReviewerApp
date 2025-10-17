package com.watxaut.fontsreviewer.presentation.map

import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import com.watxaut.fontsreviewer.domain.usecase.GetFountainsUseCase
import com.watxaut.fontsreviewer.domain.usecase.GetUserReviewedFountainsUseCase
import com.watxaut.fontsreviewer.util.MainDispatcherRule
import com.watxaut.fontsreviewer.util.TestData
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getFountainsUseCase: GetFountainsUseCase
    private lateinit var getUserReviewedFountainsUseCase: GetUserReviewedFountainsUseCase
    private lateinit var authRepository: AuthRepository
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var viewModel: MapViewModel

    @Before
    fun setup() {
        getFountainsUseCase = mockk(relaxed = true)
        getUserReviewedFountainsUseCase = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        supabaseClient = mockk(relaxed = true)
        
        // Mock the auth extension property to return a mock Auth object
        val authMock = mockk<io.github.jan.supabase.auth.Auth>(relaxed = true)
        val emptyFlow = MutableStateFlow<io.github.jan.supabase.auth.status.SessionStatus>(
            mockk(relaxed = true)
        )
        every { authMock.sessionStatus } returns emptyFlow
        
        // Mock the auth extension function
        mockkStatic("io.github.jan.supabase.auth.AuthKt")
        every { supabaseClient.auth } returns authMock
        
        // Mock default return values
        every { getFountainsUseCase(any()) } returns flowOf(
            listOf(TestData.testFountain, TestData.testFountain2)
        )
        coEvery { authRepository.getCurrentUser() } returns null
        coEvery { getUserReviewedFountainsUseCase(any()) } returns Result.success(emptySet())
    }

    @After
    fun tearDown() {
        // Clean up static mocks
        io.mockk.unmockkStatic("io.github.jan.supabase.auth.AuthKt")
    }

    @Test
    fun `init loads fountains successfully`() = runTest {
        // When - ViewModel is initialized
        viewModel = MapViewModel(
            getFountainsUseCase,
            authRepository,
            getUserReviewedFountainsUseCase,
            supabaseClient
        )
        
        // Allow flows to collect
        advanceUntilIdle()
        
        // Then
        verify(atLeast = 1) { getFountainsUseCase(false) }
    }

    @Test
    fun `loads fountains and updates UI state`() = runTest {
        // Given
        val fountains = listOf(TestData.testFountain, TestData.testFountain2)
        every { getFountainsUseCase(false) } returns flowOf(fountains)
        
        // When
        viewModel = MapViewModel(
            getFountainsUseCase,
            authRepository,
            getUserReviewedFountainsUseCase,
            supabaseClient
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is MapUiState.Success)
        assertEquals(2, (state as MapUiState.Success).fountains.size)
    }

    @Test
    fun `refresh updates fountain data`() = runTest {
        // Given
        viewModel = MapViewModel(
            getFountainsUseCase,
            authRepository,
            getUserReviewedFountainsUseCase,
            supabaseClient
        )
        advanceUntilIdle()
        
        // When
        viewModel.refresh()
        advanceUntilIdle()
        
        // Then - Should have called getFountainsUseCase again
        verify(atLeast = 2) { getFountainsUseCase(false) }
    }
}
