package com.trenya.app

import android.app.Application
import com.trenya.app.core.AppContainer
import com.trenya.app.core.Constants
import com.trenya.app.notification.DelayCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TrenYaApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Deja el worker de demoras programado desde el arranque, con el
        // intervalo que el usuario haya elegido (o el default si es la
        // primera vez que abre la app).
        CoroutineScope(Dispatchers.IO).launch {
            val intervalMinutes = runCatching {
                container.userPreferencesRepository.pollIntervalFlow.first().minutes
            }.getOrDefault(Constants.MIN_POLL_INTERVAL_MINUTES * 2)
            DelayCheckWorker.schedule(this@TrenYaApplication, intervalMinutes)
        }
    }
}
