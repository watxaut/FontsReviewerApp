package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SoftDeleteFountainUseCaseTest {

    private lateinit var fountainRepository: FountainRepository
    private lateinit var softDeleteFountainUseCase: SoftDeleteFountainUseCase

    @Before
    fun setup() {
        fountainRepository = mockk()
        softDeleteFountainUseCase = SoftDeleteFountainUseCase(fountainRepository)
    }

    @Test
    fun `invoke with valid fountainId returns success`() = runTest {
        // Given
        val fountainId = "fountain-123"
        coEvery { fountainRepository.softDeleteFountain(fountainId) } returns Result.success(Unit)

        // When
        val result = softDeleteFountainUseCase(fountainId)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { fountainRepository.softDeleteFountain(fountainId) }
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        // Given
        val fountainId = "fountain-123"
        val exception = Exception("Failed to soft delete fountain")
        coEvery { fountainRepository.softDeleteFountain(fountainId) } returns Result.failure(exception)

        // When
        val result = softDeleteFountainUseCase(fountainId)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Failed to soft delete fountain", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { fountainRepository.softDeleteFountain(fountainId) }
    }

    @Test
    fun `invoke calls repository with correct fountainId`() = runTest {
        // Given
        val fountainId = "specific-fountain-id"
        coEvery { fountainRepository.softDeleteFountain(fountainId) } returns Result.success(Unit)

        // When
        softDeleteFountainUseCase(fountainId)

        // Then
        coVerify(exactly = 1) { fountainRepository.softDeleteFountain(fountainId) }
    }
}
