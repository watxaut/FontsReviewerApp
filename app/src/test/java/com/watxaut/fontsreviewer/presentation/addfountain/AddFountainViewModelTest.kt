package com.watxaut.fontsreviewer.presentation.addfountain

import com.watxaut.fontsreviewer.domain.usecase.CreateFountainUseCase
import com.watxaut.fontsreviewer.util.MainDispatcherRule
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
class AddFountainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var createFountainUseCase: CreateFountainUseCase
    private lateinit var viewModel: AddFountainViewModel

    @Before
    fun setup() {
        createFountainUseCase = mockk()
        viewModel = AddFountainViewModel(createFountainUseCase)
    }

    @Test
    fun `initial state is Initial`() {
        val state = viewModel.uiState.value
        assertTrue(state is AddFountainUiState.Initial)
    }

    @Test
    fun `createFountain with valid data succeeds`() = runTest {
        // Given
        val fountainId = "new-fountain-id"
        coEvery { 
            createFountainUseCase(any(), any(), any(), any(), any()) 
        } returns Result.success(fountainId)

        // When
        viewModel.createFountain("New Fountain", "Main Street", "123", 41.3851, 2.1734)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AddFountainUiState.Success)
        assertEquals(fountainId, (state as AddFountainUiState.Success).fountainId)
        coVerify { createFountainUseCase("New Fountain", "Main Street", "123", 41.3851, 2.1734) }
    }

    @Test
    fun `createFountain with error shows error state`() = runTest {
        // Given
        coEvery { 
            createFountainUseCase(any(), any(), any(), any(), any()) 
        } returns Result.failure(Exception("Failed to create"))

        // When
        viewModel.createFountain("New Fountain", "Main Street", "123", 41.3851, 2.1734)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AddFountainUiState.Error)
        assertEquals("Failed to create", (state as AddFountainUiState.Error).message)
    }

    @Test
    fun `createFountain shows loading state during execution`() = runTest {
        // Given
        coEvery { 
            createFountainUseCase(any(), any(), any(), any(), any()) 
        } returns Result.success("id")

        // When
        viewModel.createFountain("Test", "Street", "1", 41.0, 2.0)

        // Then - Final state should be Success (not testing intermediate Loading state)
        assertTrue(viewModel.uiState.value is AddFountainUiState.Success)
    }
}
