package com.universal.dm.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.universal.dm.UdmApplication
import kotlinx.coroutines.launch

enum class AppThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

/**
 * Persistent Application Settings backed by SharedPreferences.
 * Ensures settings like thread count, theme mode, pitch black mode, privacy protections,
 * and download preferences are saved across app restarts and process recreation.
 */
class AppSettingsState(private val context: Context? = null) {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(loadThemeMode())
    var threadCount by mutableIntStateOf(prefs?.getInt(KEY_THREAD_COUNT, 8) ?: 8)
    var preferredVideoQuality by mutableIntStateOf(prefs?.getInt(KEY_PREFERRED_QUALITY, 1080) ?: 1080)
    var defaultDownloadFolder by mutableStateOf(prefs?.getString(KEY_DOWNLOAD_FOLDER, "/storage/emulated/0/Download/UDM") ?: "/storage/emulated/0/Download/UDM")
    var isAdBlockerEnabled by mutableStateOf(prefs?.getBoolean(KEY_AD_BLOCKER, true) ?: true)
    var isDnsFilterEnabled by mutableStateOf(prefs?.getBoolean(KEY_DNS_FILTER, true) ?: true)
    var blockPopupsAndRedirects by mutableStateOf(prefs?.getBoolean(KEY_BLOCK_POPUPS, true) ?: true)
    var blockThirdPartyCookies by mutableStateOf(prefs?.getBoolean(KEY_BLOCK_THIRD_PARTY_COOKIES, true) ?: true)
    var wifiOnlyDownloads by mutableStateOf(prefs?.getBoolean(KEY_WIFI_ONLY, false) ?: false)
    var isPitchBlackEnabled by mutableStateOf(prefs?.getBoolean(KEY_PITCH_BLACK, false) ?: false)
    var defaultSearchEngineName by mutableStateOf(prefs?.getString(KEY_SEARCH_ENGINE, "DUCKDUCKGO") ?: "DUCKDUCKGO")
    var soundOnCompletion by mutableStateOf(prefs?.getBoolean(KEY_SOUND_COMPLETION, true) ?: true)
    var autoResumeOnWifi by mutableStateOf(prefs?.getBoolean(KEY_AUTO_RESUME_WIFI, true) ?: true)
    var autoRetryOnFailure by mutableStateOf(prefs?.getBoolean(KEY_AUTO_RETRY, true) ?: true)
    var embedAudioThumbnail by mutableStateOf(prefs?.getBoolean(KEY_EMBED_AUDIO_THUMBNAIL, true) ?: true)
    val isAutoSnifferEnabled: Boolean = true

    var activeDownloadsCount by mutableIntStateOf(0)

    init {
        // Ensure auto sniffer is always active across upgrades
        prefs?.edit()?.putBoolean(KEY_AUTO_SNIFFER, true)?.apply()
    }

    private fun loadThemeMode(): AppThemeMode {
        val raw = prefs?.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return runCatching { AppThemeMode.valueOf(raw ?: AppThemeMode.SYSTEM.name) }
            .getOrDefault(AppThemeMode.SYSTEM)
    }

    fun updateThemeMode(mode: AppThemeMode) {
        themeMode = mode
        prefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }

    fun updateThreadCount(newCount: Int) {
        val clamped = newCount.coerceIn(1, 16)
        threadCount = clamped
        prefs?.edit()?.putInt(KEY_THREAD_COUNT, clamped)?.apply()
    }

    fun updatePreferredVideoQuality(height: Int) {
        val normalized = when (height) {
            2160, 1080, 720, 480 -> height
            else -> 1080
        }
        preferredVideoQuality = normalized
        prefs?.edit()?.putInt(KEY_PREFERRED_QUALITY, normalized)?.apply()
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

    fun updateBlockThirdPartyCookies(enabled: Boolean) {
        blockThirdPartyCookies = enabled
        prefs?.edit()?.putBoolean(KEY_BLOCK_THIRD_PARTY_COOKIES, enabled)?.apply()
    }

    fun updateWifiOnly(enabled: Boolean) {
        wifiOnlyDownloads = enabled
        prefs?.edit()?.putBoolean(KEY_WIFI_ONLY, enabled)?.apply()
        val appContext = context?.applicationContext
        if (appContext != null) {
            UdmApplication.appScope.launch {
                runCatching {
                    com.universal.dm.domain.downloader.DownloadManager.getInstance(appContext).updateNetworkConstraints()
                }
            }
        }
    }

    fun updatePitchBlack(enabled: Boolean) {
        isPitchBlackEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_PITCH_BLACK, enabled)?.apply()
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

    fun updateAutoRetryOnFailure(enabled: Boolean) {
        autoRetryOnFailure = enabled
        prefs?.edit()?.putBoolean(KEY_AUTO_RETRY, enabled)?.apply()
    }

    fun updateEmbedAudioThumbnail(enabled: Boolean) {
        embedAudioThumbnail = enabled
        prefs?.edit()?.putBoolean(KEY_EMBED_AUDIO_THUMBNAIL, enabled)?.apply()
    }

    companion object {
        const val PREFS_NAME = "udm_app_settings"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_THREAD_COUNT = "key_thread_count"
        private const val KEY_PREFERRED_QUALITY = "key_preferred_quality"
        private const val KEY_DOWNLOAD_FOLDER = "key_download_folder"
        private const val KEY_AD_BLOCKER = "key_ad_blocker"
        private const val KEY_DNS_FILTER = "key_dns_filter"
        private const val KEY_BLOCK_POPUPS = "key_block_popups"
        private const val KEY_BLOCK_THIRD_PARTY_COOKIES = "key_block_third_party_cookies"
        private const val KEY_WIFI_ONLY = "key_wifi_only"
        private const val KEY_PITCH_BLACK = "key_pitch_black"
        const val KEY_SEARCH_ENGINE = "key_search_engine"
        private const val KEY_SOUND_COMPLETION = "key_sound_completion"
        private const val KEY_AUTO_RESUME_WIFI = "key_auto_resume_wifi"
        const val KEY_AUTO_RETRY = "key_auto_retry"
        const val KEY_EMBED_AUDIO_THUMBNAIL = "key_embed_audio_thumbnail"
        private const val KEY_AUTO_SNIFFER = "key_auto_sniffer"
    }
}

val LocalAppSettings = compositionLocalOf { AppSettingsState() }
