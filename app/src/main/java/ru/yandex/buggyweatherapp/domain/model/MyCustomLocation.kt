package ru.yandex.buggyweatherapp.domain.model
//2. Вынесено в папку domain. логика Clean Architecture
data class MyCustomLocation(
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
        if (other !is MyCustomLocation) return false
        return latitude == other.latitude && longitude == other.longitude
    }
}