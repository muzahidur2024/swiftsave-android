package com.swiftsave.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val KEY_QUALITY = stringPreferencesKey("default_quality")

class UserPreferencesRepository(private val ctx: Context) {

    private val dataStore = ctx.dataStore

    val defaultQuality: Flow<String> =
        dataStore.data.map { prefs ->
            prefs[KEY_QUALITY] ?: "1080"
        }

    suspend fun setDefaultQuality(value: String) {
        dataStore.edit { it[KEY_QUALITY] = value }
    }
}
