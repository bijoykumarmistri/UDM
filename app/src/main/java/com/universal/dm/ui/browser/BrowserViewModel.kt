package com.universal.dm.ui.browser

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.universal.dm.data.model.DetectedPageMedia
import com.universal.dm.domain.adblock.AdBlockerEngine
import com.universal.dm.domain.sniffer.MediaSniffer
import com.universal.dm.ui.settings.AppSettingsState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLEncoder

data class BrowserUiState(
    // Tab Management
    val tabs: List<BrowserTab> = listOf(BrowserTab(title = "Home", url = null, isIncognito = false)),
    val activeTabId: String = tabs.first().id,
    val isTabSwitcherOpen: Boolean = false,
    val isIncognito: Boolean = false,

    // Navigation & Omnibar
    val urlInput: String = "",
    val currentLoadedUrl: String? = null,
    val pageTitle: String? = null,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,

    // Search Engine
    val selectedSearchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val isDesktopMode: Boolean = false,
    val isJavaScriptEnabled: Boolean = true,

    // Ad & Tracker Protection
    val blockedAdsCount: Int = 0,

    // Media Sniffer & Detection
    val detectedMediaState: DetectedPageMedia? = null,
    val isExtractingMedia: Boolean = false,
    val showMediaBottomSheet: Boolean = false,
    val autoOpenBottomSheetOnMediaDetected: Boolean = false,

    // Browser Menu & Dialogs
    val isMenuExpanded: Boolean = false,
    val isSearchEngineDialogVisible: Boolean = false,

    // Custom Favourites
    val favourites: List<FavouriteSite> = emptyList(),
    val isPinSiteDialogVisible: Boolean = false
) {
    val activeTab: BrowserTab?
        get() = tabs.find { it.id == activeTabId }

    val normalTabsCount: Int
        get() = tabs.count { !it.isIncognito }

    val incognitoTabsCount: Int
        get() = tabs.count { it.isIncognito }
}

/**
 * Pinned user favourite site model with persistent storage support.
 */
