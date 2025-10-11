package com.watxaut.fontsreviewer.domain.repository

import com.watxaut.fontsreviewer.domain.model.Fountain
import kotlinx.coroutines.flow.Flow

interface FountainRepository {
    fun getAllFountains(): Flow<List<Fountain>>
    suspend fun getFountainByCodi(codi: String): Fountain?
    suspend fun initializeFountains()
    suspend fun getFountainCount(): Int
}
