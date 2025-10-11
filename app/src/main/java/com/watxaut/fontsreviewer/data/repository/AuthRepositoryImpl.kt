package com.watxaut.fontsreviewer.data.repository

import android.util.Log
import com.watxaut.fontsreviewer.data.remote.service.SupabaseService
import com.watxaut.fontsreviewer.domain.model.User
import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseService: SupabaseService
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepositoryImpl"
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            Log.d(TAG, "=== AUTH REPOSITORY: SIGN IN START ===")
            Log.d(TAG, "Email: $email")
            
            Log.d(TAG, "Calling SupabaseService.signIn...")
            val userIdResult = supabaseService.signIn(email, password)
            
            if (userIdResult.isFailure) {
                val error = userIdResult.exceptionOrNull() ?: Exception("Sign in failed")
                Log.e(TAG, "SupabaseService.signIn failed: ${error.message}")
                return Result.failure(error)
            }

            val userId = userIdResult.getOrNull()
            Log.d(TAG, "Got user ID: $userId")
            
            if (userId == null) {
                Log.e(TAG, "User ID is null!")
                return Result.failure(Exception("No user ID"))
            }

            // Get profile to get nickname
            Log.d(TAG, "Getting profile for user: $userId")
            val profileResult = supabaseService.getProfile(userId)
            
            if (profileResult.isFailure) {
                val error = profileResult.exceptionOrNull() ?: Exception("Failed to get profile")
                Log.e(TAG, "getProfile failed: ${error.message}")
                return Result.failure(error)
            }

            val profile = profileResult.getOrNull()
            Log.d(TAG, "Got profile: $profile")
            
            if (profile == null) {
                Log.e(TAG, "Profile is null!")
                return Result.failure(Exception("No profile found"))
            }

            val user = User(
                id = profile.id,
                nickname = profile.nickname,
                totalRatings = profile.totalRatings,
                averageScore = profile.averageScore,
                bestFountainId = profile.bestFountainId
            )

            Log.d(TAG, "=== AUTH REPOSITORY: SIGN IN SUCCESS ===")
            Log.d(TAG, "User: $user")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "=== AUTH REPOSITORY: SIGN IN FAILED ===")
            Log.e(TAG, "Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, nickname: String, password: String): Result<User> {
        return try {
            Log.d(TAG, "=== AUTH REPOSITORY: SIGN UP START ===")
            Log.d(TAG, "Email: $email")
            Log.d(TAG, "Nickname: $nickname")
            
            Log.d(TAG, "Calling SupabaseService.signUp...")
            val userIdResult = supabaseService.signUp(email, password, nickname)
            
            if (userIdResult.isFailure) {
                val error = userIdResult.exceptionOrNull() ?: Exception("Sign up failed")
                Log.e(TAG, "SupabaseService.signUp failed: ${error.message}")
                return Result.failure(error)
            }

            val userId = userIdResult.getOrNull()
            Log.d(TAG, "Got user ID: $userId")
            
            if (userId == null) {
                Log.e(TAG, "User ID is null!")
                return Result.failure(Exception("No user ID"))
            }

            // Get profile
            Log.d(TAG, "Getting profile for user: $userId")
            val profileResult = supabaseService.getProfile(userId)
            
            if (profileResult.isFailure) {
                val error = profileResult.exceptionOrNull() ?: Exception("Failed to get profile")
                Log.e(TAG, "getProfile failed: ${error.message}")
                return Result.failure(error)
            }

            val profile = profileResult.getOrNull()
            Log.d(TAG, "Got profile: $profile")
            
            if (profile == null) {
                Log.e(TAG, "Profile is null!")
                return Result.failure(Exception("No profile found"))
            }

            val user = User(
                id = profile.id,
                nickname = profile.nickname,
                totalRatings = profile.totalRatings,
                averageScore = profile.averageScore,
                bestFountainId = profile.bestFountainId
            )

            Log.d(TAG, "=== AUTH REPOSITORY: SIGN UP SUCCESS ===")
            Log.d(TAG, "User: $user")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "=== AUTH REPOSITORY: SIGN UP FAILED ===")
            Log.e(TAG, "Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return supabaseService.signOut()
    }

    override suspend fun getCurrentUser(): User? {
        return try {
            val userId = supabaseService.getCurrentUserId() ?: return null

            val profileResult = supabaseService.getProfile(userId)
            if (profileResult.isFailure) return null

            val profile = profileResult.getOrNull() ?: return null

            User(
                id = profile.id,
                nickname = profile.nickname,
                totalRatings = profile.totalRatings,
                averageScore = profile.averageScore,
                bestFountainId = profile.bestFountainId
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return supabaseService.getCurrentUserId() != null
    }
}
