package com.watxaut.fontsreviewer.domain.repository

import com.watxaut.fontsreviewer.domain.model.CreateFountainRequest
import com.watxaut.fontsreviewer.domain.model.Fountain
import kotlinx.coroutines.flow.Flow

interface FountainRepository {
    fun getAllFountains(includeDeleted: Boolean = false): Flow<List<Fountain>>
    suspend fun getFountainByCodi(codi: String): Fountain?
    suspend fun createFountain(
        nom: String,
        carrer: String,
        numeroCarrer: String,
        latitude: Double,
        longitude: Double
    ): Result<String>
    suspend fun softDeleteFountain(fountainId: String): Result<Unit>
}
