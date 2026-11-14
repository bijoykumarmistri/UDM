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
    var isDnsFilterEnabled by mutableStateOf(prefs?.getBoolean(KEY_DNS_FILTER, true) ?: true)
    var blockPopupsAndRedirects by mutableStateOf(prefs?.getBoolean(KEY_BLOCK_POPUPS, true) ?: true)
    var wifiOnlyDownloads by mutableStateOf(prefs?.getBoolean(KEY_WIFI_ONLY, false) ?: false)
    var isDynamicColorEnabled by mutableStateOf(prefs?.getBoolean(KEY_DYNAMIC_COLOR, true) ?: true)
    var defaultSearchEngineName by mutableStateOf(prefs?.getString(KEY_SEARCH_ENGINE, "DUCKDUCKGO") ?: "DUCKDUCKGO")
    var soundOnCompletion by mutableStateOf(prefs?.getBoolean(KEY_SOUND_COMPLETION, true) ?: true)
    var autoResumeOnWifi by mutableStateOf(prefs?.getBoolean(KEY_AUTO_RESUME_WIFI, true) ?: true)
    val isAutoSnifferEnabled: Boolean = true

    var activeDownloadsCount by mutableIntStateOf(0)

    init {
        // Ensure auto sniffer is always active across upgrades
        prefs?.edit()?.putBoolean(KEY_AUTO_SNIFFER, true)?.apply()
    }

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

    fun updateDnsFilter(enabled: Boolean) {
        isDnsFilterEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_DNS_FILTER, enabled)?.apply()
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

    fun updateAutoSniffer(enabled: Boolean = true) {
        // Auto-sniffing is permanently enabled to ensure streams are always detected
        prefs?.edit()?.putBoolean(KEY_AUTO_SNIFFER, true)?.apply()
    }

    fun updateSearchEngine(engineName: String) {
        defaultSearchEngineName = engineName
        prefs?.edit()?.putString(KEY_SEARCH_ENGINE, engineName)?.apply()
    }

    fun updateSoundOnCompletion(enabled: Boolean) {
        soundOnCompletion = enabled
        prefs?.edit()?.putBoolean(KEY_SOUND_COMPLETION, enabled)?.apply()
    }

    fun updateAutoResumeOnWifi(enabled: Boolean) {
        autoResumeOnWifi = enabled
        prefs?.edit()?.putBoolean(KEY_AUTO_RESUME_WIFI, enabled)?.apply()
    }

    companion object {
        private const val PREFS_NAME = "udm_app_settings"
        private const val KEY_THREAD_COUNT = "key_thread_count"
        private const val KEY_DOWNLOAD_FOLDER = "key_download_folder"
        private const val KEY_AD_BLOCKER = "key_ad_blocker"
        private const val KEY_DNS_FILTER = "key_dns_filter"
        private const val KEY_BLOCK_POPUPS = "key_block_popups"
        private const val KEY_WIFI_ONLY = "key_wifi_only"
        private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"
        const val KEY_SEARCH_ENGINE = "key_search_engine"
        private const val KEY_SOUND_COMPLETION = "key_sound_completion"
        private const val KEY_AUTO_RESUME_WIFI = "key_auto_resume_wifi"
        private const val KEY_AUTO_SNIFFER = "key_auto_sniffer"
    }
}

val LocalAppSettings = compositionLocalOf { AppSettingsState() }