data class FavouriteSite(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val badge: String = "Site",
    val colorHex: String = "#00E5FF"
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("udm_app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FAVOURITES = "key_browser_favourites"
    }

    private fun loadSavedSearchEngine(): SearchEngine {
        val savedName = prefs.getString(AppSettingsState.KEY_SEARCH_ENGINE, SearchEngine.DUCKDUCKGO.name)
        return try {
            SearchEngine.valueOf(savedName ?: SearchEngine.DUCKDUCKGO.name)
        } catch (_: Exception) {
            SearchEngine.DUCKDUCKGO
        }
    }

    private fun defaultFavourites(): List<FavouriteSite> = listOf(
        FavouriteSite(id = "fav_yt", title = "YouTube", url = "https://m.youtube.com", badge = "4K AV1", colorHex = "#FF0033"),
        FavouriteSite(id = "fav_sc", title = "SoundCloud", url = "https://soundcloud.com", badge = "320k Audio", colorHex = "#FF5500"),
        FavouriteSite(id = "fav_vm", title = "Vimeo", url = "https://vimeo.com", badge = "1080p Studio", colorHex = "#00ADEF"),
        FavouriteSite(id = "fav_dm", title = "Dailymotion", url = "https://dailymotion.com", badge = "HD Stream", colorHex = "#0066DC"),
        FavouriteSite(id = "fav_ar", title = "Archive.org", url = "https://archive.org", badge = "Public Domain", colorHex = "#EAA610"),
        FavouriteSite(id = "fav_ddg", title = "DuckDuckGo", url = "https://duckduckgo.com", badge = "Privacy Search", colorHex = "#00B4D8")
    )

    private fun loadSavedFavourites(): List<FavouriteSite> {
        val jsonString = prefs.getString(KEY_FAVOURITES, null)
        if (jsonString.isNullOrBlank()) {
            return defaultFavourites()
        }
        return try {
            val array = org.json.JSONArray(jsonString)
            val list = mutableListOf<FavouriteSite>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FavouriteSite(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        badge = obj.optString("badge", "Site"),
                        colorHex = obj.optString("colorHex", "#00E5FF")
                    )
                )
            }
            if (list.isEmpty()) defaultFavourites() else list
        } catch (_: Exception) {
            defaultFavourites()
        }
    }

    private fun saveFavourites(list: List<FavouriteSite>) {
        val array = org.json.JSONArray()
        for (item in list) {
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("badge", item.badge)
            obj.put("colorHex", item.colorHex)
            array.put(obj)
        }
        prefs.edit().putString(KEY_FAVOURITES, array.toString()).apply()
    }

    private val _uiState = run {
        val engine = loadSavedSearchEngine()
        val initialTab = BrowserTab(title = "New Tab", url = null, isIncognito = false)
        MutableStateFlow(
            BrowserUiState(
                selectedSearchEngine = engine,
                favourites = loadSavedFavourites(),
                tabs = listOf(initialTab),
                activeTabId = initialTab.id,
                currentLoadedUrl = null,
                urlInput = ""
            )
        )
    }
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    val adBlockerEngine: AdBlockerEngine = AdBlockerEngine.getInstance(application)
    val mediaSniffer: MediaSniffer = MediaSniffer()

    private var mediaExtractionJob: Job? = null

    fun onUrlInputChanged(newInput: String) {
        _uiState.update { it.copy(urlInput = newInput) }
    }

    fun loadUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val searchEngine = _uiState.value.selectedSearchEngine
        val finalUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> searchEngine.queryUrl + URLEncoder.encode(trimmed, "UTF-8")
        }

        val activeId = _uiState.value.activeTabId
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(url = finalUrl, title = tab.title.ifBlank { "Loading..." }, blockedAdsCount = 0)
                } else tab
            }
            state.copy(
                tabs = updatedTabs,
                urlInput = finalUrl,
                currentLoadedUrl = finalUrl,
                isLoading = true,
                blockedAdsCount = 0,
                detectedMediaState = null,
                isTabSwitcherOpen = false
            )
        }
    }

    fun handleSharedUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return

        val searchEngine = _uiState.value.selectedSearchEngine
        val finalUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> searchEngine.queryUrl + URLEncoder.encode(trimmed, "UTF-8")
        }

        val activeId = _uiState.value.activeTabId
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(url = finalUrl, title = "Loading...", blockedAdsCount = 0)
                } else tab
            }
            state.copy(
                tabs = updatedTabs,
                urlInput = finalUrl,
                currentLoadedUrl = finalUrl,
                isLoading = true,
                blockedAdsCount = 0,
                detectedMediaState = null,
                autoOpenBottomSheetOnMediaDetected = true,
                isTabSwitcherOpen = false
            )
        }

        triggerMediaExtraction(finalUrl, null, autoShowBottomSheet = true)
    }

    // --- TAB MANAGEMENT ---

    fun openNewTab(isIncognito: Boolean = _uiState.value.isIncognito, initialUrl: String? = null) {
        val targetUrl = initialUrl
        val newTab = BrowserTab(
            title = if (isIncognito) "Incognito" else "New Tab",
            url = targetUrl,
            isIncognito = isIncognito
        )
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs + newTab,
                activeTabId = newTab.id,
                isIncognito = isIncognito,
                currentLoadedUrl = targetUrl,
                urlInput = targetUrl ?: "",
                pageTitle = if (!targetUrl.isNullOrEmpty()) null else newTab.title,
                canGoBack = false,
                canGoForward = false,
                blockedAdsCount = 0,
                detectedMediaState = null,
                isTabSwitcherOpen = false
            )
        }

        if (!targetUrl.isNullOrEmpty()) {
            loadUrl(targetUrl)
        }
    }

    fun selectTab(tabId: String) {
        val targetTab = _uiState.value.tabs.find { it.id == tabId } ?: return
        _uiState.update { state ->
            state.copy(
                activeTabId = targetTab.id,
                isIncognito = targetTab.isIncognito,
                currentLoadedUrl = targetTab.url,
                urlInput = targetTab.url ?: "",
                pageTitle = targetTab.title,
                canGoBack = targetTab.canGoBack,
                canGoForward = targetTab.canGoForward,
                blockedAdsCount = targetTab.blockedAdsCount,
                detectedMediaState = null,
                isTabSwitcherOpen = false
            )
        }
    }

    fun closeTab(tabId: String) {
        _uiState.update { state ->
            val remainingTabs = state.tabs.filter { it.id != tabId }
            if (remainingTabs.isEmpty()) {
                // If all tabs closed, start fresh tab defaulting to clean Start Page
                val fresh = BrowserTab(title = "New Tab", url = null, isIncognito = state.isIncognito)
                state.copy(
                    tabs = listOf(fresh),
                    activeTabId = fresh.id,
                    currentLoadedUrl = null,
                    urlInput = "",
                    pageTitle = null,
                    detectedMediaState = null,
                    blockedAdsCount = 0
                )
            } else {
                val newActiveTab = if (state.activeTabId == tabId) remainingTabs.last() else remainingTabs.find { it.id == state.activeTabId } ?: remainingTabs.last()
                state.copy(
                    tabs = remainingTabs,
                    activeTabId = newActiveTab.id,
                    isIncognito = newActiveTab.isIncognito,
                    currentLoadedUrl = newActiveTab.url,
                    urlInput = newActiveTab.url ?: "",
                    pageTitle = newActiveTab.title,
                    canGoBack = newActiveTab.canGoBack,
                    canGoForward = newActiveTab.canGoForward,
                    blockedAdsCount = newActiveTab.blockedAdsCount,
                    detectedMediaState = null
                )
            }
        }
    }

    fun closeAllTabs(incognitoOnly: Boolean = false) {
        _uiState.update { state ->
            val remaining = if (incognitoOnly) {
                state.tabs.filter { !it.isIncognito }
            } else {
                emptyList()
            }
            if (remaining.isEmpty()) {
                val fresh = BrowserTab(title = "New Tab", url = null, isIncognito = false)
                state.copy(
                    tabs = listOf(fresh),
                    activeTabId = fresh.id,
                    isIncognito = false,
                    currentLoadedUrl = null,
                    urlInput = "",
                    pageTitle = null,
                    detectedMediaState = null,
                    blockedAdsCount = 0,
                    isTabSwitcherOpen = false
                )
            } else {
                val active = remaining.last()
                state.copy(
                    tabs = remaining,
                    activeTabId = active.id,
                    isIncognito = active.isIncognito,
                    currentLoadedUrl = active.url,
                    urlInput = active.url ?: "",
                    pageTitle = active.title,
                    isTabSwitcherOpen = false
                )
            }
        }
    }

    fun openTabSwitcher() {
        _uiState.update { it.copy(isTabSwitcherOpen = true, isMenuExpanded = false) }
    }

    fun dismissTabSwitcher() {
        _uiState.update { it.copy(isTabSwitcherOpen = false) }
    }

    fun toggleTabSwitcher() {
        _uiState.update { it.copy(isTabSwitcherOpen = !it.isTabSwitcherOpen, isMenuExpanded = false) }
    }

    // --- FAVOURITES MANAGEMENT ---

    fun pinFavourite(title: String, url: String, badge: String = "Site", colorHex: String = "#00E5FF") {
        val cleanTitle = title.trim().ifBlank { "Site" }
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        val newSite = FavouriteSite(
            title = cleanTitle,
            url = cleanUrl,
            badge = badge,
            colorHex = colorHex
        )
        _uiState.update { state ->
            val updated = state.favourites + newSite
            saveFavourites(updated)
            state.copy(favourites = updated, isPinSiteDialogVisible = false)
        }
    }

    fun removeFavourite(id: String) {
        _uiState.update { state ->
            val updated = state.favourites.filter { it.id != id }
            saveFavourites(updated)
            state.copy(favourites = updated)
        }
    }

    fun setPinSiteDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isPinSiteDialogVisible = visible, isMenuExpanded = false) }
    }

    fun setIncognitoMode(enabled: Boolean) {
        val existing = _uiState.value.tabs.find { it.isIncognito == enabled }
        if (existing != null) {
            selectTab(existing.id)
        } else {
            openNewTab(isIncognito = enabled)
        }
    }

    // --- SEARCH ENGINE & PREFERENCES ---

    fun selectSearchEngine(engine: SearchEngine) {
        prefs.edit().putString(AppSettingsState.KEY_SEARCH_ENGINE, engine.name).apply()
        _uiState.update {
            it.copy(
                selectedSearchEngine = engine,
                isSearchEngineDialogVisible = false
            )
        }
    }

    fun setSearchEngineDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isSearchEngineDialogVisible = visible, isMenuExpanded = false) }
    }

    fun toggleDesktopMode() {
        _uiState.update { it.copy(isDesktopMode = !it.isDesktopMode, isMenuExpanded = false) }
    }

    fun toggleJavaScript() {
        _uiState.update { it.copy(isJavaScriptEnabled = !it.isJavaScriptEnabled, isMenuExpanded = false) }
    }

    fun toggleMenu() {
        _uiState.update { it.copy(isMenuExpanded = !it.isMenuExpanded) }
    }

    fun dismissMenu() {
        _uiState.update { it.copy(isMenuExpanded = false) }
    }

    // --- PAGE NAVIGATION & LIFECYCLE ---

    fun onPageStarted(url: String, isAutoSniffEnabled: Boolean = false) {
        if (url.isBlank() || url.equals("about:blank", ignoreCase = true)) return

        val activeId = _uiState.value.activeTabId
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(url = url, blockedAdsCount = 0)
                } else tab
            }
            state.copy(
                tabs = updatedTabs,
                currentLoadedUrl = url,
                urlInput = url,
                isLoading = true,
                blockedAdsCount = 0
            )
        }
        if (isAutoSniffEnabled && mediaSniffer.isMediaOrDownloadableUrl(url)) {
            triggerMediaExtraction(url, null)
        }
    }

    fun onPageFinished(url: String, title: String?, isAutoSniffEnabled: Boolean = false) {
        if (url.isBlank() || url.equals("about:blank", ignoreCase = true)) return

        val activeId = _uiState.value.activeTabId
        val displayTitle = if (!title.isNullOrBlank()) title else url
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(url = url, title = displayTitle)
                } else tab
            }
            state.copy(
                tabs = updatedTabs,
                currentLoadedUrl = url,
                pageTitle = displayTitle,
                isLoading = false
            )
        }
        if ((isAutoSniffEnabled && mediaSniffer.isMediaOrDownloadableUrl(url)) || _uiState.value.autoOpenBottomSheetOnMediaDetected) {
            triggerMediaExtraction(url, title, autoShowBottomSheet = _uiState.value.autoOpenBottomSheetOnMediaDetected)
        }
    }

    fun onProgressChanged(progress: Int) {
        _uiState.update {
            it.copy(
                progress = progress,
                isLoading = progress < 100
            )
        }
    }

    fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        val activeId = _uiState.value.activeTabId
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(canGoBack = canGoBack, canGoForward = canGoForward)
                } else tab
            }
            state.copy(
                tabs = updatedTabs,
                canGoBack = canGoBack,
                canGoForward = canGoForward
            )
        }
    }

    fun onAdBlocked() {
        val activeId = _uiState.value.activeTabId
        _uiState.update { state ->
            val newCount = state.blockedAdsCount + 1
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(blockedAdsCount = newCount)
                } else tab
            }
            state.copy(tabs = updatedTabs, blockedAdsCount = newCount)
        }
    }

    fun triggerMediaExtraction(url: String, fallbackTitle: String?, autoShowBottomSheet: Boolean = false) {
        android.util.Log.d("BrowserViewModel", "triggerMediaExtraction called for: $url, autoShow: $autoShowBottomSheet")
        // Prevent re-triggering if we already extracted media for this exact page or if an extraction is actively in progress
        val currentMedia = _uiState.value.detectedMediaState
        if (currentMedia != null && currentMedia.pageUrl == url && currentMedia.videoVariants.size > 1) {
            android.util.Log.d("BrowserViewModel", "Already have extracted media for $url")
            if (autoShowBottomSheet) {
                _uiState.update { it.copy(showMediaBottomSheet = true) }
            }
            return
        }

        mediaExtractionJob?.cancel()
        mediaExtractionJob = viewModelScope.launch {
            _uiState.update { it.copy(isExtractingMedia = true) }
            android.util.Log.d("BrowserViewModel", "Launching extractMedia for: $url")
            val media = mediaSniffer.extractMedia(url, fallbackTitle)
            android.util.Log.d("BrowserViewModel", "extractMedia finished for $url. Result media != null: ${media != null}, videoVariants: ${media?.videoVariants?.size ?: 0}")
            val hasRealStreams = media != null && (
                media.videoVariants.isNotEmpty() ||
                media.audioVariants.isNotEmpty() ||
                media.directFileUrl != null ||
                media.directFileVariant != null
            )
            val validMedia = if (hasRealStreams) media else null

            _uiState.update { current ->
                val oldVariantsCount = (current.detectedMediaState?.videoVariants?.size ?: 0) +
                        (current.detectedMediaState?.audioVariants?.size ?: 0) +
                        (if (current.detectedMediaState?.directFileVariant != null || current.detectedMediaState?.directFileUrl != null) 1 else 0)
                val newVariantsCount = (validMedia?.videoVariants?.size ?: 0) +
                        (validMedia?.audioVariants?.size ?: 0) +
                        (if (validMedia?.directFileVariant != null || validMedia?.directFileUrl != null) 1 else 0)

                val shouldUpdateMedia = when {
                    validMedia == null -> false
                    current.detectedMediaState == null -> true
                    validMedia.directFileVariant != null -> true
                    newVariantsCount >= oldVariantsCount -> true
                    else -> false
                }

                val finalMedia = if (shouldUpdateMedia) validMedia else current.detectedMediaState
                val shouldShow = (autoShowBottomSheet || current.autoOpenBottomSheetOnMediaDetected) && finalMedia != null
                android.util.Log.d("BrowserViewModel", "Updated detectedMediaState: finalMedia != null: ${finalMedia != null}, shouldShow: $shouldShow")
                current.copy(
                    isExtractingMedia = false,
                    detectedMediaState = finalMedia,
                    showMediaBottomSheet = shouldShow || current.showMediaBottomSheet,
                    autoOpenBottomSheetOnMediaDetected = if (shouldShow) false else current.autoOpenBottomSheetOnMediaDetected
                )
            }
        }
    }

    fun cancelSniffing() {
        mediaExtractionJob?.cancel()
        mediaExtractionJob = null
        _uiState.update { it.copy(isExtractingMedia = false) }
    }

    fun dismissDetectedMedia() {
        cancelSniffing()
        _uiState.update { it.copy(detectedMediaState = null, showMediaBottomSheet = false) }
    }

    fun manualSniffCurrentPage(url: String? = null, fallbackTitle: String? = null) {
        val targetUrl = url ?: _uiState.value.currentLoadedUrl ?: return
        android.util.Log.d("BrowserViewModel", "manualSniffCurrentPage: targetUrl: $targetUrl")
        triggerMediaExtraction(targetUrl, fallbackTitle ?: _uiState.value.pageTitle, autoShowBottomSheet = true)
    }

    fun showBottomSheet() {
        _uiState.update { it.copy(showMediaBottomSheet = true) }
    }

    fun hideBottomSheet() {
        _uiState.update { it.copy(showMediaBottomSheet = false) }
    }

    fun clearToHome() {
        val activeId = _uiState.value.activeTabId
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(url = null, title = if (tab.isIncognito) "Incognito" else "New Tab", blockedAdsCount = 0)
                } else tab
            }
            state.copy(
                tabs = updatedTabs,
                currentLoadedUrl = null,
                urlInput = "",
                pageTitle = null,
                detectedMediaState = null,
                blockedAdsCount = 0,
                isLoading = false
            )
        }
    }
}
