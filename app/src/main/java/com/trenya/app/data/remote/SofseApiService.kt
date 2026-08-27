package com.trenya.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Cliente de la API pública de arribos/estaciones (ver README para el origen
 * y las salvedades de esta fuente de datos).
 *
 * Base URL configurada en BuildConfig.API_BASE_URL (app/build.gradle.kts),
 * para que cambiarla -a una instancia propia, por ejemplo- sea de una línea.
 */
interface SofseApiService {

    @GET("infraestructura/estaciones")
    suspend fun buscarEstaciones(
        @Query("nombre") nombre: String? = null,
        @Query("idRamal") idRamal: Int? = null
    ): List<EstacionDto>

    @GET("arribos/estacion/{id}")
    suspend fun arribosEstacion(
        @Path("id") idEstacion: String,
        @Query("hasta") hasta: String? = null,
        @Query("fecha") fecha: String? = null,
        @Query("hora") hora: String? = null,
        @Query("cantidad") cantidad: Int? = 8,
        @Query("ramal") ramal: Int? = null,
        @Query("sentido") sentido: Int? = null
    ): ArribosResponseDto
}
