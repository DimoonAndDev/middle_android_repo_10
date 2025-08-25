package ru.yandex.buggyweatherapp

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import ru.yandex.buggyweatherapp.utils.LocationTracker
//3. указана анотация Хилт, для введения правильного DI
//4. Удалена небезопасная инициация через файл ImageLoader.kt
@HiltAndroidApp
class WeatherApplication : Application() {
    
    
    companion object {
        lateinit var appContext: Context
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        
        
        appContext = this

        LocationTracker.getInstance(this)
    }
}