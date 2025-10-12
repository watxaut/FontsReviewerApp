package com.watxaut.fontsreviewer

import android.app.Application
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FontsReviewerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Mapbox with access token
        MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN

        // Note: Fountain data is now loaded directly from Supabase
        // No local database initialization needed
    }
}
