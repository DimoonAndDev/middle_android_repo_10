package ru.yandex.buggyweatherapp.utils

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

class LocationTracker private constructor(
    
    private val context: Context
) {
    
    companion object {
        @Volatile
        private var instance: LocationTracker? = null
        
        fun getInstance(context: Context): LocationTracker {
            return instance ?: synchronized(this) {
                instance ?: LocationTracker(context).also { instance = it }
            }
        }
    }
    
    
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    
    private val listeners = CopyOnWriteArrayList<(ru.yandex.buggyweatherapp.domain.model.Location) -> Unit>()
    
    
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            
            val newLocation = ru.yandex.buggyweatherapp.domain.model.Location(
                latitude = location.latitude,
                longitude = location.longitude
            )
            
            
            notifyListeners(newLocation)
        }
        
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        
        override fun onProviderEnabled(provider: String) {}
        
        override fun onProviderDisabled(provider: String) {}
    }
    
    
    fun startTracking() {
        try {
            
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000, // 5 секунд
                10f, // 10 метров
                locationListener
            )
            
            
        } catch (e: SecurityException) {
            Log.e("LocationTracker", "Permission denied", e)
        } catch (e: Exception) {
            Log.e("LocationTracker", "Error starting location tracking", e)
        }
    }
    
    
    fun addListener(listener: (ru.yandex.buggyweatherapp.domain.model.Location) -> Unit) {
        listeners.add(listener)
    }
    
    private fun notifyListeners(location: ru.yandex.buggyweatherapp.domain.model.Location) {
        
        Handler(Looper.getMainLooper()).post {
            for (listener in listeners) {
                listener(location)
            }
        }
    }
    
    
}