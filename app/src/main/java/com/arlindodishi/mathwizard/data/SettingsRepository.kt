package com.arlindodishi.mathwizard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    val soundOn: Boolean = true,
    val musicOn: Boolean = true,
    val hapticsOn: Boolean = true,
    /** Longer timers (×1.75) for players who want less pressure. */
    val relaxed: Boolean = false,
    /** Classic Duel only. */
    val ttsOn: Boolean = true,
    /** Classic Duel only. */
    val highContrast: Boolean = false,
)

enum class Setting(val key: Preferences.Key<Boolean>, val default: Boolean) {
    SOUND(booleanPreferencesKey("sound_enabled"), true),
    MUSIC(booleanPreferencesKey("music_enabled"), true),
    HAPTICS(booleanPreferencesKey("haptics_enabled"), true),
    RELAXED(booleanPreferencesKey("relaxed_mode"), false),
    TTS(booleanPreferencesKey("tts_enabled"), true),
    HIGH_CONTRAST(booleanPreferencesKey("high_contrast"), false),
}

/**
 * App settings in Preferences DataStore. The first read migrates the old SharedPreferences
 * "Settings" file (classic screens and step 7), so nothing set before is lost.
 */
class SettingsRepository(context: Context) {
    private val store: DataStore<Preferences> = context.applicationContext.settingsStore

    val settings: Flow<Settings> = store.data.map { p ->
        fun get(s: Setting) = p[s.key] ?: s.default
        Settings(
            soundOn = get(Setting.SOUND),
            musicOn = get(Setting.MUSIC),
            hapticsOn = get(Setting.HAPTICS),
            relaxed = get(Setting.RELAXED),
            ttsOn = get(Setting.TTS),
            highContrast = get(Setting.HIGH_CONTRAST),
        )
    }

    suspend fun set(setting: Setting, value: Boolean) {
        store.edit { it[setting.key] = value }
    }
}

private const val LEGACY_PREFS = "Settings"

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, LEGACY_PREFS)) },
)
