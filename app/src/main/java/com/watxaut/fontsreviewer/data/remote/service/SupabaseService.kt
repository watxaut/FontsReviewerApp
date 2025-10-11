package com.watxaut.fontsreviewer.data.remote.service

import android.util.Log
import com.watxaut.fontsreviewer.data.remote.dto.CreateProfileDto
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
            Log.d(TAG, "=== SIGN UP START ===")
            Log.d(TAG, "Email: $email")
            Log.d(TAG, "Nickname: $nickname")
            Log.d(TAG, "Password length: ${password.length}")
            
            // First, check if the nickname is already taken
            Log.d(TAG, "Checking if nickname is available...")
            val nicknameCheck = try {
                client.from("profiles")
                    .select {
                        filter {
                            eq("nickname", nickname)
                        }
                    }
                    .decodeList<ProfileDto>()
            } catch (e: Exception) {
                Log.w(TAG, "Could not check nickname availability: ${e.message}")
                emptyList()
            }
            
            if (nicknameCheck.isNotEmpty()) {
                Log.e(TAG, "Nickname '$nickname' is already taken")
                return Result.failure(Exception("Nickname is already taken"))
            }
            Log.d(TAG, "Nickname is available")
            
            Log.d(TAG, "Calling Supabase signUpWith...")
            try {
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    data = buildJsonObject {
                        put("nickname", nickname)
                    }
                }
                Log.d(TAG, "signUpWith completed successfully")
            } catch (authException: Exception) {
                Log.e(TAG, "Auth signUpWith failed: ${authException.message}", authException)
                
                // Check if user was created despite the error
                Log.d(TAG, "Checking if user was created despite error...")
                val session = client.auth.currentSessionOrNull()
                if (session == null) {
                    Log.e(TAG, "No session found, signup truly failed")
                    throw authException
                }
                Log.w(TAG, "User was created despite error, continuing...")
            }

            // Get current session
            Log.d(TAG, "Getting current session...")
            val session = client.auth.currentSessionOrNull()
            Log.d(TAG, "Session: ${if (session != null) "exists" else "null"}")
            
            val user = session?.user
            Log.d(TAG, "User: ${if (user != null) "exists" else "null"}")
            
            val userId = user?.id
            Log.d(TAG, "User ID: $userId")
            
            if (userId == null) {
                Log.e(TAG, "No user ID after signup!")
                return Result.failure(Exception("No user ID"))
            }

            // Check if profile already exists (from trigger)
            Log.d(TAG, "Checking if profile already exists...")
            val existingProfile = try {
                getProfile(userId).getOrNull()
            } catch (e: Exception) {
                Log.d(TAG, "Profile doesn't exist yet: ${e.message}")
                null
            }
            
            if (existingProfile != null) {
                Log.d(TAG, "Profile already exists (created by trigger): $existingProfile")
                Log.d(TAG, "=== SIGN UP SUCCESS ===")
                return Result.success(userId)
            }

            // Manually create profile if trigger didn't work
            Log.d(TAG, "Creating profile manually for user: $userId")
            try {
                createProfile(userId, nickname)
                Log.d(TAG, "Profile created successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating profile: ${e.message}", e)
                
                // Last chance: check if it was created between our checks
                val finalCheck = try {
                    getProfile(userId).getOrNull()
                } catch (checkError: Exception) {
                    null
                }
                
                if (finalCheck == null) {
                    Log.e(TAG, "Failed to create profile, and none exists")
                    throw e
                }
                
                Log.w(TAG, "Profile exists despite error, continuing")
            }

            Log.d(TAG, "=== SIGN UP SUCCESS ===")
            Result.success(userId)
        } catch (e: Exception) {
            Log.e(TAG, "=== SIGN UP FAILED ===")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Error stack trace:", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            Log.d(TAG, "=== SIGN IN START ===")
            Log.d(TAG, "Email: $email")
            Log.d(TAG, "Password length: ${password.length}")
            
            Log.d(TAG, "Calling Supabase signInWith...")
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Log.d(TAG, "signInWith completed successfully")
            
            // Get current session
            Log.d(TAG, "Getting current session...")
            val session = client.auth.currentSessionOrNull()
            Log.d(TAG, "Session: ${if (session != null) "exists" else "null"}")
            
            val user = session?.user
            Log.d(TAG, "User: ${if (user != null) "exists" else "null"}")
            
            val userId = user?.id
            Log.d(TAG, "User ID: $userId")
            
            if (userId == null) {
                Log.e(TAG, "No user ID after signin!")
                return Result.failure(Exception("No user ID"))
            }
            
            Log.d(TAG, "=== SIGN IN SUCCESS ===")
            Result.success(userId)
        } catch (e: Exception) {
            Log.e(TAG, "=== SIGN IN FAILED ===")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Error stack trace:", e)
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

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    suspend fun isUserLoggedIn(): Boolean {
        return client.auth.currentUserOrNull() != null
    }

    // ==================== Profiles ====================

    private suspend fun createProfile(userId: String, nickname: String) {
        Log.d(TAG, "createProfile() called with userId: $userId, nickname: $nickname")
        try {
            client.from("profiles").insert(
                CreateProfileDto(id = userId, nickname = nickname)
            )
            Log.d(TAG, "createProfile() insert completed")
        } catch (e: Exception) {
            Log.e(TAG, "createProfile() failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun getProfile(userId: String): Result<ProfileDto> {
        return try {
            Log.d(TAG, "getProfile() called with userId: $userId")
            val profile = client.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<ProfileDto>()
            Log.d(TAG, "getProfile() success: $profile")
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "getProfile() failed: ${e.message}", e)
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
     */
    suspend fun getAllFountainsWithStats(): Result<List<FountainWithStatsDto>> {
        return try {
            val allFountains = mutableListOf<FountainWithStatsDto>()
            var offset = 0
            val batchSize = 1000
            
            Log.i(TAG, "Fetching fountains from Supabase in batches...")
            
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
                Log.i(TAG, "Fetched batch: ${batch.size} fountains (total so far: ${allFountains.size})")
                
                // If we got less than batchSize, we've reached the end
                if (batch.size < batchSize) {
                    break
                }
                
                offset += batchSize
            }
            
            Log.i(TAG, "Finished fetching all ${allFountains.size} fountains from Supabase")
            Result.success(allFountains)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch fountains: ${e.message}", e)
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
}