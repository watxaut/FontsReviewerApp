package com.watxaut.fontsreviewer.presentation.auth.login

import com.watxaut.fontsreviewer.domain.usecase.LoginUseCase
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
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var loginUseCase: LoginUseCase
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        loginUseCase = mockk()
        viewModel = LoginViewModel(loginUseCase)
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.errorMessage)
        assertFalse(state.loginSuccess)
        assertFalse(state.navigateToRegister)
    }

    @Test
    fun `onEmailChange updates email and clears error`() {
        viewModel.onEmailChange("test@example.com")
        
        val state = viewModel.uiState.value
        assertEquals("test@example.com", state.email)
        assertNull(state.emailError)
    }

    @Test
    fun `onPasswordChange updates password and clears error`() {
        viewModel.onPasswordChange("password123")
        
        val state = viewModel.uiState.value
        assertEquals("password123", state.password)
        assertNull(state.passwordError)
    }

    @Test
    fun `onLoginClick with blank email shows error`() {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("password123")
        viewModel.onLoginClick()
        
        val state = viewModel.uiState.value
        assertEquals("Email is required", state.emailError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `onLoginClick with blank password shows error`() {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("")
        viewModel.onLoginClick()
        
        val state = viewModel.uiState.value
        assertEquals("Password is required", state.passwordError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `onLoginClick with both blank shows both errors`() {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("")
        viewModel.onLoginClick()
        
        val state = viewModel.uiState.value
        assertEquals("Email is required", state.emailError)
        assertEquals("Password is required", state.passwordError)
    }

    @Test
    fun `onLoginClick with valid credentials succeeds`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        coEvery { loginUseCase(email, password) } returns Result.success(TestData.testUser)

        // When
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onLoginClick()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.loginSuccess)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        coVerify(exactly = 1) { loginUseCase(email, password) }
    }

    @Test
    fun `onLoginClick with invalid credentials shows error`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "wrongpassword"
        coEvery { loginUseCase(email, password) } returns Result.failure(
            Exception("invalid_credentials")
        )

        // When
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onLoginClick()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.loginSuccess)
        assertFalse(state.isLoading)
        assertEquals("Incorrect email or password", state.errorMessage)
    }

    @Test
    fun `onLoginClick handles network error`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        coEvery { loginUseCase(email, password) } returns Result.failure(
            Exception("network error")
        )

        // When
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onLoginClick()

        // Then
        val state = viewModel.uiState.value
        assertEquals("Network error. Please check your connection", state.errorMessage)
    }

    @Test
    fun `onLoginClick handles timeout error`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        coEvery { loginUseCase(email, password) } returns Result.failure(
            Exception("timeout")
        )

        // When
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onLoginClick()

        // Then
        val state = viewModel.uiState.value
        assertEquals("Connection timeout. Please try again", state.errorMessage)
    }

    @Test
    fun `onRegisterClick sets navigation flag`() {
        viewModel.onRegisterClick()
        
        val state = viewModel.uiState.value
        assertTrue(state.navigateToRegister)
    }

    @Test
    fun `onNavigatedToRegister clears navigation flag`() {
        viewModel.onRegisterClick()
        viewModel.onNavigatedToRegister()
        
        val state = viewModel.uiState.value
        assertFalse(state.navigateToRegister)
    }

    @Test
    fun `onLoginSuccess clears success flag`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        coEvery { loginUseCase(email, password) } returns Result.success(TestData.testUser)

        // When
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onLoginClick()
        assertTrue(viewModel.uiState.value.loginSuccess)
        
        viewModel.onLoginSuccess()

        // Then
        assertFalse(viewModel.uiState.value.loginSuccess)
    }
}
