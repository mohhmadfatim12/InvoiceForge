package com.invoiceforge.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val INVOICE_PREFIX = stringPreferencesKey("invoice_prefix")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(raw)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val invoicePrefix: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.INVOICE_PREFIX] ?: "INV-"
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setInvoicePrefix(prefix: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INVOICE_PREFIX] = prefix.trim()
        }
    }
}
