package com.trenya.app.core

object Constants {
    const val PREFS_NAME = "trenya_prefs"
    const val STATIONS_CACHE_FILE = "stations_cache.json"
    const val ARRIVALS_SNAPSHOT_FILE = "arrivals_snapshot.json"

    /** Cuántos días dejamos pasar antes de refrescar la lista completa de estaciones. */
    const val STATIONS_CACHE_MAX_AGE_DAYS = 7L

    /** Cuántas estaciones cercanas mostramos como máximo en Inicio. */
    const val NEARBY_STATIONS_LIMIT = 6

    /** Radio de búsqueda de estaciones cercanas, en metros. Más allá de esto,
     *  probablemente no sea una estación "caminable". */
    const val NEARBY_STATIONS_RADIUS_METERS = 15_000.0

    const val DEFAULT_ARRIVALS_COUNT = 8

    /** WorkManager no permite intervalos periódicos menores a 15 minutos. */
    const val MIN_POLL_INTERVAL_MINUTES = 15L

    const val DELAY_CHECK_WORK_NAME = "trenya_delay_check"

    const val NOTIF_CHANNEL_DELAYS = "trenya_delays"
    const val NOTIF_CHANNEL_GENERAL = "trenya_general"

    const val LINK_TRENES_ARGENTINOS = "https://www.trenesargentinos.gob.ar/"
    const val LINK_API_SOURCE = "https://github.com/ariedro/api-trenes"
}
