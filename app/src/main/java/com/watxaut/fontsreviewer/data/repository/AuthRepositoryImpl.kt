package com.watxaut.fontsreviewer.data.repository

import com.watxaut.fontsreviewer.util.SecureLog
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
            SecureLog.d(TAG, "=== AUTH REPOSITORY: SIGN IN START ===")
            SecureLog.d(TAG, "Email: $email")
            
            SecureLog.d(TAG, "Calling SupabaseService.signIn...")
            val userIdResult = supabaseService.signIn(email, password)
            
            if (userIdResult.isFailure) {
                val error = userIdResult.exceptionOrNull() ?: Exception("Sign in failed")
                SecureLog.e(TAG, "SupabaseService.signIn failed: ${error.message}")
                return Result.failure(error)
            }

            val userId = userIdResult.getOrNull()
            SecureLog.d(TAG, "Got user ID: $userId")
            
            if (userId == null) {
                SecureLog.e(TAG, "User ID is null!")
                return Result.failure(Exception("No user ID"))
            }

            // Get profile to get nickname
            SecureLog.d(TAG, "Getting profile for user: $userId")
            val profileResult = supabaseService.getProfile(userId)
            
            if (profileResult.isFailure) {
                val error = profileResult.exceptionOrNull() ?: Exception("Failed to get profile")
                SecureLog.e(TAG, "getProfile failed: ${error.message}")
                return Result.failure(error)
            }

            val profile = profileResult.getOrNull()
            SecureLog.d(TAG, "Got profile: $profile")
            
            if (profile == null) {
                SecureLog.e(TAG, "Profile is null!")
                return Result.failure(Exception("No profile found"))
            }

            val user = User(
                id = profile.id,
                nickname = profile.nickname,
                totalRatings = profile.totalRatings,
                averageScore = profile.averageScore,
                bestFountainId = profile.bestFountainId
            )

            SecureLog.d(TAG, "=== AUTH REPOSITORY: SIGN IN SUCCESS ===")
            SecureLog.d(TAG, "User: $user")
            Result.success(user)
        } catch (e: Exception) {
            SecureLog.e(TAG, "=== AUTH REPOSITORY: SIGN IN FAILED ===")
            SecureLog.e(TAG, "Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, nickname: String, password: String): Result<User> {
        return try {
            SecureLog.d(TAG, "=== AUTH REPOSITORY: SIGN UP START ===")
            SecureLog.d(TAG, "Email: $email")
            SecureLog.d(TAG, "Nickname: $nickname")
            
            SecureLog.d(TAG, "Calling SupabaseService.signUp...")
            val userIdResult = supabaseService.signUp(email, password, nickname)
            
            if (userIdResult.isFailure) {
                val error = userIdResult.exceptionOrNull() ?: Exception("Sign up failed")
                SecureLog.e(TAG, "SupabaseService.signUp failed: ${error.message}")
                return Result.failure(error)
            }

            val userId = userIdResult.getOrNull()
            SecureLog.d(TAG, "Got user ID: $userId")
            
            if (userId == null) {
                SecureLog.e(TAG, "User ID is null!")
                return Result.failure(Exception("No user ID"))
            }

            // Get profile
            SecureLog.d(TAG, "Getting profile for user: $userId")
            val profileResult = supabaseService.getProfile(userId)
            
            if (profileResult.isFailure) {
                val error = profileResult.exceptionOrNull() ?: Exception("Failed to get profile")
                SecureLog.e(TAG, "getProfile failed: ${error.message}")
                return Result.failure(error)
            }

            val profile = profileResult.getOrNull()
            SecureLog.d(TAG, "Got profile: $profile")
            
            if (profile == null) {
                SecureLog.e(TAG, "Profile is null!")
                return Result.failure(Exception("No profile found"))
            }

            val user = User(
                id = profile.id,
                nickname = profile.nickname,
                totalRatings = profile.totalRatings,
                averageScore = profile.averageScore,
                bestFountainId = profile.bestFountainId
            )

            SecureLog.d(TAG, "=== AUTH REPOSITORY: SIGN UP SUCCESS ===")
            SecureLog.d(TAG, "User: $user")
            Result.success(user)
        } catch (e: Exception) {
            SecureLog.e(TAG, "=== AUTH REPOSITORY: SIGN UP FAILED ===")
            SecureLog.e(TAG, "Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return supabaseService.signOut()
    }
    
    override suspend fun deleteAccount(): Result<Unit> {
        return supabaseService.deleteAccount()
    }

    override suspend fun getCurrentUser(): User? {
        return try {
            android.util.Log.e(TAG, "DEBUG: getCurrentUser() called")
            val userId = supabaseService.getCurrentUserId()
            android.util.Log.e(TAG, "DEBUG: getCurrentUserId() = $userId")
            if (userId == null) {
                android.util.Log.e(TAG, "DEBUG: No user ID - user not logged in")
                return null
            }

            val profileResult = supabaseService.getProfile(userId)
            android.util.Log.e(TAG, "DEBUG: getProfile result = ${profileResult.isSuccess}")
            if (profileResult.isFailure) {
                android.util.Log.e(TAG, "DEBUG: getProfile failed: ${profileResult.exceptionOrNull()?.message}")
                return null
            }

            val profile = profileResult.getOrNull()
            android.util.Log.e(TAG, "DEBUG: Profile = $profile")
            if (profile == null) {
                android.util.Log.e(TAG, "DEBUG: Profile is null")
                return null
            }

            val user = User(
                id = profile.id,
                nickname = profile.nickname,
                totalRatings = profile.totalRatings,
                averageScore = profile.averageScore,
                bestFountainId = profile.bestFountainId,
                role = com.watxaut.fontsreviewer.domain.model.UserRole.fromString(profile.role)
            )
            android.util.Log.e(TAG, "DEBUG: Created User object: $user")
            android.util.Log.e(TAG, "DEBUG: User role enum: ${user.role}")
            user
        } catch (e: Exception) {
            android.util.Log.e(TAG, "DEBUG: Exception in getCurrentUser: ${e.message}", e)
            null
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return supabaseService.getCurrentUserId() != null
    }
}
