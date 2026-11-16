package com.liquiddebloat.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Small preference holder. Compose state so the UI recomposes without Flow plumbing,
 * SharedPreferences so the value survives process death.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("liquid_debloat", Context.MODE_PRIVATE)

    var backupEnabled by mutableStateOf(prefs.getBoolean(KEY_BACKUP, true))
        private set

    var strongMode by mutableStateOf(prefs.getBoolean(KEY_STRONG, false))
        private set

    fun updateBackup(enabled: Boolean) {
        backupEnabled = enabled
        prefs.edit().putBoolean(KEY_BACKUP, enabled).apply()
    }

    fun updateStrong(enabled: Boolean) {
        strongMode = enabled
        prefs.edit().putBoolean(KEY_STRONG, enabled).apply()
    }

    private companion object {
        const val KEY_BACKUP = "backup_enabled"
        const val KEY_STRONG = "strong_mode"
    }
}
