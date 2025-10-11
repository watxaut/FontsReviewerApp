package com.watxaut.fontsreviewer.di

import com.watxaut.fontsreviewer.data.repository.AuthRepositoryImpl
import com.watxaut.fontsreviewer.data.repository.FountainRepositoryImpl
import com.watxaut.fontsreviewer.data.repository.ReviewRepositoryImpl
import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import com.watxaut.fontsreviewer.domain.repository.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFountainRepository(
        impl: FountainRepositoryImpl
    ): FountainRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(
        impl: ReviewRepositoryImpl
    ): ReviewRepository
}
