package ru.yandex.buggyweatherapp.domain.model

data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null
) {
    
    override fun toString(): String {
        var result = ""
        result += "Latitude: $latitude, "
        result += "Longitude: $longitude"
        name?.let {
            result += ", Name: $it"
        }
        return result
    }
    
    
    override fun equals(other: Any?): Boolean {
        if (other !is Location) return false
        return latitude == other.latitude && longitude == other.longitude
    }
}