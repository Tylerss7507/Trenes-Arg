package com.trenya.app.data.model

/** Una estación de la red, ya con tipos limpios (lat/long como Double, etc). */
data class Station(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val branchIds: List<Int>,
    val platformCount: Int?
)

/** Una estación con su distancia calculada al usuario. */
data class NearbyStation(
    val station: Station,
    val distanceMeters: Double
)

enum class TrainStatus { NORMAL, DELAYED, ALTERED, CANCELLED, UNKNOWN }

/** Una parada intermedia dentro del recorrido completo de un servicio. */
data class RouteStop(
    val stationId: Int?,
    val name: String,
    val scheduledArrival: String?,
    val scheduledDeparture: String?
)

/** Un próximo tren para una estación (resultado ya "aplanado" de la API). */
data class UpcomingTrain(
    val serviceNumber: String,
    val lineId: Int?,
    val lineName: String,
    val branchId: Int?,
    val branchName: String,
    val branchAbbreviation: String?,
    val originName: String,
    val destinationName: String,
    val scheduledArrivalIso: String?,
    val secondsRemaining: Long?,
    val platform: String?,
    val direction: Int?,
    val status: TrainStatus,
    val statusMessage: String?,
    val fullRoute: List<RouteStop>
)

/** Línea conocida (gerencia), para la pantalla de líneas. */
data class LineOverview(
    val id: Int?,
    val name: String,
    val knownBranches: List<String> = emptyList()
)

/** Estación marcada como favorita, con su preferencia de notificaciones. */
data class FavoriteStation(
    val stationId: String,
    val stationName: String,
    val notifyDelays: Boolean = true
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class PollInterval(val minutes: Long, val label: String) {
    FIFTEEN(15, "15 min"),
    THIRTY(30, "30 min"),
    SIXTY(60, "60 min")
}

/** Resultado genérico para separar éxito / error-con-caché / error sin datos. */
sealed class DataResult<out T> {
    data class Success<T>(val data: T, val isFromCache: Boolean = false, val fetchedAtMillis: Long? = null) : DataResult<T>()
    data class Error(val message: String, val cachedData: Any? = null) : DataResult<Nothing>()
    object Loading : DataResult<Nothing>()
}
