package ru.yandex.buggyweatherapp.domain.repository

import ru.yandex.buggyweatherapp.domain.model.MyCustomLocation
import ru.yandex.buggyweatherapp.domain.model.WeatherData
//2. Вынесено в папку domain. логика Clean Architecture
interface WeatherRepository {
    suspend fun getWeatherData(location: MyCustomLocation): Result<WeatherData>
    suspend fun getWeatherByCity(cityName: String): Result<WeatherData>
    fun getCachedWeatherData(): WeatherData?
}