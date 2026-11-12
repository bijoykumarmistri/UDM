package com.universal.dm.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Persistent Application Settings backed by SharedPreferences.
 * Ensures settings like thread count, theme, ad-blocker, and download folder
 * are saved across app restarts and process recreation.
 */
class AppSettingsState(private val context: Context? = null) {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var threadCount by mutableIntStateOf(prefs?.getInt(KEY_THREAD_COUNT, 8) ?: 8)
    var defaultDownloadFolder by mutableStateOf(prefs?.getString(KEY_DOWNLOAD_FOLDER, "/storage/emulated/0/Download/UDM") ?: "/storage/emulated/0/Download/UDM")
    var isAdBlockerEnabled by mutableStateOf(prefs?.getBoolean(KEY_AD_BLOCKER, true) ?: true)
    var blockPopupsAndRedirects by mutableStateOf(prefs?.getBoolean(KEY_BLOCK_POPUPS, true) ?: true)
    var wifiOnlyDownloads by mutableStateOf(prefs?.getBoolean(KEY_WIFI_ONLY, false) ?: false)
    var isDynamicColorEnabled by mutableStateOf(prefs?.getBoolean(KEY_DYNAMIC_COLOR, true) ?: true)

    // null means follow system theme, true = dark, false = light
    var isDarkThemeEnabled by mutableStateOf<Boolean?>(
        if (prefs?.contains(KEY_DARK_THEME) == true) prefs.getBoolean(KEY_DARK_THEME, false) else null
    )

    var activeDownloadsCount by mutableIntStateOf(0)

    fun updateThreadCount(newCount: Int) {
        val clamped = newCount.coerceIn(1, 16)
        threadCount = clamped
        prefs?.edit()?.putInt(KEY_THREAD_COUNT, clamped)?.apply()
    }

    fun updateDefaultDownloadFolder(path: String) {
        defaultDownloadFolder = path
        prefs?.edit()?.putString(KEY_DOWNLOAD_FOLDER, path)?.apply()
    }

    fun updateAdBlocker(enabled: Boolean) {
        isAdBlockerEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_AD_BLOCKER, enabled)?.apply()
    }

    fun updateBlockPopups(enabled: Boolean) {
        blockPopupsAndRedirects = enabled
        prefs?.edit()?.putBoolean(KEY_BLOCK_POPUPS, enabled)?.apply()
    }

    fun updateWifiOnly(enabled: Boolean) {
        wifiOnlyDownloads = enabled
        prefs?.edit()?.putBoolean(KEY_WIFI_ONLY, enabled)?.apply()
    }

    fun updateDynamicColor(enabled: Boolean) {
        isDynamicColorEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_DYNAMIC_COLOR, enabled)?.apply()
    }

    fun updateDarkTheme(enabled: Boolean?) {
        isDarkThemeEnabled = enabled
        prefs?.edit()?.let { editor ->
            if (enabled == null) {
                editor.remove(KEY_DARK_THEME)
            } else {
                editor.putBoolean(KEY_DARK_THEME, enabled)
            }
            editor.apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "udm_app_settings"
        private const val KEY_THREAD_COUNT = "key_thread_count"
        private const val KEY_DOWNLOAD_FOLDER = "key_download_folder"
        private const val KEY_AD_BLOCKER = "key_ad_blocker"
        private const val KEY_BLOCK_POPUPS = "key_block_popups"
        private const val KEY_WIFI_ONLY = "key_wifi_only"
        private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"
        private const val KEY_DARK_THEME = "key_dark_theme"
    }
}

val LocalAppSettings = compositionLocalOf { AppSettingsState() }
