package com.watxaut.fontsreviewer.di

import android.content.Context
import com.watxaut.fontsreviewer.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.android.Android
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(
        @ApplicationContext context: Context
    ): SupabaseClient {
        android.util.Log.e("NetworkModule", "Creating Supabase client with session storage...")
        
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Auth) {
                // Session storage is enabled by default in Supabase Kotlin client
                // It uses SharedPreferences automatically on Android
                // Just make sure we don't disable it
            }
            install(Postgrest)
            
            // Use Android HTTP client
            httpEngine = Android.create()
        }
    }
}
