package ru.yandex.buggyweatherapp.domain.repository

import ru.yandex.buggyweatherapp.domain.model.MyCustomLocation

interface LocationRepository {
    fun getCurrentLocation(callback: (MyCustomLocation?) -> Unit)
    suspend fun getCityNameFromLocation(location: MyCustomLocation): String?
    fun cleanin()
}