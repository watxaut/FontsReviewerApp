package com.watxaut.fontsreviewer.domain.repository

import com.watxaut.fontsreviewer.domain.model.User

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, nickname: String, password: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): User?
    fun isUserLoggedIn(): Boolean
}
