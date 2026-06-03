package com.downme.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.downme.app.util.YtDlpFormats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val KEY_QUALITY = stringPreferencesKey("default_quality")
private val KEY_DOWNLOAD_LOCATION = stringPreferencesKey("download_location")
private val KEY_THEME = stringPreferencesKey("theme_mode")
private val KEY_SHOW_QUALITY_PROMPT = booleanPreferencesKey("show_quality_prompt")

class UserPreferencesRepository(private val ctx: Context) {

    private val dataStore = ctx.dataStore

    val defaultQuality: Flow<String> =
        dataStore.data.map { prefs ->
            prefs[KEY_QUALITY] ?: "1080"
        }

    val downloadLocation: Flow<DownloadSaveLocation> =
        dataStore.data.map { prefs ->
            DownloadSaveLocation.fromId(prefs[KEY_DOWNLOAD_LOCATION])
        }

    val themeMode: Flow<AppThemeMode> =
        dataStore.data.map { prefs ->
            AppThemeMode.fromId(prefs[KEY_THEME])
        }

    val showQualityPrompt: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[KEY_SHOW_QUALITY_PROMPT] ?: true
        }

    suspend fun setDefaultQuality(value: String) {
        dataStore.edit { it[KEY_QUALITY] = YtDlpFormats.normalizeQuality(value) }
    }

    suspend fun setDownloadLocation(location: DownloadSaveLocation) {
        dataStore.edit { it[KEY_DOWNLOAD_LOCATION] = location.id }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.id }
    }

    suspend fun setShowQualityPrompt(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_QUALITY_PROMPT] = show }
    }
}
