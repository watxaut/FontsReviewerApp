package com.watxaut.fontsreviewer.data.remote.service

import com.watxaut.fontsreviewer.data.remote.dto.CreateProfileDto
import com.watxaut.fontsreviewer.util.SecureLog
import com.watxaut.fontsreviewer.data.remote.dto.CreateReviewDto
import com.watxaut.fontsreviewer.data.remote.dto.FountainStatsDto
import com.watxaut.fontsreviewer.data.remote.dto.FountainWithStatsDto
import com.watxaut.fontsreviewer.data.remote.dto.LeaderboardDto
import com.watxaut.fontsreviewer.data.remote.dto.ProfileDto
import com.watxaut.fontsreviewer.data.remote.dto.ReviewDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseService @Inject constructor(
    private val client: SupabaseClient
) {

    companion object {
        private const val TAG = "SupabaseService"
    }

    // ==================== Authentication ====================

    suspend fun signUp(email: String, password: String, nickname: String): Result<String> {
        return try {
            SecureLog.d(TAG, "=== SIGN UP START ===")
            // SECURITY: Never log email/password in production
            SecureLog.d(TAG, "Nickname: $nickname")
            
            // First, check if the nickname is already taken
            SecureLog.d(TAG, "Checking if nickname is available...")
            val nicknameCheck = try {
                client.from("profiles")
                    .select {
                        filter {
                            eq("nickname", nickname)
                        }
                    }
                    .decodeList<ProfileDto>()
            } catch (e: Exception) {
                SecureLog.w(TAG, "Could not check nickname availability: ${e.message}")
                emptyList()
            }
            
            if (nicknameCheck.isNotEmpty()) {
                SecureLog.e(TAG, "Nickname '$nickname' is already taken")
                return Result.failure(Exception("Nickname is already taken"))
            }
            SecureLog.d(TAG, "Nickname is available")
            
            SecureLog.d(TAG, "Calling Supabase signUpWith...")
            try {
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    data = buildJsonObject {
                        put("nickname", nickname)
                    }
                }
                SecureLog.d(TAG, "signUpWith completed successfully")
            } catch (authException: Exception) {
                SecureLog.e(TAG, "Auth signUpWith failed: ${authException.message}", authException)
                
                // Check if user was created despite the error
                SecureLog.d(TAG, "Checking if user was created despite error...")
                val session = client.auth.currentSessionOrNull()
                if (session == null) {
                    SecureLog.e(TAG, "No session found, signup truly failed")
                    throw authException
                }
                SecureLog.w(TAG, "User was created despite error, continuing...")
            }

            // Get current session
            SecureLog.d(TAG, "Getting current session...")
            val session = client.auth.currentSessionOrNull()
            SecureLog.d(TAG, "Session: ${if (session != null) "exists" else "null"}")
            
            val user = session?.user
            SecureLog.d(TAG, "User: ${if (user != null) "exists" else "null"}")
            
            val userId = user?.id
            // SECURITY: Never log user IDs in production
            SecureLog.d(TAG, "User ID retrieved")
            
            if (userId == null) {
                SecureLog.e(TAG, "No user ID after signup!")
                return Result.failure(Exception("No user ID"))
            }

            // Check if profile already exists (from trigger)
            SecureLog.d(TAG, "Checking if profile already exists...")
            val existingProfile = try {
                getProfile(userId).getOrNull()
            } catch (e: Exception) {
                SecureLog.d(TAG, "Profile doesn't exist yet: ${e.message}")
                null
            }
            
            if (existingProfile != null) {
                SecureLog.d(TAG, "Profile already exists (created by trigger): $existingProfile")
                SecureLog.d(TAG, "=== SIGN UP SUCCESS ===")
                return Result.success(userId)
            }

            // Manually create profile if trigger didn't work
            SecureLog.d(TAG, "Creating profile manually for user: $userId")
            try {
                createProfile(userId, nickname)
                SecureLog.d(TAG, "Profile created successfully")
            } catch (e: Exception) {
                SecureLog.e(TAG, "Error creating profile: ${e.message}", e)
                
                // Last chance: check if it was created between our checks
                val finalCheck = try {
                    getProfile(userId).getOrNull()
                } catch (checkError: Exception) {
                    null
                }
                
                if (finalCheck == null) {
                    SecureLog.e(TAG, "Failed to create profile, and none exists")
                    throw e
                }
                
                SecureLog.w(TAG, "Profile exists despite error, continuing")
            }

            SecureLog.d(TAG, "=== SIGN UP SUCCESS ===")
            Result.success(userId)
        } catch (e: Exception) {
            SecureLog.e(TAG, "=== SIGN UP FAILED ===")
            SecureLog.e(TAG, "Error type: ${e.javaClass.simpleName}")
            SecureLog.e(TAG, "Error message: ${e.message}")
            SecureLog.e(TAG, "Error stack trace:", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            SecureLog.d(TAG, "=== SIGN IN START ===")
            SecureLog.d(TAG, "Email: $email")
            SecureLog.d(TAG, "Password length: ${password.length}")
            
            SecureLog.d(TAG, "Calling Supabase signInWith...")
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            SecureLog.d(TAG, "signInWith completed successfully")
            
            // Get current session
            SecureLog.d(TAG, "Getting current session...")
            val session = client.auth.currentSessionOrNull()
            SecureLog.d(TAG, "Session: ${if (session != null) "exists" else "null"}")
            
            val user = session?.user
            SecureLog.d(TAG, "User: ${if (user != null) "exists" else "null"}")
            
            val userId = user?.id
            SecureLog.d(TAG, "User ID: $userId")
            
            if (userId == null) {
                SecureLog.e(TAG, "No user ID after signin!")
                return Result.failure(Exception("No user ID"))
            }
            
            SecureLog.d(TAG, "=== SIGN IN SUCCESS ===")
            Result.success(userId)
        } catch (e: Exception) {
            SecureLog.e(TAG, "=== SIGN IN FAILED ===")
            SecureLog.e(TAG, "Error type: ${e.javaClass.simpleName}")
            SecureLog.e(TAG, "Error message: ${e.message}")
            SecureLog.e(TAG, "Error stack trace:", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            // Get current user ID before deletion
            val userId = getCurrentUserId()
            if (userId == null) {
                return Result.failure(Exception("No user logged in"))
            }
            
            // Delete user data from profiles table
            // Note: Reviews will be handled by database cascade delete or you can delete them first
            client.from("profiles")
                .delete {
                    filter {
                        eq("id", userId)
                    }
                }
            
            // Sign out the user (Supabase doesn't provide direct account deletion via SDK)
            // The actual user deletion from auth.users should be handled by a Supabase Edge Function
            // or database trigger, or manually by admin
            client.auth.signOut()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    suspend fun isUserLoggedIn(): Boolean {
        return client.auth.currentUserOrNull() != null
    }

    // ==================== Profiles ====================

    private suspend fun createProfile(userId: String, nickname: String) {
        SecureLog.d(TAG, "createProfile() called with userId: $userId, nickname: $nickname")
        try {
            client.from("profiles").insert(
                CreateProfileDto(id = userId, nickname = nickname)
            )
            SecureLog.d(TAG, "createProfile() insert completed")
        } catch (e: Exception) {
            SecureLog.e(TAG, "createProfile() failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun getProfile(userId: String): Result<ProfileDto> {
        return try {
            SecureLog.d(TAG, "getProfile() called with userId: $userId")
            val profile = client.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<ProfileDto>()
            SecureLog.d(TAG, "getProfile() success: $profile")
            Result.success(profile)
        } catch (e: Exception) {
            SecureLog.e(TAG, "getProfile() failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateNickname(userId: String, newNickname: String): Result<Unit> {
        return try {
            client.from("profiles")
                .update(mapOf("nickname" to newNickname)) {
                    filter {
                        eq("id", userId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Reviews ====================

    suspend fun createReview(review: CreateReviewDto): Result<ReviewDto> {
        return try {
            val created = client.from("reviews")
                .insert(review) {
                    select()
                }
                .decodeSingle<ReviewDto>()
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReviewsForFountain(fountainId: String): Result<List<ReviewDto>> {
        return try {
            val reviews = client.from("reviews")
                .select {
                    filter {
                        eq("fountain_id", fountainId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<ReviewDto>()
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserReview(userId: String, fountainId: String): Result<ReviewDto?> {
        return try {
            val reviews = client.from("reviews")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("fountain_id", fountainId)
                    }
                }
                .decodeList<ReviewDto>()
            Result.success(reviews.firstOrNull())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all fountain IDs that a user has reviewed
     */
    suspend fun getUserReviewedFountainIds(userId: String): Result<List<String>> {
        return try {
            val reviews = client.from("reviews")
                .select(Columns.list("fountain_id")) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<Map<String, String>>()
            val fountainIds = reviews.mapNotNull { it["fountain_id"] }
            SecureLog.d(TAG, "User has reviewed ${fountainIds.size} fountains")
            Result.success(fountainIds)
        } catch (e: Exception) {
            SecureLog.e(TAG, "Failed to get user reviewed fountains: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateReview(reviewId: String, updates: CreateReviewDto): Result<ReviewDto> {
        return try {
            val updateMap = mutableMapOf<String, Any>(
                "taste" to updates.taste,
                "freshness" to updates.freshness,
                "location_rating" to updates.locationRating,
                "aesthetics" to updates.aesthetics,
                "splash" to updates.splash,
                "jet" to updates.jet
            )
            updates.comment?.let { updateMap["comment"] = it }
            
            val updated = client.from("reviews")
                .update(updateMap) {
                    filter {
                        eq("id", reviewId)
                    }
                    select()
                }
                .decodeSingle<ReviewDto>()
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReview(reviewId: String): Result<Unit> {
        return try {
            client.from("reviews")
                .delete {
                    filter {
                        eq("id", reviewId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Leaderboard ====================

    suspend fun getLeaderboard(limit: Int = 100): Result<List<LeaderboardDto>> {
        return try {
            val leaderboard = client.from("leaderboard")
                .select {
                    limit(limit.toLong())
                }
                .decodeList<LeaderboardDto>()
            Result.success(leaderboard)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Fountain Statistics ====================
    
    suspend fun getAllFountainStats(): Result<List<FountainStatsDto>> {
        return try {
            val stats = client.from("fountain_stats")
                .select()
                .decodeList<FountainStatsDto>()
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Fountains ====================
    
    /**
     * Get all fountains with their review statistics using pagination
     * Fetches in batches of 1000 to work around Supabase max-rows limit
     * Only includes non-deleted fountains (is_deleted = false)
     */
    suspend fun getAllFountainsWithStats(): Result<List<FountainWithStatsDto>> {
        return try {
            val allFountains = mutableListOf<FountainWithStatsDto>()
            var offset = 0
            val batchSize = 1000
            
            SecureLog.i(TAG, "Fetching fountains from Supabase in batches...")
            
            while (true) {
                val batch = client.from("fountain_stats_detailed")
                    .select {
                        range(offset.toLong(), (offset + batchSize - 1).toLong())
                    }
                    .decodeList<FountainWithStatsDto>()
                
                if (batch.isEmpty()) {
                    break
                }
                
                allFountains.addAll(batch)
                SecureLog.i(TAG, "Fetched batch: ${batch.size} fountains (total so far: ${allFountains.size})")
                
                // If we got less than batchSize, we've reached the end
                if (batch.size < batchSize) {
                    break
                }
                
                offset += batchSize
            }
            
            SecureLog.i(TAG, "Finished fetching all ${allFountains.size} fountains from Supabase")
            Result.success(allFountains)
        } catch (e: Exception) {
            SecureLog.e(TAG, "Failed to fetch fountains: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all fountains INCLUDING deleted ones (admin only)
     * Fetches from the fountains table directly, not the view
     */
    suspend fun getAllFountainsIncludingDeleted(): Result<List<FountainWithStatsDto>> {
        return try {
            val allFountains = mutableListOf<FountainWithStatsDto>()
            var offset = 0
            val batchSize = 1000
            
            SecureLog.i(TAG, "Fetching ALL fountains (including deleted) from Supabase...")
            
            while (true) {
                val batch = client.from("fountains")
                    .select(Columns.raw("""
                        codi,
                        nom,
                        carrer,
                        numero_carrer,
                        latitude,
                        longitude,
                        is_deleted,
                        total_reviews:reviews(count),
                        average_rating:reviews.overall.avg()
                    """)) {
                        range(offset.toLong(), (offset + batchSize - 1).toLong())
                    }
                    .decodeList<FountainWithStatsDto>()
                
                if (batch.isEmpty()) {
                    break
                }
                
                allFountains.addAll(batch)
                SecureLog.i(TAG, "Fetched batch: ${batch.size} fountains (total so far: ${allFountains.size})")
                
                if (batch.size < batchSize) {
                    break
                }
                
                offset += batchSize
            }
            
            SecureLog.i(TAG, "Finished fetching all ${allFountains.size} fountains (including deleted)")
            Result.success(allFountains)
        } catch (e: Exception) {
            SecureLog.e(TAG, "Failed to fetch fountains with deleted: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get a single fountain by codi
     */
    suspend fun getFountainByCodi(codi: String): Result<FountainWithStatsDto> {
        return try {
            val fountain = client.from("fountain_stats_detailed")
                .select {
                    filter {
                        eq("codi", codi)
                    }
                }
                .decodeSingle<FountainWithStatsDto>()
            Result.success(fountain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new fountain (admin only)
     */
    suspend fun createFountain(fountain: com.watxaut.fontsreviewer.data.remote.dto.CreateFountainDto): Result<String> {
        return try {
            SecureLog.d(TAG, "createFountain() called for: ${fountain.nom}")
            
            // Generate a unique fountain code
            val fountainCode = generateFountainCode()
            
            // Create fountain with generated code
            val fountainData = mapOf(
                "codi" to fountainCode,
                "nom" to fountain.nom,
                "carrer" to fountain.carrer,
                "numero_carrer" to fountain.numeroCarrer,
                "latitude" to fountain.latitude,
                "longitude" to fountain.longitude,
                "is_deleted" to false
            )
            
            client.from("fountains")
                .insert(fountainData)
            
            SecureLog.d(TAG, "createFountain() success, code: $fountainCode")
            Result.success(fountainCode)
        } catch (e: Exception) {
            SecureLog.e(TAG, "createFountain() error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Soft delete a fountain (admin only)
     */
    suspend fun softDeleteFountain(fountainId: String): Result<Unit> {
        return try {
            SecureLog.d(TAG, "softDeleteFountain() called for: $fountainId")
            
            client.from("fountains")
                .update(mapOf("is_deleted" to true)) {
                    filter {
                        eq("codi", fountainId)
                    }
                }
            
            SecureLog.d(TAG, "softDeleteFountain() success")
            Result.success(Unit)
        } catch (e: Exception) {
            SecureLog.e(TAG, "softDeleteFountain() error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate a unique fountain code in format: ADM_YYYYMMDD_HHMMSS
     */
    private fun generateFountainCode(): String {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        return "ADM_$timestamp"
    }
}