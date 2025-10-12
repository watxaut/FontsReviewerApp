package com.watxaut.fontsreviewer.domain.repository

import com.watxaut.fontsreviewer.domain.model.CreateFountainRequest
import com.watxaut.fontsreviewer.domain.model.Fountain
import kotlinx.coroutines.flow.Flow

interface FountainRepository {
    fun getAllFountains(includeDeleted: Boolean = false): Flow<List<Fountain>>
    suspend fun getFountainByCodi(codi: String): Fountain?
    suspend fun createFountain(fountain: CreateFountainRequest): Result<String>
    suspend fun softDeleteFountain(fountainId: String): Result<Unit>
}
