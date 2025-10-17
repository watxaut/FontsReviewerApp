package com.watxaut.fontsreviewer.data.mapper

import com.watxaut.fontsreviewer.data.remote.dto.FountainWithStatsDto
import org.junit.Assert.*
import org.junit.Test

class FountainMapperTest {

    @Test
    fun `FountainWithStatsDto toDomain maps all fields correctly`() {
        val dto = FountainWithStatsDto(
            codi = "fountain-123",
            nom = "Test Fountain",
            carrer = "Test Street",
            numeroCarrer = "456",
            latitude = 41.3851,
            longitude = 2.1734,
            totalReviews = 10,
            averageRating = 4.2,
            isDeleted = false
        )

        val domain = dto.toDomain()

        assertEquals("fountain-123", domain.codi)
        assertEquals("Test Fountain", domain.nom)
        assertEquals("Test Street", domain.carrer)
        assertEquals("456", domain.numeroCarrer)
        assertEquals(41.3851, domain.latitude, 0.0001)
        assertEquals(2.1734, domain.longitude, 0.0001)
        assertEquals(10, domain.totalReviews)
        assertEquals(4.2, domain.averageRating, 0.01)
        assertFalse(domain.isDeleted)
    }

    @Test
    fun `FountainWithStatsDto toDomain handles zero reviews`() {
        val dto = FountainWithStatsDto(
            codi = "fountain-123",
            nom = "New Fountain",
            carrer = "New Street",
            numeroCarrer = "1",
            latitude = 41.4,
            longitude = 2.2,
            totalReviews = 0,
            averageRating = 0.0,
            isDeleted = false
        )

        val domain = dto.toDomain()

        assertEquals(0, domain.totalReviews)
        assertEquals(0.0, domain.averageRating, 0.01)
    }

    @Test
    fun `FountainWithStatsDto toDomain handles deleted fountain`() {
        val dto = FountainWithStatsDto(
            codi = "fountain-123",
            nom = "Deleted Fountain",
            carrer = "Old Street",
            numeroCarrer = "99",
            latitude = 41.3,
            longitude = 2.1,
            totalReviews = 5,
            averageRating = 3.5,
            isDeleted = true
        )

        val domain = dto.toDomain()

        assertTrue(domain.isDeleted)
    }

    @Test
    fun `FountainWithStatsDto toDomain preserves coordinates precision`() {
        val dto = FountainWithStatsDto(
            codi = "fountain-123",
            nom = "Precise Fountain",
            carrer = "Precise Street",
            numeroCarrer = "1",
            latitude = 41.38507751,
            longitude = 2.17345123,
            totalReviews = 0,
            averageRating = 0.0,
            isDeleted = false
        )

        val domain = dto.toDomain()

        assertEquals(41.38507751, domain.latitude, 0.00000001)
        assertEquals(2.17345123, domain.longitude, 0.00000001)
    }

    @Test
    fun `FountainWithStatsDto toDomain handles high ratings count`() {
        val dto = FountainWithStatsDto(
            codi = "fountain-popular",
            nom = "Popular Fountain",
            carrer = "Main Street",
            numeroCarrer = "1",
            latitude = 41.4,
            longitude = 2.2,
            totalReviews = 999,
            averageRating = 4.95,
            isDeleted = false
        )

        val domain = dto.toDomain()

        assertEquals(999, domain.totalReviews)
        assertEquals(4.95, domain.averageRating, 0.01)
    }
}
