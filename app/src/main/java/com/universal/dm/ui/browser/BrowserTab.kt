package com.universal.dm.ui.browser

import java.util.UUID

/**
 * Supported search engines for the UDM browser omnibar
 */
enum class SearchEngine(
    val displayName: String,
    val queryUrl: String,
    val homeUrl: String,
    val hint: String
) {
    DUCKDUCKGO(
        displayName = "DuckDuckGo",
        queryUrl = "https://duckduckgo.com/?q=",
        homeUrl = "https://duckduckgo.com",
        hint = "Search privately with DuckDuckGo"
    ),
    GOOGLE(
        displayName = "Google",
        queryUrl = "https://www.google.com/search?q=",
        homeUrl = "https://www.google.com",
        hint = "Search with Google"
    ),
    BING(
        displayName = "Bing",
        queryUrl = "https://www.bing.com/search?q=",
        homeUrl = "https://www.bing.com",
        hint = "Search with Bing"
    ),
    BRAVE(
        displayName = "Brave Search",
        queryUrl = "https://search.brave.com/search?q=",
        homeUrl = "https://search.brave.com",
        hint = "Search independently with Brave"
    ),
    ECOSIA(
        displayName = "Ecosia",
        queryUrl = "https://www.ecosia.org/search?q=",
        homeUrl = "https://www.ecosia.org",
        hint = "Search & plant trees with Ecosia"
    )
}

/**
 * Individual tab entity for multi-tab browsing
 */
data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String? = null,
    val isIncognito: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val blockedAdsCount: Int = 0,
    val lastAccessedTimestamp: Long = System.currentTimeMillis()
)
