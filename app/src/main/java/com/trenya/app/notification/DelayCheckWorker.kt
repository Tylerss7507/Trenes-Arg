package com.trenya.app.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trenya.app.R
import com.trenya.app.TrenYaApplication
import com.trenya.app.core.Constants
import com.trenya.app.data.model.DataResult
import com.trenya.app.data.model.TrainStatus
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * TrenYa no tiene backend propio, así que las notificaciones de demora no son
 * push real: este worker se despierta periódicamente (mínimo 15 minutos, es
 * la cota que impone WorkManager para trabajo periódico) y consulta los
 * arribos de cada estación favorita. Si el estado de un servicio cambió a
 * algo distinto de "normal" desde la última corrida, dispara una
 * notificación local. El "último estado visto" se guarda en un JSON simple
 * en disco para no re-notificar la misma demora en cada corrida.
 */
class DelayCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val gson = Gson()

    override suspend fun doWork(): Result {
        val container = (applicationContext as TrenYaApplication).container
        val prefs = container.userPreferencesRepository

        val notificationsEnabled = prefs.notificationsEnabledFlow.first()
        if (!notificationsEnabled) return Result.success()

        val favoritesToWatch = prefs.favoritesFlow.first().filter { it.notifyDelays }
        if (favoritesToWatch.isEmpty()) return Result.success()

        val previousSnapshot = readSnapshot()
        val newSnapshot = mutableMapOf<String, String>()

        for (favorite in favoritesToWatch) {
            val result = container.trainRepository.getUpcomingTrains(favorite.stationId, cantidad = 5)
            val trains = (result as? DataResult.Success)?.data ?: continue

            for (train in trains) {
                val key = "${favorite.stationId}:${train.serviceNumber}"
                newSnapshot[key] = train.status.name

                val wasAlreadyAlerted = previousSnapshot[key] == train.status.name
                val isNoteworthy = train.status != TrainStatus.NORMAL
                if (isNoteworthy && !wasAlreadyAlerted) {
                    val message = train.statusMessage?.takeIf { it.isNotBlank() }
                        ?: fallbackMessage(train.status)
                    container.notificationHelper.notifyDelay(favorite.stationName, message, key.hashCode())
                }
            }
        }

        writeSnapshot(newSnapshot)
        return Result.success()
    }

    private fun fallbackMessage(status: TrainStatus): String = when (status) {
        TrainStatus.CANCELLED -> applicationContext.getString(R.string.status_cancelled)
        TrainStatus.DELAYED -> applicationContext.getString(R.string.status_delayed)
        TrainStatus.ALTERED -> applicationContext.getString(R.string.status_altered)
        else -> applicationContext.getString(R.string.status_unknown)
    }

    private fun readSnapshot(): Map<String, String> {
        val file = File(applicationContext.filesDir, Constants.ARRIVALS_SNAPSHOT_FILE)
        if (!file.exists()) return emptyMap()
        return runCatching {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(file.readText(), type)
        }.getOrDefault(emptyMap())
    }

    private fun writeSnapshot(snapshot: Map<String, String>) {
        runCatching {
            File(applicationContext.filesDir, Constants.ARRIVALS_SNAPSHOT_FILE).writeText(gson.toJson(snapshot))
        }
    }

    companion object {
        /** Programa (o reprograma con un nuevo intervalo) el chequeo periódico de demoras. */
        fun schedule(context: Context, intervalMinutes: Long) {
            val safeInterval = intervalMinutes.coerceAtLeast(Constants.MIN_POLL_INTERVAL_MINUTES)
            val request = PeriodicWorkRequestBuilder<DelayCheckWorker>(safeInterval, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.DELAY_CHECK_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
