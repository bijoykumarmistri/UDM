package com.universal.dm.ui.browser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.universal.dm.data.model.DetectedPageMedia
import com.universal.dm.domain.adblock.AdBlockerEngine
import com.universal.dm.domain.sniffer.MediaSniffer
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
    val isSearchEngineDialogVisible: Boolean = false
) {
    val activeTab: BrowserTab?
        get() = tabs.find { it.id == activeTabId }

    val normalTabsCount: Int
        get() = tabs.count { !it.isIncognito }

    val incognitoTabsCount: Int
        get() = tabs.count { it.isIncognito }
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    val adBlockerEngine: AdBlockerEngine = AdBlockerEngine.getInstance(application)
    val mediaSniffer: MediaSniffer = MediaSniffer()

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

        if (mediaSniffer.isMediaOrDownloadableUrl(finalUrl)) {
            triggerMediaExtraction(finalUrl, null)
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
        val newTab = BrowserTab(
            title = if (initialUrl != null) "Loading..." else if (isIncognito) "Incognito" else "New Tab",
            url = initialUrl,
            isIncognito = isIncognito
        )
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs + newTab,
                activeTabId = newTab.id,
                isIncognito = isIncognito,
                currentLoadedUrl = initialUrl,
                urlInput = initialUrl ?: "",
                pageTitle = if (initialUrl != null) null else newTab.title,
                canGoBack = false,
                canGoForward = false,
                blockedAdsCount = 0,
                detectedMediaState = null,
                isTabSwitcherOpen = false
            )
        }

        if (initialUrl != null) {
            loadUrl(initialUrl)
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
                // If all tabs closed, start fresh tab
                val fresh = BrowserTab(title = "New Tab", isIncognito = state.isIncognito)
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
                val fresh = BrowserTab(title = "New Tab", isIncognito = false)
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

    fun toggleTabSwitcher() {
        _uiState.update { it.copy(isTabSwitcherOpen = !it.isTabSwitcherOpen, isMenuExpanded = false) }
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

    fun onPageStarted(url: String) {
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
        if (mediaSniffer.isMediaOrDownloadableUrl(url)) {
            triggerMediaExtraction(url, null)
        }
    }

    fun onPageFinished(url: String, title: String?) {
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
        if (mediaSniffer.isMediaOrDownloadableUrl(url) || _uiState.value.autoOpenBottomSheetOnMediaDetected) {
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
        // Prevent re-triggering if we already extracted media for this exact page or if an extraction is active
        val currentMedia = _uiState.value.detectedMediaState
        if (currentMedia != null && currentMedia.pageUrl == url && currentMedia.videoVariants.size > 1) {
            if (autoShowBottomSheet) {
                _uiState.update { it.copy(showMediaBottomSheet = true) }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExtractingMedia = true) }
            val media = mediaSniffer.extractMedia(url, fallbackTitle)
            _uiState.update { current ->
                // Guard: only overwrite if newly extracted media has more or equal variants
                val oldVariantsCount = current.detectedMediaState?.videoVariants?.size ?: 0
                val shouldUpdateMedia = when {
                    media == null -> false
                    current.detectedMediaState == null -> true
                    media.videoVariants.size >= oldVariantsCount -> true
                    else -> false
                }

                val finalMedia = if (shouldUpdateMedia) media else current.detectedMediaState
                val shouldShow = (autoShowBottomSheet || current.autoOpenBottomSheetOnMediaDetected) && finalMedia != null
                current.copy(
                    isExtractingMedia = false,
                    detectedMediaState = finalMedia,
                    showMediaBottomSheet = shouldShow || current.showMediaBottomSheet,
                    autoOpenBottomSheetOnMediaDetected = if (shouldShow) false else current.autoOpenBottomSheetOnMediaDetected
                )
            }
        }
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
                blockedAdsCount = 0
            )
        }
    }
}
