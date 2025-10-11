package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.model.User
import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        return repository.signIn(email, password)
    }
}
