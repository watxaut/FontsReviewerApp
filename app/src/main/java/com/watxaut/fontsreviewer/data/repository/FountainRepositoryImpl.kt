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

    override fun getAllFountains(includeDeleted: Boolean): Flow<List<Fountain>> = flow {
        SecureLog.i(TAG, "Fetching all fountains from Supabase (includeDeleted=$includeDeleted)...")
        
        // Check internet connectivity first
        if (!NetworkUtil.isNetworkAvailable(context)) {
            SecureLog.e(TAG, "No internet connection available")
            throw NoInternetException("No internet connection. Please check your network settings.")
        }
        
        try {
            // Fetch fountains with stats from Supabase
            val result = if (includeDeleted) {
                supabaseService.getAllFountainsIncludingDeleted()
            } else {
                supabaseService.getAllFountainsWithStats()
            }
            
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
                        totalReviews = dto.totalReviews,
                        isDeleted = dto.isDeleted
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
                        totalReviews = it.totalReviews,
                        isDeleted = it.isDeleted
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

    
    override suspend fun createFountain(
        nom: String,
        carrer: String,
        numeroCarrer: String,
        latitude: Double,
        longitude: Double
    ): Result<String> {
        return try {
            // Check internet connectivity
            if (!NetworkUtil.isNetworkAvailable(context)) {
                SecureLog.e(TAG, "No internet connection for creating fountain")
                return Result.failure(NoInternetException("No internet connection. Please check your network settings."))
            }
            
            // Convert to DTO
            val fountainDto = com.watxaut.fontsreviewer.data.remote.dto.CreateFountainDto(
                nom = nom,
                carrer = carrer,
                numeroCarrer = numeroCarrer,
                latitude = latitude,
                longitude = longitude
            )
            
            // Call Supabase service
            val result = supabaseService.createFountain(fountainDto)
            
            if (result.isSuccess) {
                val fountainCode = result.getOrNull()!!
                SecureLog.i(TAG, "Successfully created fountain with code: $fountainCode")
                Result.success(fountainCode)
            } else {
                val error = result.exceptionOrNull()
                SecureLog.e(TAG, "Failed to create fountain: ${error?.message}")
                Result.failure(error ?: Exception("Failed to create fountain"))
            }
        } catch (e: Exception) {
            SecureLog.e(TAG, "Error creating fountain: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun softDeleteFountain(fountainId: String): Result<Unit> {
        return try {
            // Check internet connectivity
            if (!NetworkUtil.isNetworkAvailable(context)) {
                SecureLog.e(TAG, "No internet connection for deleting fountain")
                return Result.failure(NoInternetException("No internet connection. Please check your network settings."))
            }
            
            // Call Supabase service
            val result = supabaseService.softDeleteFountain(fountainId)
            
            if (result.isSuccess) {
                SecureLog.i(TAG, "Successfully soft deleted fountain: $fountainId")
                Result.success(Unit)
            } else {
                val error = result.exceptionOrNull()
                SecureLog.e(TAG, "Failed to delete fountain: ${error?.message}")
                Result.failure(error ?: Exception("Failed to delete fountain"))
            }
        } catch (e: Exception) {
            SecureLog.e(TAG, "Error deleting fountain: ${e.message}", e)
            Result.failure(e)
        }
    }
}

/**
 * Exception thrown when there's no internet connection
 */
class NoInternetException(message: String) : Exception(message)
