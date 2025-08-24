package ru.yandex.buggyweatherapp.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.yandex.buggyweatherapp.domain.model.MyCustomLocation
import ru.yandex.buggyweatherapp.domain.model.WeatherData
import ru.yandex.buggyweatherapp.domain.repository.LocationRepository
import ru.yandex.buggyweatherapp.domain.repository.WeatherRepository
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val imageLoader: ImageLoader
) : ViewModel() {

    companion object {
        private const val AUTO_REFRESH_DELAY = 60000L
        private const val ERROR_LOCATION = "Unable to get current location"
        private const val ERROR_CITY_NAME = "Error getting city name: "
        private const val ERROR_WEATHER = "Error fetching weather: "
        private const val ERROR_SEARCH = "Error searching weather: "
        private const val ERROR_EMPTY_CITY = "City name cannot be empty"
    }

    private lateinit var activityContext: Context

    val weatherData = MutableLiveData<WeatherData?>()
    val currentLocation = MutableLiveData<MyCustomLocation?>()
    val isLoading = MutableLiveData<Boolean>(false)
    val error = MutableLiveData<String?>()
    val cityName = MutableLiveData<String?>()

    private var refreshTimer: Timer? = null

    fun initialize(context: Context) {
        this.activityContext = context
        fetchCurrentLocationWeather()
        startAutoRefresh()
    }

    fun fetchCurrentLocationWeather() {
        isLoading.value = true
        error.value = null

        locationRepository.getCurrentLocation { location ->
            if (location != null) {
                currentLocation.value = location
                viewModelScope.launch {
                    try {
                        val cityNameFromLocation = withContext(Dispatchers.IO) {
                            locationRepository.getCityNameFromLocation(location)
                        }
                        cityName.value = cityNameFromLocation
                        getWeatherForLocation(location)
                    } catch (e: Exception) {
                        isLoading.value = false
                        error.value = ERROR_CITY_NAME + e.message
                    }
                }
            } else {
                isLoading.value = false
                error.value = ERROR_LOCATION
            }
        }
    }

    fun getWeatherForLocation(location: MyCustomLocation) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    weatherRepository.getWeatherData(location)
                }

                isLoading.value = false

                if (result.isSuccess) {
                    weatherData.value = result.getOrNull()
                    error.value = null
                } else {
                    error.value = result.exceptionOrNull()?.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                isLoading.value = false
                error.value = ERROR_WEATHER + e.message
            }
        }
    }

    fun searchWeatherByCity(city: String) {
        if (city.isBlank()) {
            error.value = ERROR_EMPTY_CITY
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    weatherRepository.getWeatherByCity(city)
                }

                isLoading.value = false

                if (result.isSuccess) {
                    val data = result.getOrNull()
                    weatherData.value = data
                    cityName.value = data?.cityName
                    data?.let {
                        currentLocation.value = MyCustomLocation(0.0, 0.0, it.cityName)
                    }
                } else {
                    error.value = result.exceptionOrNull()?.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                isLoading.value = false
                error.value = ERROR_SEARCH + e.message
            }
        }
    }

    fun formatTemperature(temp: Double): String {
        return "${temp.toInt()}°C"
    }

    suspend fun loadWeatherIcon(iconCode: String): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = imageLoader.execute(
                ImageRequest.Builder(activityContext)
                    .data(iconCode)
                    .build()
            )

            when (result) {
                is SuccessResult -> result.drawable.toBitmap()
                is ErrorResult -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun startAutoRefresh() {
        refreshTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    currentLocation.value?.let { location ->
                        getWeatherForLocation(location)
                    }
                }
            }, AUTO_REFRESH_DELAY, AUTO_REFRESH_DELAY)
        }
    }

    fun toggleFavorite() {
        weatherData.value?.let {
            it.isFavorite = !it.isFavorite
            weatherData.value = it
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshTimer?.cancel()
        refreshTimer = null
        locationRepository.cleanin()
    }
}