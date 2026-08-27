package com.trenya.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trenya.app.core.Constants
import com.trenya.app.data.model.FavoriteStation
import com.trenya.app.data.model.PollInterval
import com.trenya.app.data.model.Station
import com.trenya.app.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = Constants.PREFS_NAME)

/**
 * Guarda favoritas y ajustes con DataStore Preferences en vez de una base
 * Room: para el volumen de datos que maneja esta app (una lista corta de
 * favoritas, un puñado de flags) alcanza de sobra, y evita depender de un
 * procesador de anotaciones (KSP) que no podemos verificar que compile en
 * este entorno. Las favoritas se guardan como una lista serializada en JSON
 * bajo una sola clave.
 */
class UserPreferencesRepository(private val context: Context) {

    private val gson = Gson()
    private val favoritesListType = object : TypeToken<List<FavoriteStation>>() {}.type

    private object Keys {
        val FAVORITES = stringPreferencesKey("favorites_json")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val POLL_INTERVAL_MINUTES = longPreferencesKey("poll_interval_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    val favoritesFlow: Flow<List<FavoriteStation>> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.FAVORITES] ?: return@map emptyList()
        runCatching { gson.fromJson<List<FavoriteStation>>(json, favoritesListType) }
            .getOrDefault(emptyList())
    }

    suspend fun toggleFavorite(station: Station) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES]
                ?.let { runCatching { gson.fromJson<List<FavoriteStation>>(it, favoritesListType) }.getOrNull() }
                ?: emptyList()
            val exists = current.any { it.stationId == station.id }
            val updated = if (exists) {
                current.filterNot { it.stationId == station.id }
            } else {
                current + FavoriteStation(stationId = station.id, stationName = station.name)
            }
            prefs[Keys.FAVORITES] = gson.toJson(updated)
        }
    }

    suspend fun setNotifyDelays(stationId: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES]
                ?.let { runCatching { gson.fromJson<List<FavoriteStation>>(it, favoritesListType) }.getOrNull() }
                ?: emptyList()
            val updated = current.map {
                if (it.stationId == stationId) it.copy(notifyDelays = enabled) else it
            }
            prefs[Keys.FAVORITES] = gson.toJson(updated)
        }
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    val pollIntervalFlow: Flow<PollInterval> = context.dataStore.data.map { prefs ->
        val minutes = prefs[Keys.POLL_INTERVAL_MINUTES] ?: PollInterval.THIRTY.minutes
        PollInterval.entries.firstOrNull { it.minutes == minutes } ?: PollInterval.THIRTY
    }

    suspend fun setPollInterval(interval: PollInterval) {
        context.dataStore.edit { it[Keys.POLL_INTERVAL_MINUTES] = interval.minutes }
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.THEME_MODE]
        runCatching { name?.let { ThemeMode.valueOf(it) } }.getOrNull() ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }
}
