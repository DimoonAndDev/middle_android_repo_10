package ru.yandex.buggyweatherapp.data.repository

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.yandex.buggyweatherapp.domain.model.MyCustomLocation
import ru.yandex.buggyweatherapp.domain.repository.LocationRepository
import ru.yandex.buggyweatherapp.utils.LocationTracker
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val application: Application
) : LocationRepository {
    companion object {
        private const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application.applicationContext)
    private var currentLocation: MyCustomLocation? = null
    private var locationCallback: ((MyCustomLocation?) -> Unit)? = null
    private var activeLocationCallback: LocationCallback? = null

    //Проверка разрешений
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            application.applicationContext,
            LOCATION_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun handleLocationSuccess(
        location: Location,
        callback: (MyCustomLocation?) -> Unit
    ) {
        val userLocation = MyCustomLocation(location.latitude, location.longitude)
        currentLocation = userLocation
        callback(userLocation)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun getCurrentLocation(callback: (MyCustomLocation?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = MyCustomLocation(location.latitude, location.longitude)
                    currentLocation = userLocation
                    callback(userLocation)
                } else {
                    requestLocationUpdates(callback)
                }
            }
            .addOnFailureListener { e ->
                Log.e("LocationRepository", "Error getting location", e)
                callback(null)
            }
    }


    private fun requestLocationUpdates(callback: (MyCustomLocation?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }
        try {
            //остановка обновления в работе
            activeLocationCallback?.let { stopLocationUpdates() }
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .build()

            val newLocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        handleLocationSuccess(location,callback)
                        stopLocationUpdates()
                    }
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        callback(null)
                        stopLocationUpdates()
                    }
                }
            }
            activeLocationCallback = newLocationCallback

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                newLocationCallback,
                Looper.getMainLooper()
            )
            //если местоположение не получено - таймаут обновления
            Handler(Looper.getMainLooper()).postDelayed({
                callback(null)
                stopLocationUpdates()
            }, 5000)
        } catch (e: SecurityException) {
            Log.e("LocationRepository", "Location permission not granted", e)
            callback(null)
        }
    }
    private fun stopLocationUpdates() {
        activeLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            activeLocationCallback = null
        }
    }

    override suspend fun getCityNameFromLocation(location: MyCustomLocation): String? = withContext(
        Dispatchers.IO) {
        return@withContext try {
            val geocoder = Geocoder(application.applicationContext, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            addresses?.firstOrNull()?.let { address ->
                address.locality ?: address.subAdminArea ?: address.adminArea
            }
        } catch (e: Exception) {
            Log.e("LocationRepository", "Error getting city name", e)
            null
        }
    }
    //очистка ресурса
    override fun cleanin(){
        stopLocationUpdates()
        locationCallback = null
        currentLocation = null
    }
}