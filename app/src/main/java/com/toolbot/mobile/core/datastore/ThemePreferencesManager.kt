package com.toolbot.mobile.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings_preferences")

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class ThemePreferencesManager(private val context: Context) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_mode")
        @Volatile
        private var INSTANCE: ThemePreferencesManager? = null

        fun getInstance(context: Context): ThemePreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemePreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data
        .map { preferences ->
            val name = preferences[THEME_KEY] ?: AppThemeMode.SYSTEM.name
            try {
                AppThemeMode.valueOf(name)
            } catch (e: Exception) {
                AppThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = mode.name
        }
    }
}
