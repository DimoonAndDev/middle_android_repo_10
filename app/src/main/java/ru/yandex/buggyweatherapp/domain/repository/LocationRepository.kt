package ru.yandex.buggyweatherapp.domain.repository

import ru.yandex.buggyweatherapp.domain.model.Location

interface LocationRepository {
    fun getCurrentLocation(callback: (Location?) -> Unit)
    fun getCityNameFromLocation(location: Location): String?

}