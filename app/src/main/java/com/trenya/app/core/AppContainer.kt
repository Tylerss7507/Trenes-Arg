package com.trenya.app.core

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.trenya.app.BuildConfig
import com.trenya.app.data.TrainRepository
import com.trenya.app.data.UserPreferencesRepository
import com.trenya.app.data.remote.SofseApiService
import com.trenya.app.location.LocationTracker
import com.trenya.app.notification.NotificationHelper
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * DI manual y liviano: no usamos Hilt para no depender de un procesador de
 * anotaciones (KSP) cuya compatibilidad de versiones no podemos verificar en
 * este entorno de desarrollo. Cada dependencia se crea una sola vez (lazy) y
 * se comparte desde [com.trenya.app.TrenYaApplication].
 */
class AppContainer(private val appContext: Context) {

    private val gson: Gson by lazy { GsonBuilder().create() }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
                }
            )
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private val api: SofseApiService by lazy { retrofit.create(SofseApiService::class.java) }

    val trainRepository: TrainRepository by lazy { TrainRepository(api, appContext) }
    val userPreferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(appContext) }
    val locationTracker: LocationTracker by lazy { LocationTracker(appContext) }
    val notificationHelper: NotificationHelper by lazy { NotificationHelper(appContext) }
}
