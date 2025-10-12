package com.watxaut.fontsreviewer.data.repository

import android.content.Context
import com.watxaut.fontsreviewer.util.SecureLog
import com.watxaut.fontsreviewer.data.remote.service.SupabaseService
import com.watxaut.fontsreviewer.domain.model.Fountain
import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import com.watxaut.fontsreviewer.util.NetworkUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FountainRepositoryImpl @Inject constructor(
    private val supabaseService: SupabaseService,
    @ApplicationContext private val context: Context
) : FountainRepository {

    companion object {
        private const val TAG = "FountainRepository"
    }

    override fun getAllFountains(): Flow<List<Fountain>> = flow {
        SecureLog.i(TAG, "Fetching all fountains from Supabase...")
        
        // Check internet connectivity first
        if (!NetworkUtil.isNetworkAvailable(context)) {
            SecureLog.e(TAG, "No internet connection available")
            throw NoInternetException("No internet connection. Please check your network settings.")
        }
        
        try {
            // Fetch fountains with stats from Supabase
            val result = supabaseService.getAllFountainsWithStats()
            
            if (result.isSuccess) {
                val fountainsDto = result.getOrNull() ?: emptyList()
                val fountains = fountainsDto.map { dto ->
                    Fountain(
                        codi = dto.codi,
                        nom = dto.nom,
                        carrer = dto.carrer,
                        numeroCarrer = dto.numeroCarrer,
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        averageRating = dto.averageRating,
                        totalReviews = dto.totalReviews
                    )
                }
                
                SecureLog.i(TAG, "Fetched ${fountains.size} fountains from Supabase")
                SecureLog.i(TAG, "Fountains with reviews: ${fountains.count { it.totalReviews > 0 }}")
                
                emit(fountains)
            } else {
                val error = result.exceptionOrNull()
                SecureLog.e(TAG, "Failed to fetch fountains from Supabase: ${error?.message}")
                
                // Check if it's a network issue
                if (!NetworkUtil.isNetworkAvailable(context)) {
                    throw NoInternetException("Lost internet connection while loading fountains.")
                } else {
                    throw Exception("Failed to load fountains: ${error?.message ?: "Unknown error"}")
                }
            }
        } catch (e: NoInternetException) {
            throw e
        } catch (e: Exception) {
            SecureLog.e(TAG, "Error fetching fountains: ${e.message}", e)
            
            // Check if it's a network issue
            if (!NetworkUtil.isNetworkAvailable(context)) {
                throw NoInternetException("No internet connection. Please check your network settings.")
            } else {
                throw Exception("Failed to load fountains: ${e.message ?: "Unknown error"}")
            }
        }
    }

    override suspend fun getFountainByCodi(codi: String): Fountain? {
        return try {
            // Check internet connectivity
            if (!NetworkUtil.isNetworkAvailable(context)) {
                SecureLog.w(TAG, "No internet connection for fetching fountain $codi")
                return null
            }
            
            val result = supabaseService.getFountainByCodi(codi)
            if (result.isSuccess) {
                val dto = result.getOrNull()
                dto?.let {
                    Fountain(
                        codi = it.codi,
                        nom = it.nom,
                        carrer = it.carrer,
                        numeroCarrer = it.numeroCarrer,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        averageRating = it.averageRating,
                        totalReviews = it.totalReviews
                    )
                }
            } else {
                SecureLog.e(TAG, "Failed to fetch fountain $codi: ${result.exceptionOrNull()?.message}")
                null
            }
        } catch (e: Exception) {
            SecureLog.e(TAG, "Error fetching fountain $codi: ${e.message}")
            null
        }
    }

    override suspend fun initializeFountains() {
        // No longer needed - fountains are in Supabase
        SecureLog.i(TAG, "initializeFountains called but no longer needed (fountains are in Supabase)")
    }

    override suspend fun getFountainCount(): Int {
        return try {
            val result = supabaseService.getAllFountainsWithStats()
            result.getOrNull()?.size ?: 0
        } catch (e: Exception) {
            SecureLog.e(TAG, "Error getting fountain count: ${e.message}")
            0
        }
    }
}

/**
 * Exception thrown when there's no internet connection
 */
class NoInternetException(message: String) : Exception(message)
