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
//2. Вынесено в папку data. Создан отдельный репозиторийImpl.логика Clean Architecture
//3. Вызов через HILT для введения правильного DI
//6. решены ошибки: DI, проверка разрешений для безопаасности, очистка ресурсов,
// работа с потоками и корутинами, чтобы не зависало, введение доп методов
@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val application: Application //6. использование Hilt
) : LocationRepository {
    companion object {
        private const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION //6. константа для разрешений
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application.applicationContext)//6.правильный вызов контекста
    private var currentLocation: MyCustomLocation? = null//6.Чтобы не совпадало с встроенной Location - переименован класс
    private var locationCallback: ((MyCustomLocation?) -> Unit)? = null
    private var activeLocationCallback: LocationCallback? = null//6. отслеживание активного колбека для очистки ресурсов

    //6.Проверка разрешений в отдельной функции
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            application.applicationContext,
            LOCATION_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }
    //6. идентичный код вынесен в метод
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
        //6. проверка разрешений с возвратом - безопасность
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
        //6. доп. проверка разрешений
        if (!hasLocationPermission()) {
            callback(null)
            return
        }
        try {
            //остановка обновления в работе - убираем утечки памяти и разгружаем процесс
            activeLocationCallback?.let { stopLocationUpdates() }
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .build()

            val newLocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        handleLocationSuccess(location,callback)
                        //6. после получение локации - останавливаем процесс. избегаем утечки
                        stopLocationUpdates()
                    }
                }
                //6. Проверка доступности локации. Если недоступна - останавливаем обновление
                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        callback(null)
                        stopLocationUpdates()
                    }
                }
            }
            //6. учитываем колбек для очистки ресурсов
            activeLocationCallback = newLocationCallback

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                newLocationCallback,
                Looper.getMainLooper()
            )
            //если местоположение не получено - таймаут обновления, чтобы не загружать поток. лучше вообще наверно уйти на корутины
            Handler(Looper.getMainLooper()).postDelayed({
                callback(null)
                stopLocationUpdates()
            }, 5000)
        } catch (e: SecurityException) {
            Log.e("LocationRepository", "Location permission not granted", e)
            callback(null)
        }
    }
    //6. очистка ресурсов не пустая
    private fun stopLocationUpdates() {
        activeLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            activeLocationCallback = null
        }
    }
    //6. Переведено на корутины с диспатчером IO, чтобы не загружать основной поток
    override suspend fun getCityNameFromLocation(location: MyCustomLocation): String? = withContext(
        Dispatchers.IO) {
        return@withContext try {
            val geocoder = Geocoder(application.applicationContext, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            //6. проверка null
            addresses?.firstOrNull()?.let { address ->
                address.locality ?: address.subAdminArea ?: address.adminArea
            }
        } catch (e: Exception) {
            Log.e("LocationRepository", "Error getting city name", e)
            null
        }
    }
    //очистка ресурса - избегаем утечек памяти
    override fun cleanin(){
        stopLocationUpdates()
        locationCallback = null
        currentLocation = null
    }
}