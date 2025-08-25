package ru.yandex.buggyweatherapp.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.yandex.buggyweatherapp.domain.model.MyCustomLocation
import ru.yandex.buggyweatherapp.domain.model.WeatherData
import ru.yandex.buggyweatherapp.domain.repository.LocationRepository
import ru.yandex.buggyweatherapp.domain.repository.WeatherRepository
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

//2. вынесено в папку UI. логика Clean Architecture
//3. Зависимости через HILT
//8. исправлены ошибки: использование встроенных корутин вместо кстомного скоуп и коллбеков,
// добавлена очистка ресурсов для избежания утечек памяти, добавлены константы.
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val imageLoader: ImageLoader
) : ViewModel() { //8. использование Hilt DI
    //8. Введены константы для ошибок
    companion object {
        private const val AUTO_REFRESH_DELAY = 60000L
        private const val ERROR_LOCATION = "Unable to get current location"
        private const val ERROR_CITY_NAME = "Error getting city name: "
        private const val ERROR_WEATHER = "Error fetching weather: "
        private const val ERROR_SEARCH = "Error searching weather: "
        private const val ERROR_EMPTY_CITY = "City name cannot be empty"
    }

    private lateinit var activityContext: Context

    //8. добавлено начальное значение
    val weatherData = MutableLiveData<WeatherData?>()
    val currentLocation = MutableLiveData<MyCustomLocation?>()
    val isLoading = MutableLiveData<Boolean>(false)
    val error = MutableLiveData<String?>()
    val cityName = MutableLiveData<String?>()

    //8. stateflow для автоматического обновления иконки
    private val _weatherIcon = MutableStateFlow<Bitmap?>(null)
    val weatherIcon: StateFlow<Bitmap?> = _weatherIcon.asStateFlow()

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
                //8. вместо кастомного корутина использован встроенный
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

    //8. метод исправлен на работу через корутины, не черезколбеки, чтобы избежать callback hell
    fun getWeatherForLocation(location: MyCustomLocation) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    weatherRepository.getWeatherData(location)
                }

                isLoading.value = false
                //8. используется result и when вместо прямой проверки data, лучшая практика
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
//8.добавлена корутина, чтобы не держать главный поток
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
                    //8. доп. проверка null
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
    //8. использована корутина с нужным сипатчером вместо кастом скуоп
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
        //8. использование апплай безопаснее
        refreshTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    currentLocation.value?.let { location ->
                        getWeatherForLocation(location)
                    }
                }
                //8. использование констант
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
        //8. оичтка ресурсов не пустая
        super.onCleared()
        refreshTimer?.cancel()
        refreshTimer = null
        locationRepository.cleanin()
    }
}