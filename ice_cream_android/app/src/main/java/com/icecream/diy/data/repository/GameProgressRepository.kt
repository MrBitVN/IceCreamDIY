package com.icecream.diy.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ice_cream_prefs")

interface GameProgressRepository {
    fun getLevel(): Flow<Int>
    suspend fun saveLevel(level: Int)
    fun getCoins(): Flow<Int>
    suspend fun saveCoins(coins: Int)
    fun getSoundEnabled(): Flow<Boolean>
    suspend fun saveSoundEnabled(enabled: Boolean)
    fun getHapticsEnabled(): Flow<Boolean>
    suspend fun saveHapticsEnabled(enabled: Boolean)
    suspend fun resetProgress()
}

class DataStoreProgressRepository(private val context: Context) : GameProgressRepository {
    companion object {
        val KEY_LEVEL = intPreferencesKey("ice_cream_saved_level")
        val KEY_COINS = intPreferencesKey("ice_cream_saved_coins")
        val KEY_SOUND = androidx.datastore.preferences.core.booleanPreferencesKey("ice_cream_sound_enabled")
        val KEY_HAPTICS = androidx.datastore.preferences.core.booleanPreferencesKey("ice_cream_haptics_enabled")
    }

    override fun getLevel(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_LEVEL] ?: 1
    }

    override suspend fun saveLevel(level: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LEVEL] = level
        }
    }

    override fun getCoins(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_COINS] ?: 250
    }

    override suspend fun saveCoins(coins: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COINS] = coins
        }
    }

    override fun getSoundEnabled(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOUND] ?: true
    }

    override suspend fun saveSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SOUND] = enabled
        }
    }

    override fun getHapticsEnabled(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAPTICS] ?: true
    }

    override suspend fun saveHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAPTICS] = enabled
        }
    }

    override suspend fun resetProgress() {
        context.dataStore.edit { prefs ->
            prefs[KEY_LEVEL] = 1
            prefs[KEY_COINS] = 250
        }
    }
}
