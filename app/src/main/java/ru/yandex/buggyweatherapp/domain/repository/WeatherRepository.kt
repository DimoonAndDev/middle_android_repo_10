package ru.yandex.buggyweatherapp.domain.repository

import ru.yandex.buggyweatherapp.domain.model.MyCustomLocation
import ru.yandex.buggyweatherapp.domain.model.WeatherData

interface WeatherRepository {
    fun getWeatherData(location: MyCustomLocation, callback: (WeatherData?, Exception?) -> Unit)
    fun getWeatherByCity(cityName: String, callback: (WeatherData?, Exception?) -> Unit)
}