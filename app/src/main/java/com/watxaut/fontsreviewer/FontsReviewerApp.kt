package com.watxaut.fontsreviewer

import android.app.Application
import com.mapbox.common.MapboxOptions
import com.watxaut.fontsreviewer.domain.usecase.InitializeFountainsUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FontsReviewerApp : Application() {

    @Inject
    lateinit var initializeFountainsUseCase: InitializeFountainsUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize Mapbox with access token
        MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN

        // Initialize fountains from CSV on first launch
        applicationScope.launch {
            try {
                initializeFountainsUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
