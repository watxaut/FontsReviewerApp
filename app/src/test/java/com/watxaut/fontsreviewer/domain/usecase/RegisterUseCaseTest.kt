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

class RegisterUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var registerUseCase: RegisterUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        registerUseCase = RegisterUseCase(authRepository)
    }

    @Test
    fun `invoke with valid data returns success with user`() = runTest {
        // Given
        val email = "newuser@example.com"
        val nickname = "NewUser"
        val password = "password123"
        val expectedUser = TestData.testUser.copy(nickname = nickname)
        coEvery { 
            authRepository.signUp(email, nickname, password) 
        } returns Result.success(expectedUser)

        // When
        val result = registerUseCase(email, nickname, password)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())
        coVerify(exactly = 1) { authRepository.signUp(email, nickname, password) }
    }

    @Test
    fun `invoke with duplicate email returns failure`() = runTest {
        // Given
        val email = "existing@example.com"
        val nickname = "User"
        val password = "password123"
        val exception = Exception("Email already exists")
        coEvery { 
            authRepository.signUp(email, nickname, password) 
        } returns Result.failure(exception)

        // When
        val result = registerUseCase(email, nickname, password)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Email already exists", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { authRepository.signUp(email, nickname, password) }
    }

    @Test
    fun `invoke calls repository with correct parameters`() = runTest {
        // Given
        val email = "test@example.com"
        val nickname = "TestNick"
        val password = "securePass"
        coEvery { 
            authRepository.signUp(email, nickname, password) 
        } returns Result.success(TestData.testUser)

        // When
        registerUseCase(email, nickname, password)

        // Then
        coVerify(exactly = 1) { authRepository.signUp(email, nickname, password) }
    }
}
