package ru.yandex.buggyweatherapp

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import coil.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.buggyweatherapp.domain.model.WeatherData
import ru.yandex.buggyweatherapp.domain.repository.LocationRepository
import ru.yandex.buggyweatherapp.domain.repository.WeatherRepository
import ru.yandex.buggyweatherapp.ui.viewmodel.WeatherViewModel

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
//5. Тестовый файл переименован согласно назначению.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class WeatherViewModelTest {

    @Mock
    private lateinit var mockWeatherRepository: WeatherRepository

    @Mock
    private lateinit var mockLocationRepository: LocationRepository

    @Mock
    private lateinit var mockImageLoader: ImageLoader

    @Mock
    private lateinit var mockContext: Context

    private lateinit var viewModel: WeatherViewModel

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    @Before
    //5. Добавлены Mock-сущности для проведения юнит-тестов
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        viewModel = WeatherViewModel(
            mockWeatherRepository,
            mockLocationRepository,
            mockImageLoader
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    //5. Написан простой юнит-тест вместо бессмысленного 2+2=4
    @Test
    fun toggleFavoriteInvertFavoriteStatus() = runTest {
        // Arrange
        val testWeatherData = createTestWeatherData().apply {
            isFavorite = false
        }
        viewModel.weatherData.value = testWeatherData
        viewModel.toggleFavorite()
        assertEquals(true, viewModel.weatherData.value?.isFavorite)
        viewModel.toggleFavorite()
        assertEquals(false, viewModel.weatherData.value?.isFavorite)
    }
    //5. введение текстового элемента
    private fun createTestWeatherData(): WeatherData {
        return WeatherData(
            cityName = "Moscow",
            country = "RU",
            temperature = 25.5,
            feelsLike = 27.0,
            minTemp = 20.0,
            maxTemp = 30.0,
            humidity = 60,
            pressure = 1013,
            windSpeed = 5.0,
            windDirection = 180,
            description = "clear sky",
            icon = "01d",
            cloudiness = 0,
            sunriseTime = 1234567890L,
            sunsetTime = 1234568900L,
            timezone = 10800,
            timestamp = 1234567000L,
            rawApiData = "{}",
            rain = null,
            snow = null,
            isFavorite = false
        )
    }
}