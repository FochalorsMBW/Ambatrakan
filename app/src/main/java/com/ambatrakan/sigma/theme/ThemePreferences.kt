package com.ambatrakan.sigma.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Pilihan mode tema yang bisa dipilih user di Settings. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Menyimpan & mengekspos preferensi mode tema (SYSTEM/LIGHT/DARK).
 * Disimpan di SharedPreferences agar bertahan antar sesi, dan diekspos sebagai
 * StateFlow agar Compose bisa bereaksi langsung saat user mengganti toggle.
 */
class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private fun loadMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_MODE, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val KEY_MODE = "theme_mode"
    }
}
