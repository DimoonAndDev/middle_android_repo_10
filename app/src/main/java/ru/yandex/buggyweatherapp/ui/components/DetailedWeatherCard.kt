package ru.yandex.buggyweatherapp.ui.components

import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.yandex.buggyweatherapp.domain.model.WeatherData
import ru.yandex.buggyweatherapp.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailedWeatherCard(weather: WeatherData) {
    //3. получение viewmodel через Hilt
    //9. переименован класс, чтобы не путать со встренным в WeatherScreen - улучшение понятности кода,
    //использование компонентов Compose вместо общих AndroidView, иконка через StateFlow из viewModel,
    //разделение data и ui через viewmodel и coil
    val weatherViewModel = hiltViewModel<WeatherViewModel>()
    val weatherIcon by weatherViewModel.weatherIcon.collectAsState() //9. иконка достается и обновляется из viewmodel для однозначности источника
    var currentIconUrl by remember { mutableStateOf("") } //9. отслеживание урл, чтобы обновлять иконку, только когда url обновился - экономия ресурсов

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weather.cityName,
                    style = MaterialTheme.typography.headlineMedium
                )

                IconButton(onClick = { weatherViewModel.toggleFavorite() }) {
                    Icon(
                        imageVector = if (weather.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = (if (weather.isFavorite) Color.RED else MaterialTheme.colorScheme.onSurface) as androidx.compose.ui.graphics.Color
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                val iconUrl = "https://openweathermap.org/img/wn/${weather.icon}@2x.png"

                // Загружаем иконку только если URL изменился
                LaunchedEffect(iconUrl) {
                    if (currentIconUrl != iconUrl) {
                        currentIconUrl = iconUrl
                        //9. загрузка не через встроенный ImageLoader, а вьюмодел - разделение DATA и UI
                        weatherViewModel.loadWeatherIcon(iconUrl)
                    }
                }

                if (weatherIcon != null) {
                    Image(//9. использование стандартного Image от Compose
                        bitmap = weatherIcon!!.asImageBitmap(),
                        contentDescription = "Weather icon",
                        modifier = Modifier.size(50.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Placeholder пока иконка загружается
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(androidx.compose.ui.graphics.Color(Color.LTGRAY), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Loading",
                            modifier = Modifier.align(Alignment.Center),
                            tint = androidx.compose.ui.graphics.Color(Color.WHITE)
                        )
                    }
                }

                Text(
                    text = "${weather.temperature.toInt()}°C",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Text(
                text = weather.description.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                item {
                    WeatherDataRow("Feels like", "${weather.feelsLike.toInt()}°C")
                }
                item {
                    WeatherDataRow("Min/Max", "${weather.minTemp.toInt()}°C / ${weather.maxTemp.toInt()}°C")
                }
                item {
                    WeatherDataRow("Humidity", "${weather.humidity}%")
                }
                item {
                    WeatherDataRow("Pressure", "${weather.pressure} hPa")
                }
                item {
                    WeatherDataRow("Wind", "${weather.windSpeed} m/s")
                }
                item {
                    WeatherDataRow("Sunrise", formatTimestamp(weather.sunriseTime))
                }
                item {
                    WeatherDataRow("Sunset", formatTimestamp(weather.sunsetTime))
                }
            }
        }
    }
}

@Composable
private fun WeatherDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {//9. стилистическое выделение текста
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}//9. убран Disposable - Coil делает все сам.

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return format.format(date)
}