package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CreateFountainUseCaseTest {

    private lateinit var fountainRepository: FountainRepository
    private lateinit var createFountainUseCase: CreateFountainUseCase

    @Before
    fun setup() {
        fountainRepository = mockk()
        createFountainUseCase = CreateFountainUseCase(fountainRepository)
    }

    @Test
    fun `invoke with valid data returns success with fountain id`() = runTest {
        // Given
        val nom = "New Fountain"
        val carrer = "Test Street"
        val numeroCarrer = "123"
        val latitude = 41.3851
        val longitude = 2.1734
        val expectedId = "new-fountain-id"
        
        coEvery { 
            fountainRepository.createFountain(nom, carrer, numeroCarrer, latitude, longitude)
        } returns Result.success(expectedId)

        // When
        val result = createFountainUseCase(nom, carrer, numeroCarrer, latitude, longitude)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedId, result.getOrNull())
        coVerify(exactly = 1) { 
            fountainRepository.createFountain(nom, carrer, numeroCarrer, latitude, longitude)
        }
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        // Given
        val nom = "New Fountain"
        val carrer = "Test Street"
        val numeroCarrer = "123"
        val latitude = 41.3851
        val longitude = 2.1734
        val exception = Exception("Failed to create fountain")
        
        coEvery { 
            fountainRepository.createFountain(nom, carrer, numeroCarrer, latitude, longitude)
        } returns Result.failure(exception)

        // When
        val result = createFountainUseCase(nom, carrer, numeroCarrer, latitude, longitude)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Failed to create fountain", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke calls repository with correct parameters`() = runTest {
        // Given
        val nom = "Specific Fountain"
        val carrer = "Specific Street"
        val numeroCarrer = "456"
        val latitude = 41.4
        val longitude = 2.2
        
        coEvery { 
            fountainRepository.createFountain(nom, carrer, numeroCarrer, latitude, longitude)
        } returns Result.success("id")

        // When
        createFountainUseCase(nom, carrer, numeroCarrer, latitude, longitude)

        // Then
        coVerify(exactly = 1) { 
            fountainRepository.createFountain(nom, carrer, numeroCarrer, latitude, longitude)
        }
    }
}
