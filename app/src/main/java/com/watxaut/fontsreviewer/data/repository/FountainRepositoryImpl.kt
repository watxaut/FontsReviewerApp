package com.watxaut.fontsreviewer.data.repository

import android.content.Context
import com.watxaut.fontsreviewer.data.local.dao.FountainDao
import com.watxaut.fontsreviewer.data.mapper.toDomain
import com.watxaut.fontsreviewer.data.remote.service.SupabaseService
import com.watxaut.fontsreviewer.domain.model.Fountain
import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import com.watxaut.fontsreviewer.util.CsvParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FountainRepositoryImpl @Inject constructor(
    private val fountainDao: FountainDao,
    private val supabaseService: SupabaseService,
    @ApplicationContext private val context: Context
) : FountainRepository {

    override fun getAllFountains(): Flow<List<Fountain>> {
        return fountainDao.getAllFountains()
            .map { entities -> 
                val fountains = entities.map { it.toDomain() }
                
                // Fetch fountain statistics from Supabase and merge
                try {
                    val statsResult = supabaseService.getAllFountainStats()
                    if (statsResult.isSuccess) {
                        val statsMap = statsResult.getOrNull()
                            ?.associate { it.fountainId to (it.averageRating to it.totalReviews) }
                            ?: emptyMap()
                        
                        // Merge stats with fountains
                        fountains.map { fountain ->
                            val (avgRating, totalReviews) = statsMap[fountain.codi] ?: (0.0 to 0)
                            fountain.copy(
                                averageRating = avgRating,
                                totalReviews = totalReviews
                            )
                        }
                    } else {
                        // If stats fetch fails, return fountains without stats
                        fountains
                    }
                } catch (e: Exception) {
                    // If error, return fountains without stats
                    android.util.Log.w("FountainRepository", "Failed to fetch fountain stats: ${e.message}")
                    fountains
                }
            }
    }

    override suspend fun getFountainByCodi(codi: String): Fountain? {
        return fountainDao.getFountainByCodi(codi)?.toDomain()
    }

    override suspend fun initializeFountains() {
        val count = fountainDao.getFountainCount()
        if (count == 0) {
            val fountains = CsvParser.parseFountainsFromAssets(context)
            fountainDao.insertAll(fountains)
        }
    }

    override suspend fun getFountainCount(): Int {
        return fountainDao.getFountainCount()
    }
}
