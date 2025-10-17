package com.watxaut.fontsreviewer.presentation.auth.register

import com.watxaut.fontsreviewer.domain.usecase.RegisterUseCase
import com.watxaut.fontsreviewer.util.InputValidator
import com.watxaut.fontsreviewer.util.MainDispatcherRule
import com.watxaut.fontsreviewer.util.TestData
import com.watxaut.fontsreviewer.util.ValidationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var registerUseCase: RegisterUseCase
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setup() {
        registerUseCase = mockk()
        
        // Mock InputValidator to avoid Android framework dependencies
        mockkObject(InputValidator)
        every { InputValidator.validateEmail(any()) } returns ValidationResult.Valid
        every { InputValidator.validateNickname(any()) } returns ValidationResult.Valid
        
        viewModel = RegisterViewModel(registerUseCase)
    }
    
    @After
    fun tearDown() {
        unmockkObject(InputValidator)
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        
        assertEquals("", state.email)
        assertEquals("", state.nickname)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertFalse(state.isLoading)
        assertNull(state.emailError)
        assertNull(state.nicknameError)
        assertNull(state.passwordError)
        assertNull(state.confirmPasswordError)
        assertNull(state.errorMessage)
        assertFalse(state.registerSuccess)
        assertFalse(state.navigateBack)
    }

    @Test
    fun `onEmailChange updates email and clears error`() {
        viewModel.onEmailChange("test@example.com")
        
        val state = viewModel.uiState.value
        assertEquals("test@example.com", state.email)
        assertNull(state.emailError)
    }

    @Test
    fun `onNicknameChange updates nickname and clears error`() {
        viewModel.onNicknameChange("TestUser")
        
        val state = viewModel.uiState.value
        assertEquals("TestUser", state.nickname)
        assertNull(state.nicknameError)
    }

    @Test
    fun `onPasswordChange updates password and clears error`() {
        viewModel.onPasswordChange("password123")
        
        val state = viewModel.uiState.value
        assertEquals("password123", state.password)
        assertNull(state.passwordError)
    }

    @Test
    fun `onConfirmPasswordChange updates confirmPassword and clears error`() {
        viewModel.onConfirmPasswordChange("password123")
        
        val state = viewModel.uiState.value
        assertEquals("password123", state.confirmPassword)
        assertNull(state.confirmPasswordError)
    }

    @Test
    fun `onRegisterClick with blank email shows error`() {
        // Mock InputValidator to return error for blank email
        every { InputValidator.validateEmail("") } returns ValidationResult.Error("Email is required")
        
        viewModel.onEmailChange("")
        viewModel.onNicknameChange("TestUser")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.onRegisterClick()
        
        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `onRegisterClick with blank nickname shows error`() {
        // Mock InputValidator to return error for blank nickname
        every { InputValidator.validateNickname("") } returns ValidationResult.Error("Nickname is required")
        
        viewModel.onEmailChange("test@example.com")
        viewModel.onNicknameChange("")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.onRegisterClick()
        
        val state = viewModel.uiState.value
        assertNotNull(state.nicknameError)
    }

    @Test
    fun `onRegisterClick with blank password shows error`() {
        viewModel.onEmailChange("test@example.com")
        viewModel.onNicknameChange("TestUser")
        viewModel.onPasswordChange("")
        viewModel.onConfirmPasswordChange("")
        viewModel.onRegisterClick()
        
        val state = viewModel.uiState.value
        assertEquals("Password is required", state.passwordError)
    }

    @Test
    fun `onRegisterClick with short password shows error`() {
        viewModel.onEmailChange("test@example.com")
        viewModel.onNicknameChange("TestUser")
        viewModel.onPasswordChange("12345")
        viewModel.onConfirmPasswordChange("12345")
        viewModel.onRegisterClick()
        
        val state = viewModel.uiState.value
        assertEquals("Password must be at least 6 characters", state.passwordError)
    }

    @Test
    fun `onRegisterClick with mismatched passwords shows error`() {
        viewModel.onEmailChange("test@example.com")
        viewModel.onNicknameChange("TestUser")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password456")
        viewModel.onRegisterClick()
        
        val state = viewModel.uiState.value
        assertEquals("Passwords do not match", state.confirmPasswordError)
    }

    @Test
    fun `onRegisterClick with valid data succeeds`() = runTest {
        // Given
        val email = "test@example.com"
        val nickname = "TestUser"
        val password = "password123"
        
        // Ensure InputValidator returns valid for all inputs
        every { InputValidator.validateEmail(email) } returns ValidationResult.Valid
        every { InputValidator.validateNickname(nickname) } returns ValidationResult.Valid
        
        coEvery { registerUseCase(email, nickname, password) } returns Result.success(TestData.testUser)

        // When
        viewModel.onEmailChange(email)
        viewModel.onNicknameChange(nickname)
        viewModel.onPasswordChange(password)
        viewModel.onConfirmPasswordChange(password)
        viewModel.onRegisterClick()
        advanceUntilIdle()  // Wait for coroutines to complete

        // Then
        val state = viewModel.uiState.value
        assertTrue("Expected registerSuccess to be true, but was false. Error: ${state.errorMessage}", state.registerSuccess)
        assertFalse("Expected isLoading to be false, but was true", state.isLoading)
        assertNull("Expected errorMessage to be null, but was: ${state.errorMessage}", state.errorMessage)
        coVerify(exactly = 1) { registerUseCase(email, nickname, password) }
    }

    @Test
    fun `onRegisterClick with duplicate email shows error`() = runTest {
        // Given
        val email = "existing@example.com"
        val nickname = "TestUser"
        val password = "password123"
        
        // Ensure InputValidator returns valid
        every { InputValidator.validateEmail(email) } returns ValidationResult.Valid
        every { InputValidator.validateNickname(nickname) } returns ValidationResult.Valid
        
        coEvery { 
            registerUseCase(email, nickname, password) 
        } returns Result.failure(Exception("email already exists"))

        // When
        viewModel.onEmailChange(email)
        viewModel.onNicknameChange(nickname)
        viewModel.onPasswordChange(password)
        viewModel.onConfirmPasswordChange(password)
        viewModel.onRegisterClick()

        // Then
        val state = viewModel.uiState.value
        assertEquals("Email already exists", state.errorMessage)
        assertFalse(state.registerSuccess)
    }

    @Test
    fun `onRegisterClick handles network error`() = runTest {
        // Given
        val email = "test@example.com"
        val nickname = "TestUser"
        val password = "password123"
        
        // Ensure InputValidator returns valid
        every { InputValidator.validateEmail(email) } returns ValidationResult.Valid
        every { InputValidator.validateNickname(nickname) } returns ValidationResult.Valid
        
        coEvery { 
            registerUseCase(email, nickname, password) 
        } returns Result.failure(Exception("network error"))

        // When
        viewModel.onEmailChange(email)
        viewModel.onNicknameChange(nickname)
        viewModel.onPasswordChange(password)
        viewModel.onConfirmPasswordChange(password)
        viewModel.onRegisterClick()

        // Then
        assertEquals("Network error. Please check your connection", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onBackClick sets navigation flag`() {
        viewModel.onBackClick()
        
        assertTrue(viewModel.uiState.value.navigateBack)
    }

    @Test
    fun `onNavigatedBack clears navigation flag`() {
        viewModel.onBackClick()
        viewModel.onNavigatedBack()
        
        assertFalse(viewModel.uiState.value.navigateBack)
    }

    @Test
    fun `onRegisterSuccess clears success flag`() = runTest {
        // Given
        val email = "test@example.com"
        val nickname = "TestUser"
        val password = "password123"
        
        // Ensure InputValidator returns valid
        every { InputValidator.validateEmail(email) } returns ValidationResult.Valid
        every { InputValidator.validateNickname(nickname) } returns ValidationResult.Valid
        
        coEvery { registerUseCase(email, nickname, password) } returns Result.success(TestData.testUser)

        // When
        viewModel.onEmailChange(email)
        viewModel.onNicknameChange(nickname)
        viewModel.onPasswordChange(password)
        viewModel.onConfirmPasswordChange(password)
        viewModel.onRegisterClick()
        assertTrue(viewModel.uiState.value.registerSuccess)
        
        viewModel.onRegisterSuccess()

        // Then
        assertFalse(viewModel.uiState.value.registerSuccess)
    }
}
