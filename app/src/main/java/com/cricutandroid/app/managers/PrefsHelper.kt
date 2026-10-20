package com.cricutandroid.app.managers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Android equivalent of iOS KeychainHelper + UserDefaults.
 * Uses DataStore Preferences for persistent storage.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

object PrefsHelper {

    private lateinit var context: Context

    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    fun saveBool(key: String, value: Boolean) {
        runBlocking {
            context.dataStore.edit { it[booleanPreferencesKey(key)] = value }
        }
    }

    fun getBool(key: String): Boolean = runBlocking {
        context.dataStore.data.map { it[booleanPreferencesKey(key)] ?: false }.first()
    }

    fun saveString(key: String, value: String) {
        runBlocking {
            context.dataStore.edit { it[stringPreferencesKey(key)] = value }
        }
    }

    fun getString(key: String): String? = runBlocking {
        context.dataStore.data.map { it[stringPreferencesKey(key)] }.first()
    }

    suspend fun saveBoolAsync(key: String, value: Boolean) {
        context.dataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    suspend fun getBoolAsync(key: String): Boolean {
        return context.dataStore.data.map { it[booleanPreferencesKey(key)] ?: false }.first()
    }
}
