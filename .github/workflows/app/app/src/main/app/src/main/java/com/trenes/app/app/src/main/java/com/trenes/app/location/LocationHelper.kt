package com.trenes.app.location

import kotlin.math.*

data class EstacionGps(val id: Int, val nombre: String, val lat: Double, val lon: Double)

object LocationHelper {
    // Coordenadas base de estaciones principales
    val estacionesGps = listOf(
        EstacionGps(271, "Ezeiza", -34.8547, -58.5208),
        EstacionGps(267, "Lomas de Zamora", -34.7592, -58.4022),
        EstacionGps(1, "Plaza Constitución", -34.6281, -58.3806),
        EstacionGps(4, "Once", -34.6085, -58.4069),
        EstacionGps(2, "Retiro", -34.5912, -58.3746)
    )

    fun encontrarEstacionMasCercana(latUsuario: Double, lonUsuario: Double): EstacionGps? {
        return estacionesGps.minByOrNull { estacion ->
            calcularDistanciaMetros(latUsuario, lonUsuario, estacion.lat, estacion.lon)
        }
    }

    private fun calcularDistanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Radio de la Tierra en metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
