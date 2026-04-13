package com.icoffee.app.data.location

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

object LocationProvider {
    // Istanbul Beyoglu — fallback when GPS is unavailable or denied
    const val FALLBACK_LAT = 41.0082
    const val FALLBACK_LON = 28.9784

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Returns the device's best current location, or null if unavailable.
     * Caller must verify location permission before calling.
     */
    suspend fun getCurrentLocation(): Pair<Double, Double>? {
        val ctx = appContext ?: return null
        return try {
            val client = LocationServices.getFusedLocationProviderClient(ctx)
            val cts = CancellationTokenSource()
            val location = client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cts.token
            ).await()
            location?.let { it.latitude to it.longitude }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    /** Non-blocking last-known location (may be null or stale). */
    suspend fun getLastKnownLocation(): Pair<Double, Double>? {
        val ctx = appContext ?: return null
        return try {
            val client = LocationServices.getFusedLocationProviderClient(ctx)
            val location = client.lastLocation.await()
            location?.let { it.latitude to it.longitude }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
