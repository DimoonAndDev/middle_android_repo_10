package ru.yandex.buggyweatherapp.domain.repository

import ru.yandex.buggyweatherapp.domain.model.Location
import ru.yandex.buggyweatherapp.domain.model.WeatherData

interface WeatherRepository {
    fun getWeatherData(location: Location, callback: (WeatherData?, Exception?) -> Unit)
    fun getWeatherByCity(cityName: String, callback: (WeatherData?, Exception?) -> Unit)
}