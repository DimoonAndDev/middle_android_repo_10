package ru.yandex.buggyweatherapp.data.repository

import android.util.Log
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.yandex.buggyweatherapp.data.api.WeatherApiService
import ru.yandex.buggyweatherapp.domain.model.MyCustomLocation
import ru.yandex.buggyweatherapp.domain.model.WeatherData
import ru.yandex.buggyweatherapp.domain.repository.WeatherRepository
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val weatherApi: WeatherApiService
) :
    WeatherRepository {

    private var cachedWeatherData: WeatherData? = null
    private var cacheTimestamp: Long = 0
    private val cacheTTL: Long = 10 * 60 * 1000 // 10 минут

    override suspend fun getWeatherData(location: MyCustomLocation): Result<WeatherData> = withContext(
        Dispatchers.IO) {
        // Актуальности кэша
        if (cachedWeatherData != null && System.currentTimeMillis() - cacheTimestamp < cacheTTL) {
            return@withContext Result.success(cachedWeatherData!!)
        }

        return@withContext try {
            val response = weatherApi.getCurrentWeather(location.latitude, location.longitude).execute()

            if (response.isSuccessful && response.body() != null) {
                val weatherData = parseWeatherData(response.body()!!, location)
                cachedWeatherData = weatherData
                cacheTimestamp = System.currentTimeMillis()
                Result.success(weatherData)
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching weather", e)
            // Возвращаем кэшированные данные, даже если они устарели
            cachedWeatherData?.let { Result.success(it) } ?: Result.failure(e)
        }
    }

    override suspend fun getWeatherByCity(cityName: String): Result<WeatherData> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = weatherApi.getWeatherByCity(cityName).execute()

            if (response.isSuccessful && response.body() != null) {
                val json = response.body()!!
                val location = extractLocationFromResponse(json)
                val weatherData = parseWeatherData(json, location)
                cachedWeatherData = weatherData
                cacheTimestamp = System.currentTimeMillis()
                Result.success(weatherData)
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching weather by city", e)
            cachedWeatherData?.let { Result.success(it) } ?: Result.failure(e)
        }
    }

    override fun getCachedWeatherData(): WeatherData? = cachedWeatherData


    private fun parseWeatherData(json: JsonObject, location: MyCustomLocation): WeatherData {
        val main = json.getAsJsonObject("main")
        val wind = json.getAsJsonObject("wind")
        val sys = json.getAsJsonObject("sys")
        val weatherArray = json.getAsJsonArray("weather")
        val weather = weatherArray?.get(0)?.asJsonObject ?: JsonObject()
        val clouds = json.getAsJsonObject("clouds")
//добавлена "безопасность" в формате элвис-операторов
        return WeatherData(
            cityName = json.get("name")?.asString ?: location.name ?: "Unknown",
            country = sys.get("country")?.asString ?: "",
            temperature = main.get("temp")?.asDouble ?: 0.0,
            feelsLike = main.get("feels_like")?.asDouble ?: 0.0,
            minTemp = main.get("temp_min")?.asDouble ?: 0.0,
            maxTemp = main.get("temp_max")?.asDouble ?: 0.0,
            humidity = main.get("humidity")?.asInt ?: 0,
            pressure = main.get("pressure")?.asInt ?: 0,
            windSpeed = wind.get("speed")?.asDouble ?: 0.0,
            windDirection = wind.get("deg")?.asInt ?: 0,
            description = weather.get("description")?.asString ?: "",
            icon = weather.get("icon")?.asString ?: "",
            cloudiness = clouds.get("all")?.asInt ?: 0,
            sunriseTime = sys.get("sunrise")?.asLong ?: 0L,
            sunsetTime = sys.get("sunset")?.asLong ?: 0L,
            timezone = json.get("timezone")?.asInt ?: 0,
            timestamp = json.get("dt")?.asLong ?: (System.currentTimeMillis() / 1000),
            rawApiData = json.toString(),
            rain = json.getAsJsonObject("rain")?.get("1h")?.asDouble,
            snow = json.getAsJsonObject("snow")?.get("1h")?.asDouble
        )
    }

    private fun extractLocationFromResponse(json: JsonObject): MyCustomLocation {
        val coord = json.getAsJsonObject("coord")
        //тоже элвис-операторы
        val lat = coord.get("lat")?.asDouble ?: 0.0
        val lon = coord.get("lon")?.asDouble ?: 0.0
        val name = json.get("name")?.asString ?: ""

        return MyCustomLocation(lat, lon, name)
    }
}