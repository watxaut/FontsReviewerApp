package com.watxaut.fontsreviewer.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtil {
    
    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get current location using FusedLocationProviderClient
     * Uses PRIORITY_BALANCED_POWER_ACCURACY for coarse location
     */
    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) {
            return null
        }
        
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)
        
        return suspendCancellableCoroutine { continuation ->
            try {
                val cancellationTokenSource = CancellationTokenSource()
                
                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
                
                @Suppress("MissingPermission")
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    continuation.resume(location)
                }.addOnFailureListener {
                    continuation.resume(null)
                }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }
        }
    }
    
    /**
     * Calculate distance between two points in meters using Haversine formula
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in meters
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Check if fountain is within allowed range for operator
     * @param userLat User's latitude
     * @param userLon User's longitude
     * @param fountainLat Fountain's latitude
     * @param fountainLon Fountain's longitude
     * @param maxDistanceMeters Maximum allowed distance (default 300m)
     * @return true if fountain is within range
     */
    fun isWithinRange(
        userLat: Double,
        userLon: Double,
        fountainLat: Double,
        fountainLon: Double,
        maxDistanceMeters: Double = 300.0
    ): Boolean {
        val distance = calculateDistance(userLat, userLon, fountainLat, fountainLon)
        return distance <= maxDistanceMeters
    }
}
