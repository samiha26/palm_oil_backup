package com.example.palm_oil.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null
    
    companion object {
        private const val TAG = "LocationHelper"
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
    
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }
    
    suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        
        return try {
            suspendCancellableCoroutine { continuation ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        Log.d(TAG, "Last known location: ${location?.latitude}, ${location?.longitude}, accuracy: ${location?.accuracy}")
                        continuation.resume(location)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to get last known location", exception)
                        continuation.resume(null)
                    }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting last known location", e)
            null
        }
    }
    
    fun getLocationUpdates(): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d(TAG, "New location: ${location.latitude}, ${location.longitude}, " +
                            "accuracy: ${location.accuracy}m, provider: ${location.provider}, " +
                            "time: ${location.time}")
                    trySend(location)
                }
            }
            
            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                Log.d(TAG, "Location availability: ${locationAvailability.isLocationAvailable}")
            }
        }
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500) // 500ms interval
            .setMinUpdateIntervalMillis(250) // Minimum 250ms between updates
            .setMinUpdateDistanceMeters(0.5f) // Update every 0.5 meters
            .setMaxUpdateDelayMillis(1000) // Maximum 1 second delay
            .build()
        
        try {
            Log.d(TAG, "Starting location updates...")
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                context.mainLooper
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception requesting location updates", e)
            close(e)
        }
        
        awaitClose {
            Log.d(TAG, "Stopping location updates...")
            locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        }
    }
    
    fun stopLocationUpdates() {
        locationCallback?.let { 
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
            Log.d(TAG, "Location updates stopped")
        }
    }
}
