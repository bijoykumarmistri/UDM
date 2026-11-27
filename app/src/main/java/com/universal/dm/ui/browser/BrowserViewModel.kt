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
    val recentlyClosedTabs: List<BrowserTab> = emptyList(),
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

    // Search Engine & WebView Controls
    val selectedSearchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val isDesktopMode: Boolean = false,
    val isJavaScriptEnabled: Boolean = true,
    val isForceDarkWebPages: Boolean = false,
    val textZoom: Int = 100,

    // Find in Page
    val isFindInPageVisible: Boolean = false,
    val findQuery: String = "",
    val findActiveMatch: Int = 0,
    val findTotalMatches: Int = 0,

    // Ad & Tracker Protection
    val blockedAdsCount: Int = 0,
    val sessionBlockedAdsTotal: Int = 0,

    // Media Sniffer & Detection
    val detectedMediaState: DetectedPageMedia? = null,
    val isExtractingMedia: Boolean = false,
    val showMediaBottomSheet: Boolean = false,
    val autoOpenBottomSheetOnMediaDetected: Boolean = false,

    // Browser Menu, Sheets & Dialogs
    val isMenuExpanded: Boolean = false,
    val isSearchEngineDialogVisible: Boolean = false,
    val isHistorySheetOpen: Boolean = false,
    val isPageInfoDialogVisible: Boolean = false,

    // Browsing History & Custom Favourites
    val history: List<BrowserHistoryItem> = emptyList(),
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
    private val historyPrefs = application.getSharedPreferences("udm_browser_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FAVOURITES = "key_browser_favourites"
        private const val KEY_HISTORY = "key_browser_history_items"
        private const val KEY_TEXT_ZOOM = "key_browser_text_zoom"
        private const val KEY_FORCE_DARK_WEB = "key_browser_force_dark_web"
        private const val MAX_HISTORY_ITEMS = 150
        private const val MAX_CLOSED_TABS = 10
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

    private fun loadSavedHistory(): List<BrowserHistoryItem> {
        val jsonString = historyPrefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = org.json.JSONArray(jsonString)
            val list = ArrayList<BrowserHistoryItem>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val url = obj.optString("url")
                if (url.isNotBlank()) {
                    list.add(
                        BrowserHistoryItem(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            title = obj.optString("title", url),
                            url = url,
                            visitedAt = obj.optLong("visitedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(list: List<BrowserHistoryItem>) {
        val array = org.json.JSONArray()
        for (item in list.take(MAX_HISTORY_ITEMS)) {
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("visitedAt", item.visitedAt)
            array.put(obj)
        }
        historyPrefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    private val _uiState = run {
        val engine = loadSavedSearchEngine()
        val initialTab = BrowserTab(title = "New Tab", url = null, isIncognito = false)
        MutableStateFlow(
            BrowserUiState(
                selectedSearchEngine = engine,
                favourites = loadSavedFavourites(),
                history = loadSavedHistory(),
                textZoom = prefs.getInt(KEY_TEXT_ZOOM, 100).coerceIn(75, 175),
                isForceDarkWebPages = prefs.getBoolean(KEY_FORCE_DARK_WEB, false),
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
    private var activeExtractionCleanUrl: String? = null
    private var activeExtractionTabId: String? = null
    private var isAutoShowRequestedForActiveExtraction: Boolean = false

    fun onUrlInputChanged(newInput: String) {
        _uiState.update { it.copy(urlInput = newInput) }
    }

    fun loadUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val searchEngine = _uiState.value.selectedSearchEngine
        val finalUrl = when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> searchEngine.queryUrl + URLEncoder.encode(trimmed, "UTF-8")
        }

        val activeId = _uiState.value.activeTabId
        cancelSniffingForTab(activeId)
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(
                        url = finalUrl,
                        title = tab.title.ifBlank { "Loading..." },
                        blockedAdsCount = 0,
                        detectedMedia = null
                    )
                } else tab
            }
            state.copy(
                tabs = updatedTabs,
                urlInput = finalUrl,
                currentLoadedUrl = finalUrl,
                isLoading = true,
                blockedAdsCount = 0,
                detectedMediaState = null,
                isTabSwitcherOpen = false,
                isHistorySheetOpen = false,
                isFindInPageVisible = false,
                findQuery = "",
                findActiveMatch = 0,
                findTotalMatches = 0
            )
        }
    }

    fun handleSharedUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return

        val searchEngine = _uiState.value.selectedSearchEngine
        val finalUrl = when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> searchEngine.queryUrl + URLEncoder.encode(trimmed, "UTF-8")
        }

        val activeId = _uiState.value.activeTabId
        cancelSniffingForTab(activeId)
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(url = finalUrl, title = "Loading...", blockedAdsCount = 0, detectedMedia = null)
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
                isExtractingMedia = activeExtractionTabId == newTab.id && mediaExtractionJob?.isActive == true,
                showMediaBottomSheet = false,
                isTabSwitcherOpen = false,
                isFindInPageVisible = false,
                findQuery = "",
                findActiveMatch = 0,
                findTotalMatches = 0
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
                detectedMediaState = targetTab.detectedMedia,
                isExtractingMedia = activeExtractionTabId == targetTab.id && mediaExtractionJob?.isActive == true,
                showMediaBottomSheet = false,
                isTabSwitcherOpen = false,
                isFindInPageVisible = false,
                findQuery = "",
                findActiveMatch = 0,
                findTotalMatches = 0
            )
        }
    }

    fun closeTab(tabId: String) {
        cancelSniffingForTab(tabId)
        _uiState.update { state ->
            val closingTab = state.tabs.find { it.id == tabId }
            val updatedClosed = if (closingTab != null && !closingTab.isIncognito && !closingTab.url.isNullOrBlank()) {
                (listOf(closingTab.copy(detectedMedia = null)) + state.recentlyClosedTabs).take(MAX_CLOSED_TABS)
            } else {
                state.recentlyClosedTabs
            }

            val remainingTabs = state.tabs.filter { it.id != tabId }
            if (remainingTabs.isEmpty()) {
                val fresh = BrowserTab(title = "New Tab", url = null, isIncognito = state.isIncognito)
                state.copy(
                    tabs = listOf(fresh),
                    activeTabId = fresh.id,
                    recentlyClosedTabs = updatedClosed,
                    currentLoadedUrl = null,
                    urlInput = "",
                    pageTitle = null,
                    detectedMediaState = null,
                    isExtractingMedia = false,
                    showMediaBottomSheet = false,
                    blockedAdsCount = 0,
                    isFindInPageVisible = false,
                    findQuery = "",
                    isMenuExpanded = false
                )
            } else {
                val newActiveTab = if (state.activeTabId == tabId) {
                    remainingTabs.last()
                } else {
                    remainingTabs.find { it.id == state.activeTabId } ?: remainingTabs.last()
                }
                state.copy(
                    tabs = remainingTabs,
                    activeTabId = newActiveTab.id,
                    recentlyClosedTabs = updatedClosed,
                    isIncognito = newActiveTab.isIncognito,
                    currentLoadedUrl = newActiveTab.url,
                    urlInput = newActiveTab.url ?: "",
                    pageTitle = newActiveTab.title,
                    canGoBack = newActiveTab.canGoBack,
                    canGoForward = newActiveTab.canGoForward,
                    blockedAdsCount = newActiveTab.blockedAdsCount,
                    detectedMediaState = newActiveTab.detectedMedia,
                    isExtractingMedia = activeExtractionTabId == newActiveTab.id && mediaExtractionJob?.isActive == true,
                    showMediaBottomSheet = false,
                    isFindInPageVisible = false,
                    findQuery = "",
                    isMenuExpanded = false
                )
            }
        }
    }

    fun reopenLastClosedTab(): Boolean {
        val lastClosed = _uiState.value.recentlyClosedTabs.firstOrNull() ?: return false
        _uiState.update { state ->
            val rest = state.recentlyClosedTabs.drop(1)
            val restored = lastClosed.copy(
                id = java.util.UUID.randomUUID().toString(),
                lastAccessedTimestamp = System.currentTimeMillis()
            )
            state.copy(
                tabs = state.tabs + restored,
                activeTabId = restored.id,
                recentlyClosedTabs = rest,
                isIncognito = false,
                currentLoadedUrl = restored.url,
                urlInput = restored.url ?: "",
                pageTitle = restored.title,
                canGoBack = false,
                canGoForward = false,
                blockedAdsCount = 0,
                detectedMediaState = null,
                isExtractingMedia = false,
                showMediaBottomSheet = false,
                isTabSwitcherOpen = false,
                isMenuExpanded = false
            )
        }
        return true
    }

    fun closeAllTabs(incognitoOnly: Boolean = false) {
        cancelSniffing()
        _uiState.update { state ->
            val closedStandard = if (!incognitoOnly) {
                state.tabs.filter { !it.isIncognito && !it.url.isNullOrBlank() }.map { it.copy(detectedMedia = null) }
            } else emptyList()
            val updatedClosed = (closedStandard.reversed() + state.recentlyClosedTabs).take(MAX_CLOSED_TABS)

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
                    recentlyClosedTabs = updatedClosed,
                    isIncognito = false,
                    currentLoadedUrl = null,
                    urlInput = "",
                    pageTitle = null,
                    detectedMediaState = null,
                    isExtractingMedia = false,
                    showMediaBottomSheet = false,
                    blockedAdsCount = 0,
                    isTabSwitcherOpen = false,
                    isFindInPageVisible = false,
                    findQuery = ""
                )
            } else {
                val active = remaining.last()
                state.copy(
                    tabs = remaining,
                    activeTabId = active.id,
                    recentlyClosedTabs = updatedClosed,
                    isIncognito = active.isIncognito,
                    currentLoadedUrl = active.url,
                    urlInput = active.url ?: "",
                    pageTitle = active.title,
                    detectedMediaState = active.detectedMedia,
                    showMediaBottomSheet = false,
                    isTabSwitcherOpen = false,
                    isFindInPageVisible = false,
                    findQuery = ""
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
        if (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true)) {
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

    // --- SEARCH ENGINE, ENGINE SETTINGS & FIND IN PAGE ---

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

    fun toggleForceDarkWebPages() {
        _uiState.update { state ->
            val next = !state.isForceDarkWebPages
            prefs.edit().putBoolean(KEY_FORCE_DARK_WEB, next).apply()
            state.copy(isForceDarkWebPages = next, isMenuExpanded = false)
        }
    }

    fun adjustTextZoom(delta: Int) {
        _uiState.update { state ->
            val next = (state.textZoom + delta).coerceIn(75, 175)
            prefs.edit().putInt(KEY_TEXT_ZOOM, next).apply()
            state.copy(textZoom = next)
        }
    }

    fun resetTextZoom() {
        prefs.edit().putInt(KEY_TEXT_ZOOM, 100).apply()
        _uiState.update { it.copy(textZoom = 100) }
    }

    fun setFindInPageVisible(visible: Boolean) {
        _uiState.update {
            it.copy(
                isFindInPageVisible = visible,
                isMenuExpanded = false,
                findQuery = if (visible) it.findQuery else "",
                findActiveMatch = if (visible) it.findActiveMatch else 0,
                findTotalMatches = if (visible) it.findTotalMatches else 0
            )
        }
    }

    fun onFindQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                findQuery = query,
                findActiveMatch = if (query.isBlank()) 0 else it.findActiveMatch,
                findTotalMatches = if (query.isBlank()) 0 else it.findTotalMatches
            )
        }
    }

    fun onFindResultsUpdated(activeMatchOrdinal: Int, numberOfMatches: Int) {
        val displayActive = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
        _uiState.update {
            it.copy(
                findActiveMatch = displayActive,
                findTotalMatches = numberOfMatches
            )
        }
    }

    fun setHistorySheetOpen(open: Boolean) {
        _uiState.update { it.copy(isHistorySheetOpen = open, isMenuExpanded = false) }
    }

    fun setPageInfoDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isPageInfoDialogVisible = visible, isMenuExpanded = false) }
    }

    fun deleteHistoryItem(id: String) {
        _uiState.update { state ->
            val updated = state.history.filter { it.id != id }
            saveHistory(updated)
            state.copy(history = updated)
        }
    }

    fun clearHistory() {
        historyPrefs.edit().remove(KEY_HISTORY).apply()
        _uiState.update { it.copy(history = emptyList()) }
    }

    private fun recordHistory(url: String, title: String?) {
        if (_uiState.value.isIncognito) return
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) return

        val cleanTitle = title?.trim()?.takeIf { it.isNotBlank() && !it.equals("about:blank", ignoreCase = true) } ?: url
        _uiState.update { state ->
            // Collapse duplicates of the exact same URL so history stays clean and chronological
            val filtered = state.history.filterNot { it.url.equals(url, ignoreCase = true) }
            val entry = BrowserHistoryItem(
                title = cleanTitle,
                url = url,
                visitedAt = System.currentTimeMillis()
            )
            val updated = (listOf(entry) + filtered).take(MAX_HISTORY_ITEMS)
            saveHistory(updated)
            state.copy(history = updated)
        }
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
        val newCleanUrl = mediaSniffer.cleanMediaUrl(url)

        _uiState.update { state ->
            val activeTab = state.tabs.find { it.id == activeId }
            val oldCleanMediaUrl = activeTab?.detectedMedia?.pageUrl?.let { mediaSniffer.cleanMediaUrl(it) }
            val mediaInvalidated = oldCleanMediaUrl != null && oldCleanMediaUrl != newCleanUrl

            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(
                        url = url,
                        blockedAdsCount = 0,
                        detectedMedia = if (mediaInvalidated) null else tab.detectedMedia
                    )
                } else tab
            }
            state.copy(
                tabs = updatedTabs,
                currentLoadedUrl = url,
                urlInput = url,
                isLoading = true,
                blockedAdsCount = 0,
                detectedMediaState = if (mediaInvalidated) null else state.detectedMediaState
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
        recordHistory(url, title)
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
        _uiState.update { state ->
            val activeId = state.activeTabId
            val newCount = state.blockedAdsCount + 1
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(blockedAdsCount = newCount)
                } else tab
            }
            state.copy(
                tabs = updatedTabs,
                blockedAdsCount = newCount,
                sessionBlockedAdsTotal = state.sessionBlockedAdsTotal + 1
            )
        }
    }

    fun triggerMediaExtraction(url: String, fallbackTitle: String?, autoShowBottomSheet: Boolean = false) {
        val targetTabId = _uiState.value.activeTabId
        val cleanUrl = mediaSniffer.cleanMediaUrl(url)
        android.util.Log.d("BrowserViewModel", "triggerMediaExtraction called for: $url (clean: $cleanUrl), tab=$targetTabId, autoShow: $autoShowBottomSheet")

        // 1. Check in-memory LRU cache first (0ms instant response)
        val cached = mediaSniffer.getCachedMedia(cleanUrl) ?: mediaSniffer.getCachedMedia(url)
        if (cached != null) {
            android.util.Log.d("BrowserViewModel", "Instant cache hit for $cleanUrl")
            _uiState.update { current ->
                val updatedTabs = current.tabs.map { tab ->
                    if (tab.id == targetTabId) tab.copy(detectedMedia = cached) else tab
                }
                if (current.activeTabId == targetTabId) {
                    val shouldShow = autoShowBottomSheet || current.showMediaBottomSheet || current.autoOpenBottomSheetOnMediaDetected
                    current.copy(
                        tabs = updatedTabs,
                        isExtractingMedia = false,
                        detectedMediaState = cached,
                        showMediaBottomSheet = shouldShow,
                        autoOpenBottomSheetOnMediaDetected = false
                    )
                } else {
                    current.copy(tabs = updatedTabs)
                }
            }
            return
        }

        // 2. Prevent restarting or cancelling if extraction is currently in-flight for this exact URL and tab!
        if (mediaExtractionJob?.isActive == true && activeExtractionCleanUrl == cleanUrl && activeExtractionTabId == targetTabId) {
            android.util.Log.d("BrowserViewModel", "Extraction already in progress for $cleanUrl; deduplicating.")
            if (autoShowBottomSheet) {
                isAutoShowRequestedForActiveExtraction = true
            }
            return
        }

        // 3. Prevent re-triggering if current UI state already has full media for this URL
        val currentMedia = _uiState.value.detectedMediaState
        val currentCleanUrl = currentMedia?.pageUrl?.let { mediaSniffer.cleanMediaUrl(it) }
        if (currentMedia != null && (currentCleanUrl == cleanUrl || currentMedia.pageUrl == url) && currentMedia.videoVariants.size > 1) {
            android.util.Log.d("BrowserViewModel", "Already have extracted media for $url")
            if (autoShowBottomSheet) {
                _uiState.update { it.copy(showMediaBottomSheet = true) }
            }
            return
        }

        mediaExtractionJob?.cancel()
        activeExtractionCleanUrl = cleanUrl
        activeExtractionTabId = targetTabId
        isAutoShowRequestedForActiveExtraction = autoShowBottomSheet

        mediaExtractionJob = viewModelScope.launch {
            try {
                _uiState.update { current ->
                    if (current.activeTabId == targetTabId) current.copy(isExtractingMedia = true) else current
                }
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
                    val targetTab = current.tabs.find { it.id == targetTabId } ?: return@update current
                    val oldMedia = targetTab.detectedMedia
                    val oldVariantsCount = (oldMedia?.videoVariants?.size ?: 0) +
                        (oldMedia?.audioVariants?.size ?: 0) +
                        (if (oldMedia?.directFileVariant != null || oldMedia?.directFileUrl != null) 1 else 0)
                    val newVariantsCount = (validMedia?.videoVariants?.size ?: 0) +
                        (validMedia?.audioVariants?.size ?: 0) +
                        (if (validMedia?.directFileVariant != null || validMedia?.directFileUrl != null) 1 else 0)

                    val shouldUpdateMedia = when {
                        validMedia == null -> false
                        oldMedia == null -> true
                        validMedia.directFileVariant != null -> true
                        newVariantsCount >= oldVariantsCount -> true
                        else -> false
                    }

                    val finalMedia = if (shouldUpdateMedia) validMedia else oldMedia
                    val updatedTabs = current.tabs.map { tab ->
                        if (tab.id == targetTabId) tab.copy(detectedMedia = finalMedia) else tab
                    }

                    // Only mutate visible bottom sheet / extraction spinner if user is still viewing targetTabId
                    if (current.activeTabId == targetTabId) {
                        val shouldShow = (autoShowBottomSheet || isAutoShowRequestedForActiveExtraction || current.autoOpenBottomSheetOnMediaDetected) && finalMedia != null
                        android.util.Log.d("BrowserViewModel", "Updated detectedMediaState: finalMedia != null: ${finalMedia != null}, shouldShow: $shouldShow")
                        current.copy(
                            tabs = updatedTabs,
                            isExtractingMedia = false,
                            detectedMediaState = finalMedia,
                            showMediaBottomSheet = shouldShow || current.showMediaBottomSheet,
                            autoOpenBottomSheetOnMediaDetected = if (shouldShow) false else current.autoOpenBottomSheetOnMediaDetected
                        )
                    } else {
                        current.copy(tabs = updatedTabs)
                    }
                }
            } finally {
                if (activeExtractionCleanUrl == cleanUrl && activeExtractionTabId == targetTabId) {
                    activeExtractionCleanUrl = null
                    activeExtractionTabId = null
                    isAutoShowRequestedForActiveExtraction = false
                }
            }
        }
    }

    private fun cancelSniffingForTab(tabId: String) {
        if (activeExtractionTabId == tabId) {
            cancelSniffing()
        }
    }

    fun cancelSniffing() {
        mediaExtractionJob?.cancel()
        mediaExtractionJob = null
        activeExtractionCleanUrl = null
        activeExtractionTabId = null
        isAutoShowRequestedForActiveExtraction = false
        _uiState.update { it.copy(isExtractingMedia = false) }
    }

    fun dismissDetectedMedia() {
        cancelSniffing()
        val activeId = _uiState.value.activeTabId
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) tab.copy(detectedMedia = null) else tab
            }
            state.copy(tabs = updatedTabs, detectedMediaState = null, showMediaBottomSheet = false)
        }
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
        cancelSniffingForTab(activeId)
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(
                        url = null,
                        title = if (tab.isIncognito) "Incognito" else "New Tab",
                        blockedAdsCount = 0,
                        detectedMedia = null
                    )
                } else tab
            }
            state.copy(
                tabs = updatedTabs,
                currentLoadedUrl = null,
                urlInput = "",
                pageTitle = null,
                detectedMediaState = null,
                isExtractingMedia = false,
                showMediaBottomSheet = false,
                blockedAdsCount = 0,
                isLoading = false,
                isFindInPageVisible = false,
                findQuery = "",
                findActiveMatch = 0,
                findTotalMatches = 0
            )
        }
    }

    override fun onCleared() {
        mediaExtractionJob?.cancel()
    }
}
