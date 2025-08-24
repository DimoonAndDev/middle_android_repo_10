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


    private lateinit var activityContext: Context


    val weatherData = MutableLiveData<WeatherData>()
    val currentLocation = MutableLiveData<MyCustomLocation>()
    val isLoading = MutableLiveData<Boolean>()
    val error = MutableLiveData<String>()
    val cityName = MutableLiveData<String>()


    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())


    private var refreshTimer: Timer? = null


    fun initialize(context: Context) {
        this.activityContext = context
        viewModelScope.launch {
            fetchCurrentLocationWeather()
            startAutoRefresh()
        }
    }


    fun fetchCurrentLocationWeather() {
        viewModelScope.launch {
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
                            error.value = "Error getting city name: ${e.message}"
                        }
                    }
                } else {
                    isLoading.value = false
                    error.value = "Unable to get current location"
                }
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
                error.value = "Error fetching weather: ${e.message}"
            }
        }
    }

    fun searchWeatherByCity(city: String) {
        if (city.isBlank()) {
            error.value = "City name cannot be empty"
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
                error.value = "Error searching weather: ${e.message}"
            }
        }
    }


    fun formatTemperature(temp: Double): String {
        return "${temp.toInt()}°C"
    }


    fun loadWeatherIcon(iconCode: String): Bitmap {
        var bitmap: Bitmap = createBitmap(100, 100)
        coroutineScope.launch {

            try {
                val result = imageLoader.execute(
                    ImageRequest.Builder(activityContext)
                        .data(iconCode)
                        .build()
                )
                // Обработка результата

                when (result) {
                    is SuccessResult -> {
                        bitmap = result.drawable.toBitmap()
                    }

                    is ErrorResult -> {

                    }
                }
            } catch (e: Exception) {
            }
        }
        return bitmap
    }


    private fun startAutoRefresh() {
        refreshTimer = Timer()
        refreshTimer?.schedule(object : TimerTask() {
            override fun run() {
                currentLocation.value?.let { location ->
                    getWeatherForLocation(location)
                }
            }
        }, 60000, 60000)
    }


    fun toggleFavorite() {
        weatherData.value?.let {
            it.isFavorite = !it.isFavorite

            weatherData.value = it
        }
    }


    override fun onCleared() {
        super.onCleared()

    }
}