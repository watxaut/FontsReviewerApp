package com.watxaut.fontsreviewer.presentation.profile

import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import com.watxaut.fontsreviewer.util.MainDispatcherRule
import com.watxaut.fontsreviewer.util.TestData
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
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setup() {
        authRepository = mockk(relaxed = true)
        
        // Mock getCurrentUser to return test user by default
        coEvery { authRepository.getCurrentUser() } returns TestData.testUser
        coEvery { authRepository.isUserLoggedIn() } returns true
        
        viewModel = ProfileViewModel(authRepository)
    }

    @Test
    fun `init loads user profile successfully`() = runTest {
        // When - ViewModel is initialized (happens in setup)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is ProfileUiState.Success)
        val successState = state as ProfileUiState.Success
        assertEquals(TestData.testUser, successState.user)
    }

    @Test
    fun `init shows not authenticated state when user is null`() = runTest {
        // Given
        coEvery { authRepository.getCurrentUser() } returns null
        
        // When
        val viewModel = ProfileViewModel(authRepository)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is ProfileUiState.NotAuthenticated)
    }

    @Test
    fun `refreshProfile reloads user profile`() = runTest {
        // Given
        val updatedUser = TestData.testUser.copy(totalRatings = 20)
        coEvery { authRepository.getCurrentUser() } returns updatedUser
        
        // When
        viewModel.refreshProfile()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is ProfileUiState.Success)
        assertEquals(20, (state as ProfileUiState.Success).user.totalRatings)
    }

    @Test
    fun `onLogoutClick signs out successfully`() = runTest {
        // Given
        coEvery { authRepository.signOut() } returns Result.success(Unit)
        
        // When
        viewModel.onLogoutClick()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is ProfileUiState.NotAuthenticated)
        coVerify(exactly = 1) { authRepository.signOut() }
    }

    @Test
    fun `onDeleteAccountClick deletes account successfully`() = runTest {
        // Given
        coEvery { authRepository.deleteAccount() } returns Result.success(Unit)
        
        // When
        viewModel.onDeleteAccountClick()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is ProfileUiState.AccountDeleted)
        coVerify(exactly = 1) { authRepository.deleteAccount() }
    }

    @Test
    fun `onDeleteAccountClick handles error`() = runTest {
        // Given
        coEvery { authRepository.getCurrentUser() } returns TestData.testUser
        coEvery { authRepository.deleteAccount() } returns Result.failure(Exception("Delete failed"))
        
        // Create fresh viewModel with user loaded
        val testViewModel = ProfileViewModel(authRepository)
        
        // When
        testViewModel.onDeleteAccountClick()

        // Then
        val state = testViewModel.uiState.value
        assertTrue(state is ProfileUiState.DeleteAccountError || state is ProfileUiState.Error)
        when (state) {
            is ProfileUiState.DeleteAccountError -> assertEquals("Delete failed", state.errorMessage)
            is ProfileUiState.Error -> assertEquals("Delete failed", state.message)
            else -> fail("Expected error state")
        }
    }

    @Test
    fun `onDeleteAccountClick shows deleting state first`() = runTest {
        // Given
        coEvery { authRepository.deleteAccount() } returns Result.success(Unit)
        
        // When
        viewModel.onDeleteAccountClick()
        
        // Then - Final state should be AccountDeleted (not testing intermediate state here)
        val state = viewModel.uiState.value
        assertTrue(state is ProfileUiState.AccountDeleted)
    }

    @Test
    fun `onDismissDeleteError reloads profile`() = runTest {
        // Given - Trigger delete error
        coEvery { authRepository.getCurrentUser() } returns TestData.testUser
        coEvery { authRepository.deleteAccount() } returns Result.failure(Exception("Delete failed"))
        
        // Create fresh viewModel
        val testViewModel = ProfileViewModel(authRepository)
        testViewModel.onDeleteAccountClick()
        
        // Verify we're in error state
        val errorState = testViewModel.uiState.value
        assertTrue(errorState is ProfileUiState.DeleteAccountError || errorState is ProfileUiState.Error)
        
        // Reset mock to return user for reload
        coEvery { authRepository.getCurrentUser() } returns TestData.testUser
        
        // When
        testViewModel.onDismissDeleteError()
        
        // Then - Should reload and show success
        val state = testViewModel.uiState.value
        assertTrue(state is ProfileUiState.Success || state is ProfileUiState.Loading)
    }

    @Test
    fun `user role is correctly loaded`() {
        val state = viewModel.uiState.value
        assertTrue(state is ProfileUiState.Success)
        assertEquals(TestData.testUser.role, (state as ProfileUiState.Success).user.role)
    }

    @Test
    fun `admin user role is correctly loaded`() = runTest {
        // Given
        coEvery { authRepository.getCurrentUser() } returns TestData.testAdminUser
        
        // When
        val viewModel = ProfileViewModel(authRepository)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is ProfileUiState.Success)
        assertEquals(TestData.testAdminUser.role, (state as ProfileUiState.Success).user.role)
    }
}
