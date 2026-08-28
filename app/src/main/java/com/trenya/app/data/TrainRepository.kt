package com.trenya.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trenya.app.core.Constants
import com.trenya.app.core.DistanceUtils
import com.trenya.app.data.model.DataResult
import com.trenya.app.data.model.NearbyStation
import com.trenya.app.data.model.RouteStop
import com.trenya.app.data.model.Station
import com.trenya.app.data.model.TrainStatus
import com.trenya.app.data.model.UpcomingTrain
import com.trenya.app.data.remote.ArriboResultDto
import com.trenya.app.data.remote.EstacionDto
import com.trenya.app.data.remote.SofseApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class TrainRepository(
    private val api: SofseApiService,
    private val context: Context
) {
    private val gson = Gson()
    private var inMemoryStations: List<Station>? = null

    /** Usadas para reconstruir la lista completa de estaciones (ver getAllStations). */
    private val ALPHABET_PROBES = listOf("a", "e", "i", "o", "u")

    // ---------------------------------------------------------------------
    // Estaciones
    // ---------------------------------------------------------------------

    /**
     * Devuelve la lista completa de estaciones. Prioriza memoria, después
     * caché en disco (si no está vencida), y por último la red. Si la red
     * falla, devuelve lo último cacheado en vez de una lista vacía: la lista
     * de estaciones cambia muy poco, así que datos "viejos" siguen siendo
     * útiles para búsqueda y cercanía.
     *
     * En vez de un único pedido sin filtro (que es justamente el que no
     * andaba: la API lo maneja distinto a como documenta), reconstruimos la
     * lista combinando varias búsquedas livianas por texto -una por cada
     * vocal-, que es el mismo mecanismo ya verificado que usa searchStations().
     * Prácticamente cualquier nombre de estación en español tiene alguna
     * vocal, así que la unión de las cinco cubre el universo real.
     */
    suspend fun getAllStations(forceRefresh: Boolean = false): List<Station> = withContext(Dispatchers.IO) {
        inMemoryStations?.let { if (!forceRefresh) return@withContext it }

        val disk = readStationsCache()
        val isFresh = disk != null &&
            (System.currentTimeMillis() - disk.savedAtMillis) < TimeUnit.DAYS.toMillis(Constants.STATIONS_CACHE_MAX_AGE_DAYS)

        if (!forceRefresh && isFresh) {
            inMemoryStations = disk!!.stations
            return@withContext disk.stations
        }

        try {
            val merged = LinkedHashMap<String, Station>()
            val results = ALPHABET_PROBES.map { letter ->
                async {
                    try {
                        api.buscarEstaciones(nombre = letter).mapNotNull { it.toDomain() }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll()
            results.forEach { batch -> batch.forEach { merged[it.id] = it } }

            val stations = merged.values.toList()
            if (stations.isNotEmpty()) {
                inMemoryStations = stations
                writeStationsCache(stations)
                stations
            } else {
                disk?.stations ?: emptyList()
            }
        } catch (e: Exception) {
            disk?.stations ?: inMemoryStations ?: emptyList()
        }
    }

    /**
     * Busca estaciones por nombre directamente contra la API (parámetro
     * `nombre`), en vez de traer la lista completa y filtrar en el
     * dispositivo. Esto es lo que hace posible el autocompletado: cada
     * letra dispara una consulta liviana y en vivo al servidor, en vez de
     * depender de haber podido descargar antes las ~300 estaciones enteras.
     *
     * De paso, guarda en la caché en memoria cada estación que aparece en
     * un resultado, para que getStationById() pueda encontrarla más tarde
     * sin necesitar getAllStations().
     */
    suspend fun searchStations(query: String): DataResult<List<Station>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext DataResult.Success(emptyList())
        try {
            val stations = api.buscarEstaciones(nombre = query.trim())
                .mapNotNull { it.toDomain() }
                .sortedBy { it.name }
            mergeIntoMemoryCache(stations)
            DataResult.Success(stations)
        } catch (e: Exception) {
            DataResult.Error(e.message ?: "No pudimos conectar con el buscador de estaciones")
        }
    }

    private fun mergeIntoMemoryCache(stations: List<Station>) {
        if (stations.isEmpty()) return
        val merged = inMemoryStations.orEmpty().associateBy { it.id }.toMutableMap()
        stations.forEach { merged[it.id] = it }
        inMemoryStations = merged.values.toList()
    }

    suspend fun getStationById(id: String): Station? =
        getAllStations().firstOrNull { it.id == id }

    fun findNearbyStations(
        latitude: Double,
        longitude: Double,
        stations: List<Station>,
        limit: Int = Constants.NEARBY_STATIONS_LIMIT
    ): List<NearbyStation> = stations
        .map { NearbyStation(it, DistanceUtils.metersBetween(latitude, longitude, it.latitude, it.longitude)) }
        .filter { it.distanceMeters <= Constants.NEARBY_STATIONS_RADIUS_METERS }
        .sortedBy { it.distanceMeters }
        .take(limit)

    // ---------------------------------------------------------------------
    // Arribos
    // ---------------------------------------------------------------------

    suspend fun getUpcomingTrains(
        stationId: String,
        destinationStationId: String? = null,
        cantidad: Int = Constants.DEFAULT_ARRIVALS_COUNT,
        ramal: Int? = null
    ): DataResult<List<UpcomingTrain>> = withContext(Dispatchers.IO) {
        try {
            val response = api.arribosEstacion(
                idEstacion = stationId,
                hasta = destinationStationId,
                cantidad = cantidad,
                ramal = ramal
            )
            val trains = response.results.orEmpty().mapNotNull { it.toDomain() }
            DataResult.Success(trains, fetchedAtMillis = System.currentTimeMillis())
        } catch (e: Exception) {
            DataResult.Error(e.message ?: "No se pudo conectar con el servicio de arribos")
        }
    }

    // ---------------------------------------------------------------------
    // Caché en disco (JSON plano, sin Room)
    // ---------------------------------------------------------------------

    private data class StationsCache(val savedAtMillis: Long, val stations: List<Station>)

    private fun readStationsCache(): StationsCache? {
        val file = File(context.filesDir, Constants.STATIONS_CACHE_FILE)
        if (!file.exists()) return null
        return runCatching {
            val type = object : TypeToken<StationsCache>() {}.type
            gson.fromJson<StationsCache>(file.readText(), type)
        }.getOrNull()
    }

    private fun writeStationsCache(stations: List<Station>) {
        runCatching {
            val cache = StationsCache(System.currentTimeMillis(), stations)
            File(context.filesDir, Constants.STATIONS_CACHE_FILE).writeText(gson.toJson(cache))
        }
    }

    // ---------------------------------------------------------------------
    // Mapeo DTO -> dominio
    // ---------------------------------------------------------------------

    private fun EstacionDto.toDomain(): Station? {
        val id = idEstacion ?: return null
        val lat = latitud?.toDoubleOrNull() ?: return null
        val lon = longitud?.toDoubleOrNull() ?: return null
        return Station(
            id = id,
            name = nombre?.trim().orEmpty().ifBlank { "Estación $id" },
            latitude = lat,
            longitude = lon,
            branchIds = (operativaEnRamales ?: incluidaEnRamales).orEmpty(),
            platformCount = andenesHabilitados?.toIntOrNull()
        )
    }

    private fun ArriboResultDto.toDomain(): UpcomingTrain? {
        val servicio = servicio ?: return null
        val arribo = arribo
        val status = resolveStatus(servicio.tipo?.nombre, servicio.leyenda, servicio.oculto)
        return UpcomingTrain(
            serviceNumber = servicio.numero?.toString() ?: "—",
            lineId = servicio.gerencia?.id,
            lineName = servicio.gerencia?.nombre ?: "—",
            branchId = servicio.ramal?.id,
            branchName = servicio.ramal?.nombre ?: "—",
            branchAbbreviation = servicio.ramal?.siglas,
            originName = servicio.desde?.nombre ?: servicio.ramal?.cabeceraInicial?.nombre ?: "—",
            destinationName = servicio.hasta?.nombre ?: servicio.ramal?.cabeceraFinal?.nombre ?: "—",
            scheduledArrivalIso = arribo?.llegada?.real ?: arribo?.llegada?.estimada ?: arribo?.llegada?.programada,
            secondsRemaining = arribo?.segundos,
            platform = arribo?.anden?.nombre,
            direction = servicio.sentido,
            status = status,
            statusMessage = servicio.leyenda?.trim()?.takeIf { it.isNotBlank() },
            fullRoute = servicio.estaciones.orEmpty().map {
                RouteStop(
                    stationId = it.idElemento,
                    name = it.nombre ?: "—",
                    scheduledArrival = it.llegada?.programada,
                    scheduledDeparture = it.salida?.programada
                )
            }
        )
    }

    /**
     * La API no documenta valores posibles de `tipo`/`leyenda` para servicios
     * demorados o cancelados (solo se observó el caso "Normal" sin leyenda en
     * las pruebas hechas para este proyecto). Esta heurística es la mejor
     * inferencia razonable a partir de esos dos campos y debería revisarse
     * apenas se observe un caso real de demora para ajustar el mapeo.
     */
    private fun resolveStatus(tipoNombre: String?, leyenda: String?, oculto: Boolean?): TrainStatus {
        if (oculto == true) return TrainStatus.CANCELLED
        if (!leyenda.isNullOrBlank()) return TrainStatus.DELAYED
        return when (tipoNombre?.trim()?.lowercase()) {
            "normal", null -> TrainStatus.NORMAL
            "cancelado", "suspendido" -> TrainStatus.CANCELLED
            else -> TrainStatus.ALTERED
        }
    }
}
