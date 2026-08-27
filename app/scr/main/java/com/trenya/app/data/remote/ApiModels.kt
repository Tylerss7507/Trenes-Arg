package com.trenya.app.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Estos modelos reflejan el JSON devuelto por la API pública espejo de SOFSE
 * (ver README). Fueron chequeados contra respuestas reales de:
 *   GET /infraestructura/estaciones?nombre=Migue
 *   GET /arribos/estacion/271?hasta=236&fecha=...&hora=...&cantidad=3
 *
 * Todos los campos son nullable a propósito: la API no publica un contrato
 * formal (es un proxy comunitario de una API interna no documentada
 * oficialmente), así que preferimos degradar con gracia a mostrar "sin dato"
 * antes que romper el parseo si un campo falta en algún caso no observado.
 */

// ---------- GET /infraestructura/estaciones ----------

data class EstacionDto(
    val nombre: String?,
    @SerializedName("id_estacion") val idEstacion: String?,
    @SerializedName("id_tramo") val idTramo: String?,
    val orden: String?,
    val latitud: String?,
    val longitud: String?,
    @SerializedName("andenes_habilitados") val andenesHabilitados: String?,
    val visibilidad: VisibilidadDto?,
    @SerializedName("incluida_en_ramales") val incluidaEnRamales: List<Int>?,
    @SerializedName("operativa_en_ramales") val operativaEnRamales: List<Int>?
)

data class VisibilidadDto(
    val totem: Int?,
    @SerializedName("app_mobile") val appMobile: Int?
)

// ---------- GET /arribos/estacion/{id} ----------

data class ArribosResponseDto(
    val timestamp: Long?,
    val results: List<ArriboResultDto>?,
    val total: Int?
)

data class ArriboResultDto(
    val arribo: ArriboDto?,
    val servicio: ServicioDto?
)

data class ArriboDto(
    val idElemento: Int?,
    val orden: Int?,
    val nombre: String?,
    val tipo: TipoDto?,
    val parada: Boolean?,
    val anden: AndenDto?,
    val llegada: TiempoDto?,
    val salida: TiempoDto?,
    // Segundos restantes hasta la llegada, relativo al instante de la consulta.
    // Es lo más parecido a "en vivo" que expone la API: no hay un campo
    // separado de hora real vs. programada, así que lo tratamos como la
    // mejor estimación disponible del "próximo tren en X minutos".
    val segundos: Long?
)

data class TipoDto(val id: Int?, val nombre: String?)
data class AndenDto(val id: Int?, val nombre: String?)

data class TiempoDto(
    val programada: String?,
    // No observados en ninguna respuesta real durante el desarrollo, pero se
    // dejan mapeados por si la API los agrega para servicios en curso.
    val real: String? = null,
    val estimada: String? = null
)

data class ServicioDto(
    val numero: Int?,
    val horaSalida: HoraSalidaDto?,
    val sentido: Int?,
    val gerencia: GerenciaDto?,
    val ramal: RamalDto?,
    val tipo: TipoDto?,
    val desde: EstacionRutaDto?,
    val hasta: EstacionRutaDto?,
    val estaciones: List<EstacionRutaDto>?,
    val oculto: Boolean?,
    // Texto libre con la alerta del servicio (demora, alteración, etc.) cuando
    // existe. Es el campo clave para la función de notificaciones por demoras.
    val leyenda: String?
)

data class HoraSalidaDto(val hours: Int?, val minutes: Int?)
data class GerenciaDto(val id: Int?, val nombre: String?)

data class RamalDto(
    val id: Int?,
    val nombre: String?,
    val siglas: String?,
    val cabeceraInicial: CabeceraDto?,
    val cabeceraFinal: CabeceraDto?
)

data class CabeceraDto(
    val id: Int?,
    val nombre: String?,
    @SerializedName("nombre_corto") val nombreCorto: String?
)

data class EstacionRutaDto(
    val idElemento: Int?,
    val orden: Int?,
    val nombre: String?,
    val parada: Boolean?,
    val anden: AndenDto?,
    val llegada: TiempoDto?,
    val salida: TiempoDto?
)
