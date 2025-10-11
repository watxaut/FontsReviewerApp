package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.model.User
import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, nickname: String, password: String): Result<User> {
        return repository.signUp(email, nickname, password)
    }
}
