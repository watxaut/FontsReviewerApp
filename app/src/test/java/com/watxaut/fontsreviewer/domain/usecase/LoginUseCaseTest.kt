package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import com.watxaut.fontsreviewer.util.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LoginUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var loginUseCase: LoginUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        loginUseCase = LoginUseCase(authRepository)
    }

    @Test
    fun `invoke with valid credentials returns success with user`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val expectedUser = TestData.testUser
        coEvery { authRepository.signIn(email, password) } returns Result.success(expectedUser)

        // When
        val result = loginUseCase(email, password)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())
        coVerify(exactly = 1) { authRepository.signIn(email, password) }
    }

    @Test
    fun `invoke with invalid credentials returns failure`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "wrongpassword"
        val exception = Exception("Invalid credentials")
        coEvery { authRepository.signIn(email, password) } returns Result.failure(exception)

        // When
        val result = loginUseCase(email, password)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Invalid credentials", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { authRepository.signIn(email, password) }
    }

    @Test
    fun `invoke calls repository with correct parameters`() = runTest {
        // Given
        val email = "user@example.com"
        val password = "securePassword"
        coEvery { authRepository.signIn(email, password) } returns Result.success(TestData.testUser)

        // When
        loginUseCase(email, password)

        // Then
        coVerify(exactly = 1) { authRepository.signIn(email, password) }
    }
}
