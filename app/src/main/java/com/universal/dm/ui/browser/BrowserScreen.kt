package com.universal.dm.ui.browser

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universal.dm.domain.adblock.AdBlockerEngine
import com.universal.dm.domain.downloader.DownloadManager
import com.universal.dm.domain.sniffer.MediaSniffer
import com.universal.dm.ui.components.UdmHeaderActionButton
import com.universal.dm.ui.components.UdmIconSquircle
import com.universal.dm.ui.settings.LocalAppSettings
import com.universal.dm.ui.theme.UDMTheme
import com.universal.dm.ui.theme.UdmTheme
import kotlinx.coroutines.launch
import java.net.URI

private const val LEGACY_FORCE_DARK_CSS_JS = """
    (function() {
        var id = 'udm-force-dark-style';
        var existing = document.getElementById(id);
        if (!existing) {
            var style = document.createElement('style');
            style.id = id;
            style.textContent = 'html { filter: invert(0.92) hue-rotate(180deg) !important; background: #121212 !important; } img, video, picture, canvas, svg, [style*="background-image"] { filter: invert(1) hue-rotate(180deg) !important; }';
            (document.head || document.documentElement).appendChild(style);
        }
    })();
"""

private class AppliedWebSettingsTracker(
    var desktopMode: Boolean,
    var jsEnabled: Boolean,
    var forceDark: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    modifier: Modifier = Modifier,
    sharedUrl: String? = null,
    onSharedUrlHandled: () -> Unit = {},
    onDownloadTriggered: (String, String) -> Unit = { _, _ -> }
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Browser Preview", style = MaterialTheme.typography.titleMedium)
        }
    } else {
        val vm: BrowserViewModel = viewModel()
        BrowserScreenContent(
            modifier = modifier,
            viewModel = vm,
            sharedUrl = sharedUrl,
            onSharedUrlHandled = onSharedUrlHandled,
            onDownloadTriggered = onDownloadTriggered
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserScreenContent(
    modifier: Modifier,
    viewModel: BrowserViewModel,
    sharedUrl: String?,
    onSharedUrlHandled: () -> Unit,
    onDownloadTriggered: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val appSettings = LocalAppSettings.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var favouriteToRemove by remember { mutableStateOf<FavouriteSite?>(null) }

    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            viewModel.handleSharedUrl(sharedUrl)
            onSharedUrlHandled()
        }
    }

    LaunchedEffect(uiState.findQuery, uiState.isFindInPageVisible, webViewInstance) {
        val wv = webViewInstance ?: return@LaunchedEffect
        if (!uiState.isFindInPageVisible || uiState.findQuery.isBlank()) {
            wv.clearMatches()
        } else {
            wv.findAllAsync(uiState.findQuery)
        }
    }

    val handleSmartBackNavigation: () -> Unit = {
        when {
            uiState.isFindInPageVisible -> {
                webViewInstance?.clearMatches()
                viewModel.setFindInPageVisible(false)
            }
            uiState.isHistorySheetOpen -> {
                viewModel.setHistorySheetOpen(false)
            }
            uiState.isTabSwitcherOpen -> {
                viewModel.dismissTabSwitcher()
            }
            uiState.currentLoadedUrl != null -> {
                val webView = webViewInstance
                if (webView != null && webView.canGoBack()) {
                    val history = webView.copyBackForwardList()
                    val curIndex = history.currentIndex
                    if (curIndex <= 0) {
                        webView.stopLoading()
                        webView.clearHistory()
                        webViewInstance = null
                        viewModel.clearToHome()
                    } else if (curIndex == 1) {
                        val curItem = history.getItemAtIndex(curIndex)
                        val prevItem = history.getItemAtIndex(0)
                        val curHost = curItem?.url?.let { runCatching { URI(it).host?.removePrefix("www.") }.getOrNull() }
                        val prevHost = prevItem?.url?.let { runCatching { URI(it).host?.removePrefix("www.") }.getOrNull() }
                        val prevUrl = prevItem?.url ?: ""

                        if (curHost == prevHost || prevUrl.contains("vimeo.com", ignoreCase = true)) {
                            webView.stopLoading()
                            webView.clearHistory()
                            webViewInstance = null
                            viewModel.clearToHome()
                        } else {
                            webView.goBack()
                        }
                    } else {
                        val curItem = history.getItemAtIndex(curIndex)
                        val prevItem = history.getItemAtIndex(curIndex - 1)
                        val curHost = curItem?.url?.let { runCatching { URI(it).host?.removePrefix("www.") }.getOrNull() }
                        val prevHost = prevItem?.url?.let { runCatching { URI(it).host?.removePrefix("www.") }.getOrNull() }

                        if (curHost != null && curHost == prevHost && curIndex >= 2) {
                            val prevPrevItem = history.getItemAtIndex(curIndex - 2)
                            val prevPrevHost = prevPrevItem?.url?.let { runCatching { URI(it).host?.removePrefix("www.") }.getOrNull() }
                            if (prevPrevHost != curHost) {
                                webView.goBackOrForward(-2)
                            } else {
                                webView.goBack()
                            }
                        } else {
                            webView.goBack()
                        }
                    }
                } else {
                    webViewInstance?.stopLoading()
                    webViewInstance?.clearHistory()
                    webViewInstance = null
                    viewModel.clearToHome()
                }
            }
            else -> {}
        }
    }

    val handleHomeNavigation: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        webViewInstance?.stopLoading()
        webViewInstance?.clearHistory()
        webViewInstance = null
        viewModel.clearToHome()
    }

    val handleClearSiteData: () -> Unit = {
        webViewInstance?.clearCache(true)
        runCatching { CookieManager.getInstance().removeAllCookies(null) }
        runCatching { CookieManager.getInstance().flush() }
        runCatching { WebStorage.getInstance().deleteAllData() }
        viewModel.dismissMenu()
        scope.launch {
            snackbarHostState.showSnackbar("Cache & cookies cleared")
        }
    }

    val handleCopyCurrentUrl: () -> Unit = {
        val url = uiState.currentLoadedUrl
        if (!url.isNullOrBlank()) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("URL", url))
            scope.launch {
                snackbarHostState.showSnackbar("Link copied")
            }
        }
        viewModel.dismissMenu()
    }

    val handleShareCurrentUrl: () -> Unit = {
        val url = uiState.currentLoadedUrl
        if (!url.isNullOrBlank()) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Link"))
        }
        viewModel.dismissMenu()
    }

    val handleOpenExternalBrowser: () -> Unit = {
        val url = uiState.currentLoadedUrl
        if (!url.isNullOrBlank()) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
        viewModel.dismissMenu()
    }

    val handlePinCurrentOrOpenDialog: () -> Unit = {
        val pageUrl = uiState.currentLoadedUrl
        if (!pageUrl.isNullOrBlank()) {
            viewModel.pinFavourite(
                title = uiState.pageTitle ?: "Bookmark",
                url = pageUrl,
                badge = "Site"
            )
            scope.launch {
                snackbarHostState.showSnackbar("Added to bookmarks")
            }
        } else {
            viewModel.setPinSiteDialogVisible(true)
        }
        viewModel.dismissMenu()
    }

    BackHandler(
        enabled = uiState.currentLoadedUrl != null ||
            uiState.isTabSwitcherOpen ||
            uiState.isHistorySheetOpen ||
            uiState.isFindInPageVisible
    ) {
        handleSmartBackNavigation()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            BrowserTopBar(
                uiState = uiState,
                appSettingsAdBlocker = appSettings.isAdBlockerEnabled,
                onAdBlockerToggle = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    appSettings.updateAdBlocker(!appSettings.isAdBlockerEnabled)
                },
                onUrlChanged = { viewModel.onUrlInputChanged(it) },
                onGo = { viewModel.loadUrl(uiState.urlInput) },
                onClear = { viewModel.onUrlInputChanged("") },
                onBack = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    handleSmartBackNavigation()
                },
                onForward = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    webViewInstance?.goForward()
                    viewModel.dismissMenu()
                },
                onHome = {
                    handleHomeNavigation()
                    viewModel.dismissMenu()
                },
                onPinCurrentPage = handlePinCurrentOrOpenDialog,
                onOpenSearchEngineDialog = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setSearchEngineDialogVisible(true)
                },
                onOpenPageInfo = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setPageInfoDialogVisible(true)
                },
                onOpenHistory = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setHistorySheetOpen(true)
                },
                onToggleTabs = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.toggleTabSwitcher()
                },
                onToggleMenu = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.toggleMenu()
                },
                onDismissMenu = { viewModel.dismissMenu() },
                onNewTab = {
                    viewModel.openNewTab(isIncognito = false)
                    viewModel.dismissMenu()
                },
                onToggleIncognito = {
                    viewModel.setIncognitoMode(!uiState.isIncognito)
                    viewModel.dismissMenu()
                },
                onReopenClosedTab = {
                    if (viewModel.reopenLastClosedTab()) {
                        scope.launch { snackbarHostState.showSnackbar("Restored closed tab") }
                    }
                },
                onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                onToggleForceDark = { viewModel.toggleForceDarkWebPages() },
                onOpenFindInPage = { viewModel.setFindInPageVisible(true) },
                onFindQueryChanged = { viewModel.onFindQueryChanged(it) },
                onFindNext = { forward -> webViewInstance?.findNext(forward) },
                onCloseFindInPage = {
                    webViewInstance?.clearMatches()
                    viewModel.setFindInPageVisible(false)
                },
                onCopyUrl = handleCopyCurrentUrl,
                onShare = handleShareCurrentUrl,
                onOpenExternalBrowser = handleOpenExternalBrowser,
                onClearSiteData = handleClearSiteData,
                onReloadOrStop = {
                    if (uiState.isLoading) {
                        webViewInstance?.stopLoading()
                    } else {
                        webViewInstance?.reload()
                    }
                    viewModel.dismissMenu()
                },
                onManualSniff = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val currentUrl = webViewInstance?.url ?: uiState.currentLoadedUrl
                    val currentTitle = webViewInstance?.title ?: uiState.pageTitle
                    if (uiState.detectedMediaState != null) {
                        viewModel.showBottomSheet()
                    } else {
                        webViewInstance?.evaluateJavascript(MediaSniffer.JS_MEDIA_SCANNER, null)
                        if (currentUrl != null) {
                            viewModel.manualSniffCurrentPage(url = currentUrl, fallbackTitle = currentTitle)
                        }
                    }
                }
            )

            AnimatedContent(
                targetState = uiState.currentLoadedUrl == null,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                },
                label = "BrowserViewportTransition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { isHome ->
                if (isHome) {
                    BrowserHomeDashboard(
                        isIncognito = uiState.isIncognito,
                        selectedSearchEngine = uiState.selectedSearchEngine,
                        favourites = uiState.favourites,
                        onSearchEngineClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setSearchEngineDialogVisible(true)
                        },
                        onBookmarkClicked = { url ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.loadUrl(url)
                        },
                        onPasteAndDownload = { pasteUrl ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onUrlInputChanged(pasteUrl)
                            viewModel.loadUrl(pasteUrl)
                        },
                        onPinSiteClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setPinSiteDialogVisible(true)
                        },
                        onRemoveFavourite = { site ->
                            favouriteToRemove = site
                        }
                    )
                } else {
                    var lastLoadedProgrammaticUrl by remember { mutableStateOf<String?>(null) }
                    val mainHandler = remember { Handler(Looper.getMainLooper()) }
                    val appliedTracker = remember {
                        AppliedWebSettingsTracker(
                            desktopMode = uiState.isDesktopMode,
                            jsEnabled = uiState.isJavaScriptEnabled,
                            forceDark = uiState.isForceDarkWebPages
                        )
                    }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                @SuppressLint("SetJavaScriptEnabled")
                                settings.javaScriptEnabled = uiState.isJavaScriptEnabled
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.loadWithOverviewMode = uiState.isDesktopMode
                                settings.useWideViewPort = true
                                settings.textZoom = uiState.textZoom
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.setSupportMultipleWindows(true)
                                settings.javaScriptCanOpenWindowsAutomatically = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                runCatching {
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(
                                        this,
                                        !appSettings.blockThirdPartyCookies
                                    )
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    settings.isAlgorithmicDarkeningAllowed = uiState.isForceDarkWebPages
                                }

                                if (uiState.isDesktopMode) {
                                    settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                }

                                if (uiState.isIncognito) {
                                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                    clearHistory()
                                    clearCache(true)
                                }

                                setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                                    if (isDoneCounting) {
                                        viewModel.onFindResultsUpdated(activeMatchOrdinal, numberOfMatches)
                                    }
                                }

                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onMediaFound(mediaUrl: String) {
                                        val currentMedia = viewModel.uiState.value.detectedMediaState
                                        val isChunkOrBlob = mediaUrl.startsWith("blob:") ||
                                            mediaUrl.contains("videoplayback") ||
                                            mediaUrl.contains("/init.", ignoreCase = true) ||
                                            mediaUrl.contains(".m4s", ignoreCase = true)
                                        if (currentMedia == null && !isChunkOrBlob) {
                                            mainHandler.post {
                                                viewModel.triggerMediaExtraction(mediaUrl, viewModel.uiState.value.pageTitle, autoShowBottomSheet = false)
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onUrlChanged(newUrl: String) {
                                        mainHandler.post {
                                            lastLoadedProgrammaticUrl = newUrl
                                            viewModel.onPageStarted(newUrl, isAutoSniffEnabled = true)
                                        }
                                    }
                                }, "UdmMediaBridge")

                                setDownloadListener { url, _, _, _, _ ->
                                    mainHandler.post {
                                        viewModel.triggerMediaExtraction(url, title)
                                        viewModel.showBottomSheet()
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val targetUrl = request?.url?.toString() ?: return false
                                        if (targetUrl.startsWith("http://", ignoreCase = true) ||
                                            targetUrl.startsWith("https://", ignoreCase = true)
                                        ) {
                                            return false
                                        }
                                        if (appSettings.blockPopupsAndRedirects && request?.hasGesture() == false) {
                                            return true
                                        }
                                        return runCatching {
                                            val intent = Intent(Intent.ACTION_VIEW, request?.url).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            ctx.startActivity(intent)
                                            true
                                        }.getOrDefault(true)
                                    }

                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): WebResourceResponse? {
                                        val reqUrl = request?.url ?: return null

                                        val isHostBlocked = appSettings.isDnsFilterEnabled && reqUrl.host?.let { viewModel.adBlockerEngine.isHostBlocked(it) } == true
                                        val isAdBlocked = appSettings.isAdBlockerEnabled && viewModel.adBlockerEngine.shouldBlockRequest(reqUrl)

                                        if (isHostBlocked || isAdBlocked) {
                                            viewModel.onAdBlocked()
                                            return viewModel.adBlockerEngine.createEmptyResourceResponse()
                                        }

                                        val urlString = reqUrl.toString()
                                        val pageUrl = viewModel.uiState.value.currentLoadedUrl ?: ""
                                        val isOnKnownPlatform = viewModel.mediaSniffer.isSupportedPlatformUrl(pageUrl)

                                        if (appSettings.isAutoSnifferEnabled && !isOnKnownPlatform && viewModel.mediaSniffer.isMediaOrDownloadableUrl(urlString)) {
                                            val currentMedia = viewModel.uiState.value.detectedMediaState
                                            val isChunkOrSegment = urlString.contains("videoplayback") ||
                                                urlString.contains("range=") ||
                                                urlString.contains(".ts") ||
                                                urlString.contains(".m4s", ignoreCase = true) ||
                                                urlString.contains("/init.", ignoreCase = true) ||
                                                urlString.contains("soundcloud.cloud", ignoreCase = true)
                                            if (currentMedia == null && !isChunkOrSegment) {
                                                val currentTitle = viewModel.uiState.value.pageTitle
                                                mainHandler.post {
                                                    viewModel.triggerMediaExtraction(urlString, currentTitle)
                                                }
                                            }
                                        }

                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        if (url.isNullOrBlank() || url.equals("about:blank", ignoreCase = true)) return
                                        lastLoadedProgrammaticUrl = url
                                        viewModel.onPageStarted(url, appSettings.isAutoSnifferEnabled)
                                        viewModel.onNavigationStateChanged(canGoBack(), canGoForward())
                                    }

                                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                        super.doUpdateVisitedHistory(view, url, isReload)
                                        if (url.isNullOrBlank() || url.equals("about:blank", ignoreCase = true)) return
                                        if (url != lastLoadedProgrammaticUrl) {
                                            lastLoadedProgrammaticUrl = url
                                            viewModel.onPageStarted(url, appSettings.isAutoSnifferEnabled)
                                        }
                                        viewModel.onNavigationStateChanged(canGoBack(), canGoForward())
                                    }

                                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                                        super.onPageCommitVisible(view, url)
                                        if (appSettings.isAdBlockerEnabled) {
                                            view?.evaluateJavascript(AdBlockerEngine.BRAVE_SHIELD_CSS_JS, null)
                                            view?.evaluateJavascript(AdBlockerEngine.BRAVE_YOUTUBE_SHIELD_JS, null)
                                        }
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && viewModel.uiState.value.isForceDarkWebPages) {
                                            view?.evaluateJavascript(LEGACY_FORCE_DARK_CSS_JS, null)
                                        }
                                        view?.evaluateJavascript(MediaSniffer.JS_MEDIA_SCANNER, null)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        if (url.isNullOrBlank() || url.equals("about:blank", ignoreCase = true)) return
                                        lastLoadedProgrammaticUrl = url
                                        viewModel.onPageFinished(url, view?.title, appSettings.isAutoSnifferEnabled)
                                        viewModel.onNavigationStateChanged(canGoBack(), canGoForward())

                                        if (appSettings.isAdBlockerEnabled) {
                                            view?.evaluateJavascript(AdBlockerEngine.BRAVE_SHIELD_CSS_JS, null)
                                            view?.evaluateJavascript(AdBlockerEngine.BRAVE_YOUTUBE_SHIELD_JS, null)
                                        }
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && viewModel.uiState.value.isForceDarkWebPages) {
                                            view?.evaluateJavascript(LEGACY_FORCE_DARK_CSS_JS, null)
                                        }

                                        view?.evaluateJavascript(MediaSniffer.JS_MEDIA_SCANNER, null)
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        super.onProgressChanged(view, newProgress)
                                        viewModel.onProgressChanged(newProgress)
                                        viewModel.onNavigationStateChanged(canGoBack(), canGoForward())
                                    }

                                    override fun onCreateWindow(
                                        view: WebView?,
                                        isDialog: Boolean,
                                        isUserGesture: Boolean,
                                        resultMsg: android.os.Message?
                                    ): Boolean {
                                        if (appSettings.blockPopupsAndRedirects && !isUserGesture) {
                                            return false
                                        }
                                        if (isUserGesture && resultMsg != null) {
                                            val parentWebView = view ?: return false
                                            val transport = resultMsg.obj as? WebView.WebViewTransport
                                            val tempWebView = WebView(parentWebView.context)
                                            var destroyed = false
                                            val destroyTemp = {
                                                if (!destroyed) {
                                                    destroyed = true
                                                    tempWebView.stopLoading()
                                                    tempWebView.destroy()
                                                }
                                            }
                                            tempWebView.webViewClient = object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                                                    request?.url?.toString()?.let { targetUrl ->
                                                        lastLoadedProgrammaticUrl = targetUrl
                                                        parentWebView.loadUrl(targetUrl)
                                                    }
                                                    mainHandler.post(destroyTemp)
                                                    return true
                                                }
                                            }
                                            mainHandler.postDelayed(destroyTemp, 5000L)
                                            transport?.webView = tempWebView
                                            resultMsg.sendToTarget()
                                            return true
                                        }
                                        return false
                                    }
                                }

                                webViewInstance = this
                            }
                        },
                        update = { webView ->
                            @SuppressLint("SetJavaScriptEnabled")
                            webView.settings.javaScriptEnabled = uiState.isJavaScriptEnabled
                            webView.settings.loadWithOverviewMode = uiState.isDesktopMode
                            webView.settings.textZoom = uiState.textZoom
                            webView.settings.javaScriptCanOpenWindowsAutomatically = !appSettings.blockPopupsAndRedirects
                            webView.settings.setSupportMultipleWindows(!appSettings.blockPopupsAndRedirects)
                            runCatching {
                                CookieManager.getInstance().setAcceptThirdPartyCookies(
                                    webView,
                                    !appSettings.blockThirdPartyCookies
                                )
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                webView.settings.isAlgorithmicDarkeningAllowed = uiState.isForceDarkWebPages
                            }
                            if (uiState.isDesktopMode) {
                                webView.settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            } else {
                                webView.settings.userAgentString = null
                            }

                            val settingsChanged = appliedTracker.desktopMode != uiState.isDesktopMode ||
                                appliedTracker.jsEnabled != uiState.isJavaScriptEnabled ||
                                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && appliedTracker.forceDark != uiState.isForceDarkWebPages)

                            appliedTracker.desktopMode = uiState.isDesktopMode
                            appliedTracker.jsEnabled = uiState.isJavaScriptEnabled
                            appliedTracker.forceDark = uiState.isForceDarkWebPages

                            if (uiState.currentLoadedUrl != null && uiState.currentLoadedUrl != lastLoadedProgrammaticUrl) {
                                lastLoadedProgrammaticUrl = uiState.currentLoadedUrl
                                webView.loadUrl(uiState.currentLoadedUrl!!)
                            } else if (settingsChanged && uiState.currentLoadedUrl != null) {
                                webView.reload()
                            }
                        },
                        onRelease = { webView ->
                            mainHandler.removeCallbacksAndMessages(null)
                            webView.stopLoading()
                            webView.loadUrl("about:blank")
                            webView.onPause()
                            webView.removeJavascriptInterface("UdmMediaBridge")
                            webView.clearHistory()
                            webView.removeAllViews()
                            webView.destroy()
                            if (webViewInstance === webView) {
                                webViewInstance = null
                            }
                        }
                    )
                }
            }
        }

        // Clean Floating Media Ready Pill (sits neatly right above the bottom nav)
        val hasMedia = uiState.detectedMediaState != null
        val media = uiState.detectedMediaState
        val isExtracting = uiState.isExtractingMedia

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            AnimatedVisibility(
                visible = hasMedia || isExtracting,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(tween(160)) + scaleIn(initialScale = 0.94f),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(160)
                ) + fadeOut(tween(130)) + scaleOut(targetScale = 0.94f)
            ) {
                val colorScheme = MaterialTheme.colorScheme
                val extendedColors = UdmTheme.extendedColors
                if (isExtracting) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerHigh,
                        tonalElevation = 4.dp,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.primary
                            )
                            Text(
                                text = "Checking media...",
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.cancelSniffing()
                                    }
                            )
                        }
                    }
                } else if (media != null && (media.videoVariants.isNotEmpty() || media.audioVariants.isNotEmpty() || media.directFileUrl != null || media.directFileVariant != null)) {
                    val isDirectFile = media.isDirectFile || (media.videoVariants.isEmpty() && media.audioVariants.isEmpty())
                    val totalVariants = if (isDirectFile) 1 else (media.videoVariants.size + media.audioVariants.size)
                    val pillColor = if (isDirectFile) extendedColors.successContainer else colorScheme.primaryContainer
                    val pillOnColor = if (isDirectFile) extendedColors.onSuccessContainer else colorScheme.onPrimaryContainer

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.showBottomSheet()
                        },
                        shape = CircleShape,
                        color = pillColor,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = pillOnColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isDirectFile) "Download file" else "Download ($totalVariants options)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = pillOnColor
                            )
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.dismissDetectedMedia()
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = pillOnColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isTabSwitcherOpen) {
            TabSwitcherSheet(
                tabs = uiState.tabs,
                activeTabId = uiState.activeTabId,
                onDismiss = { viewModel.dismissTabSwitcher() },
                onSelectTab = { tabId -> viewModel.selectTab(tabId) },
                onCloseTab = { tabId -> viewModel.closeTab(tabId) },
                onOpenNewTab = { isIncognito -> viewModel.openNewTab(isIncognito) },
                onCloseAllTabs = { incognitoOnly -> viewModel.closeAllTabs(incognitoOnly) }
            )
        }

        if (uiState.isHistorySheetOpen) {
            BrowserHistorySheet(
                history = uiState.history,
                onDismiss = { viewModel.setHistorySheetOpen(false) },
                onOpenUrl = { url -> viewModel.loadUrl(url) },
                onOpenInNewTab = { url -> viewModel.openNewTab(isIncognito = uiState.isIncognito, initialUrl = url) },
                onDeleteItem = { id -> viewModel.deleteHistoryItem(id) },
                onClearHistory = {
                    viewModel.clearHistory()
                    scope.launch { snackbarHostState.showSnackbar("History cleared") }
                }
            )
        }

        if (uiState.isPageInfoDialogVisible && uiState.currentLoadedUrl != null) {
            PageInfoDialog(
                title = uiState.pageTitle,
                url = uiState.currentLoadedUrl!!,
                blockedAdsCount = uiState.blockedAdsCount,
                isShieldEnabled = appSettings.isAdBlockerEnabled,
                textZoom = uiState.textZoom,
                onAdjustZoom = { delta -> viewModel.adjustTextZoom(delta) },
                onResetZoom = { viewModel.resetTextZoom() },
                onDismiss = { viewModel.setPageInfoDialogVisible(false) },
                onCopyUrl = handleCopyCurrentUrl,
                onPinToFavourite = handlePinCurrentOrOpenDialog,
                onClearSiteData = handleClearSiteData
            )
        }

        if (uiState.isSearchEngineDialogVisible) {
            SearchEngineSelectionDialog(
                selectedEngine = uiState.selectedSearchEngine,
                onSelectEngine = { engine -> viewModel.selectSearchEngine(engine) },
                onDismiss = { viewModel.setSearchEngineDialogVisible(false) }
            )
        }

        if (uiState.isPinSiteDialogVisible) {
            PinFavouriteDialog(
                initialUrl = uiState.currentLoadedUrl,
                initialTitle = uiState.pageTitle,
                onDismiss = { viewModel.setPinSiteDialogVisible(false) },
                onPin = { title, url, colorHex ->
                    viewModel.pinFavourite(title, url, "Site", colorHex)
                    scope.launch {
                        snackbarHostState.showSnackbar("Bookmarked \"$title\"")
                    }
                }
            )
        }

        favouriteToRemove?.let { site ->
            RemoveFavouriteConfirmDialog(
                site = site,
                onDismiss = { favouriteToRemove = null },
                onConfirmRemove = {
                    viewModel.removeFavourite(site.id)
                    favouriteToRemove = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Removed \"${site.title}\"")
                    }
                }
            )
        }

        if (uiState.showMediaBottomSheet && uiState.detectedMediaState != null) {
            DownloadConfigBottomSheet(
                detectedMedia = uiState.detectedMediaState!!,
                onDismiss = { viewModel.hideBottomSheet() },
                onEnqueueDownload = { title, fileName, downloadUrl, pairedAudioUrl, mediaType, qualityLabel, codecLabel, threadCount, destinationFolder, totalBytes, sourcePageUrl, formatId ->
                    val capturedThumbUrl = uiState.detectedMediaState?.thumbnailUrl
                    scope.launch {
                        DownloadManager.getInstance(context).enqueueDownload(
                            title = title,
                            sourcePageUrl = if (sourcePageUrl.isNotBlank()) sourcePageUrl else (uiState.currentLoadedUrl ?: downloadUrl),
                            downloadUrl = downloadUrl,
                            pairedAudioUrl = pairedAudioUrl,
                            fileName = fileName,
                            destinationFolder = destinationFolder,
                            mediaType = mediaType,
                            qualityLabel = qualityLabel,
                            codecLabel = codecLabel,
                            formatId = formatId,
                            threadCount = threadCount,
                            totalBytes = totalBytes,
                            thumbnailUrl = capturedThumbUrl
                        )
                        snackbarHostState.showSnackbar("Downloading $fileName")
                    }
                    onDownloadTriggered(downloadUrl, title)
                    viewModel.hideBottomSheet()
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserTopBar(
    uiState: BrowserUiState,
    appSettingsAdBlocker: Boolean,
    onAdBlockerToggle: () -> Unit,
    onUrlChanged: (String) -> Unit,
    onGo: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onPinCurrentPage: () -> Unit,
    onOpenSearchEngineDialog: () -> Unit,
    onOpenPageInfo: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleTabs: () -> Unit,
    onToggleMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onNewTab: () -> Unit,
    onToggleIncognito: () -> Unit,
    onReopenClosedTab: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleForceDark: () -> Unit,
    onOpenFindInPage: () -> Unit,
    onFindQueryChanged: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    onCloseFindInPage: () -> Unit,
    onCopyUrl: () -> Unit,
    onShare: () -> Unit,
    onOpenExternalBrowser: () -> Unit,
    onClearSiteData: () -> Unit,
    onReloadOrStop: () -> Unit,
    onManualSniff: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = UdmTheme.extendedColors
    val isOnPage = uiState.currentLoadedUrl != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isOnPage) colorScheme.surface else colorScheme.background)
            .statusBarsPadding()
    ) {
        AnimatedVisibility(
            visible = uiState.isIncognito,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.tertiaryContainer)
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnLock,
                        contentDescription = null,
                        tint = colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Incognito",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onTertiaryContainer
                    )
                }
                Text(
                    text = "Exit",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.tertiary,
                    modifier = Modifier.clickable { onToggleIncognito() }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!isOnPage) {
                // Clean Home Header
                Text(
                    text = if (uiState.isIncognito) "Private" else "Browser",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onBackground,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )

                UdmHeaderActionButton(
                    icon = Icons.Default.History,
                    contentDescription = "History",
                    onClick = onOpenHistory
                )
            } else {
                // Clean Loaded Webpage Omnibar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = onBack,
                            onLongClick = onHome
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                var isEditingUrl by remember(uiState.currentLoadedUrl) { mutableStateOf(false) }
                val urlFocusRequester = remember { FocusRequester() }
                LaunchedEffect(isEditingUrl) {
                    if (isEditingUrl) {
                        runCatching { urlFocusRequester.requestFocus() }
                    }
                }

                val activeUrl = uiState.currentLoadedUrl
                val isHttps = activeUrl.startsWith("https://", ignoreCase = true)
                val displayHost = remember(activeUrl) {
                    runCatching {
                        URI(activeUrl).host?.removePrefix("www.")?.takeIf { it.isNotBlank() }
                    }.getOrNull() ?: activeUrl
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            uiState.isIncognito -> Icons.Default.VpnLock
                            isHttps -> Icons.Default.Lock
                            else -> Icons.Default.LockOpen
                        },
                        contentDescription = "Page info",
                        tint = when {
                            uiState.isIncognito -> colorScheme.tertiary
                            isHttps -> extendedColors.success
                            else -> colorScheme.error
                        },
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onOpenPageInfo() }
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        if (!isEditingUrl) {
                            Text(
                                text = displayHost,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isEditingUrl = true }
                            )
                        } else {
                            if (uiState.urlInput.isEmpty()) {
                                Text(
                                    text = "Search or enter URL",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                            BasicTextField(
                                value = uiState.urlInput,
                                onValueChange = onUrlChanged,
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = colorScheme.onSurface,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = {
                                    isEditingUrl = false
                                    onGo()
                                }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(urlFocusRequester)
                            )
                        }
                    }

                    if (isEditingUrl && uiState.urlInput.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onClear() }
                        )
                    } else if (!isEditingUrl) {
                        Icon(
                            imageVector = if (uiState.isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (uiState.isLoading) "Stop" else "Reload",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(17.dp)
                                .clickable { onReloadOrStop() }
                        )
                    }
                }

                // Compact Media Sniff Button
                val mediaState = uiState.detectedMediaState
                val isDirect = mediaState?.isDirectFile == true || (mediaState != null && mediaState.videoVariants.isEmpty() && mediaState.audioVariants.isEmpty())
                val totalStreamCount = if (isDirect) 1 else ((mediaState?.videoVariants?.size ?: 0) + (mediaState?.audioVariants?.size ?: 0))
                val hasStreams = totalStreamCount > 0
                val isExtracting = uiState.isExtractingMedia

                Surface(
                    onClick = onManualSniff,
                    shape = CircleShape,
                    color = when {
                        hasStreams -> colorScheme.primary
                        isExtracting -> colorScheme.primaryContainer
                        else -> colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isExtracting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download media",
                                tint = if (hasStreams) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        if (hasStreams) {
                            Text(
                                text = "$totalStreamCount",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            TabCountButton(
                tabCount = uiState.tabs.size,
                isIncognito = uiState.isIncognito,
                onClick = onToggleTabs
            )

            Box {
                UdmHeaderActionButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    onClick = onToggleMenu,
                    modifier = Modifier.size(if (isOnPage) 36.dp else 40.dp)
                )

                DropdownMenu(
                    expanded = uiState.isMenuExpanded,
                    onDismissRequest = onDismissMenu,
                    shape = RoundedCornerShape(20.dp),
                    containerColor = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.width(268.dp)
                ) {
                    // Quick Icon Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MenuQuickIcon(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            enabled = isOnPage && uiState.canGoForward,
                            onClick = onForward
                        )
                        MenuQuickIcon(
                            icon = if (uiState.isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = "Reload",
                            enabled = isOnPage,
                            onClick = onReloadOrStop
                        )
                        MenuQuickIcon(
                            icon = Icons.Default.Home,
                            contentDescription = "Home",
                            enabled = isOnPage,
                            onClick = onHome
                        )
                        MenuQuickIcon(
                            icon = Icons.Default.Star,
                            contentDescription = "Bookmark",
                            enabled = true,
                            onClick = onPinCurrentPage
                        )
                        MenuQuickIcon(
                            icon = Icons.Default.Share,
                            contentDescription = "Share",
                            enabled = isOnPage,
                            onClick = onShare
                        )
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.45f))

                    if (isOnPage) {
                        DropdownMenuItem(
                            text = { Text("Download media") },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = colorScheme.primary) },
                            onClick = {
                                onManualSniff()
                                onDismissMenu()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Find in page") },
                            leadingIcon = { Icon(Icons.Default.FindInPage, contentDescription = null) },
                            onClick = onOpenFindInPage
                        )
                        DropdownMenuItem(
                            text = { Text("Copy link") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = onCopyUrl
                        )
                        DropdownMenuItem(
                            text = { Text("Page info & zoom") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = onOpenPageInfo
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.45f))
                    }

                    DropdownMenuItem(
                        text = { Text("New tab") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = onNewTab
                    )
                    DropdownMenuItem(
                        text = { Text(if (uiState.isIncognito) "Exit incognito" else "New incognito tab") },
                        leadingIcon = { Icon(Icons.Default.VpnLock, contentDescription = null) },
                        onClick = onToggleIncognito
                    )
                    if (uiState.recentlyClosedTabs.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Reopen closed tab") },
                            leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                            onClick = onReopenClosedTab
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("History") },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                        onClick = onOpenHistory
                    )

                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.45f))

                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Desktop site")
                                Switch(checked = uiState.isDesktopMode, onCheckedChange = { onToggleDesktopMode() })
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.DesktopWindows, contentDescription = null) },
                        onClick = onToggleDesktopMode
                    )
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Dark pages")
                                Switch(checked = uiState.isForceDarkWebPages, onCheckedChange = { onToggleForceDark() })
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                        onClick = onToggleForceDark
                    )
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ad blocker")
                                Switch(checked = appSettingsAdBlocker, onCheckedChange = { onAdBlockerToggle() })
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null) },
                        onClick = onAdBlockerToggle
                    )

                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.45f))

                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Search engine")
                                Text(
                                    text = uiState.selectedSearchEngine.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.primary
                                )
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        onClick = onOpenSearchEngineDialog
                    )
                    if (isOnPage) {
                        DropdownMenuItem(
                            text = { Text("Open in browser") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                            onClick = onOpenExternalBrowser
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Clear cache & cookies", color = colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = colorScheme.error) },
                        onClick = onClearSiteData
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isOnPage && uiState.isFindInPageVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val findFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                runCatching { findFocusRequester.requestFocus() }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (uiState.findQuery.isEmpty()) {
                        Text(
                            text = "Find in page...",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = uiState.findQuery,
                        onValueChange = onFindQueryChanged,
                        singleLine = true,
                        textStyle = TextStyle(color = colorScheme.onSurface, fontSize = 13.sp),
                        cursorBrush = SolidColor(colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onFindNext(true) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(findFocusRequester)
                    )
                }

                if (uiState.findQuery.isNotEmpty()) {
                    Text(
                        text = "${uiState.findActiveMatch}/${uiState.findTotalMatches}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.findTotalMatches > 0) colorScheme.primary else colorScheme.error
                    )
                }

                IconButton(onClick = { onFindNext(false) }, enabled = uiState.findTotalMatches > 0, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous")
                }
                IconButton(onClick = { onFindNext(true) }, enabled = uiState.findTotalMatches > 0, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                }
                IconButton(onClick = onCloseFindInPage, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        val animatedProgress by animateFloatAsState(
            targetValue = if (uiState.isLoading) (uiState.progress / 100f).coerceIn(0.08f, 1f) else 0f,
            animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing),
            label = "PageLoadProgress"
        )
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(2.5.dp)
                        .background(colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun MenuQuickIcon(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) colorScheme.onSurface else colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun TabCountButton(
    tabCount: Int,
    isIncognito: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surfaceContainerHigh)
            .border(
                width = 1.5.dp,
                color = if (isIncognito) colorScheme.tertiary else colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$tabCount",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isIncognito) colorScheme.tertiary else colorScheme.onSurface
        )
    }
}

/**
 * Clean, airy Material 3 Expressive Browser Start Page.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserHomeDashboard(
    isIncognito: Boolean = false,
    selectedSearchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    favourites: List<FavouriteSite> = emptyList(),
    onSearchEngineClick: () -> Unit = {},
    onBookmarkClicked: (String) -> Unit,
    onPasteAndDownload: (String) -> Unit,
    onPinSiteClick: () -> Unit,
    onRemoveFavourite: (FavouriteSite) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    var searchInput by remember { mutableStateOf("") }

    val clipText = remember {
        runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
        }.getOrNull()
    }
    val hasClipboardUrl = !clipText.isNullOrBlank() &&
        (clipText.startsWith("http://", ignoreCase = true) || clipText.startsWith("https://", ignoreCase = true))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 1. Clean Hero Search Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            UdmIconSquircle(
                icon = if (isIncognito) Icons.Default.VpnLock else Icons.Default.Public,
                containerColor = if (isIncognito) colorScheme.tertiaryContainer else colorScheme.primaryContainer,
                contentColor = if (isIncognito) colorScheme.onTertiaryContainer else colorScheme.onPrimaryContainer,
                size = 56.dp,
                iconSize = 28.dp,
                cornerRadius = 18.dp
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                color = colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchInput.isEmpty()) {
                            Text(
                                text = "Search ${selectedSearchEngine.displayName} or enter URL",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colorScheme.onSurface,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                if (searchInput.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onBookmarkClicked(searchInput.trim())
                                }
                            }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (searchInput.isNotEmpty()) {
                        IconButton(
                            onClick = { searchInput = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBookmarkClicked(searchInput.trim())
                            },
                            shape = CircleShape,
                            color = colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Go",
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            onClick = onSearchEngineClick,
                            shape = CircleShape,
                            color = colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = selectedSearchEngine.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            if (hasClipboardUrl && searchInput.isEmpty() && clipText != null) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPasteAndDownload(clipText)
                    },
                    shape = CircleShape,
                    color = colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = clipText,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. Clean 4-Column Bookmarks Grid (No boxed cards, no spam tags)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Bookmarks",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            val allItems = favourites
            val rows = (allItems.size + 1 + 3) / 4

            for (rowIndex in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (colIndex in 0 until 4) {
                        val itemIndex = rowIndex * 4 + colIndex
                        if (itemIndex < allItems.size) {
                            val site = allItems[itemIndex]
                            val primaryFallback = colorScheme.primary
                            val isLightSurface = colorScheme.surface.luminance() > 0.5f
                            val accentColor = remember(site.colorHex, isLightSurface, primaryFallback) {
                                if (site.colorHex.equals("#00E5FF", ignoreCase = true)) {
                                    primaryFallback
                                } else {
                                    val parsed = parseHexColor(site.colorHex, primaryFallback)
                                    if (isLightSurface && parsed.luminance() > 0.65f) primaryFallback else parsed
                                }
                            }
                            val siteIcon = remember(site.url) { getFavouriteIcon(site.url) }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .combinedClickable(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onBookmarkClicked(site.url)
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onRemoveFavourite(site)
                                        }
                                    )
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colorScheme.surfaceContainerLow),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = siteIcon,
                                        contentDescription = site.title,
                                        tint = accentColor,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                                Text(
                                    text = site.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (itemIndex == allItems.size) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onPinSiteClick()
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add bookmark",
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = "Add",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun getFavouriteIcon(url: String): ImageVector {
    val lower = url.lowercase()
    return when {
        lower.contains("youtube") || lower.contains("youtu.be") -> Icons.Default.Sensors
        lower.contains("sound") || lower.contains("music") || lower.contains("spotify") -> Icons.Default.MusicNote
        lower.contains("vimeo") || lower.contains("movie") || lower.contains("film") -> Icons.Default.Movie
        lower.contains("dailymotion") || lower.contains("stream") -> Icons.Default.PlayArrow
        lower.contains("duckduckgo") || lower.contains("google") || lower.contains("search") -> Icons.Default.Search
        else -> Icons.Default.Language
    }
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFF006874)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun PinFavouriteDialog(
    initialUrl: String? = null,
    initialTitle: String? = null,
    onDismiss: () -> Unit,
    onPin: (title: String, url: String, colorHex: String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var title by remember { mutableStateOf(initialTitle ?: "") }
    var url by remember { mutableStateOf(initialUrl ?: "") }
    var selectedColor by remember { mutableStateOf("#00838F") }
    val haptic = LocalHapticFeedback.current

    val colors = listOf("#00838F", "#D32F2F", "#E65100", "#1565C0", "#6A1B9A", "#B26A00")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "Add bookmark",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. GitHub") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { hex ->
                        val parsed = parseHexColor(hex)
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedColor = hex
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (parsed.luminance() > 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val cleanTitle = title.ifBlank {
                            runCatching { URI(url).host?.removePrefix("www.") }.getOrNull() ?: "Site"
                        }
                        onPin(cleanTitle, url, selectedColor)
                    }
                },
                shape = CircleShape
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RemoveFavouriteConfirmDialog(
    site: FavouriteSite,
    onDismiss: () -> Unit,
    onConfirmRemove: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colorScheme.surfaceContainerHigh,
        title = { Text("Remove bookmark?", style = MaterialTheme.typography.titleMedium) },
        text = {
            Text(
                text = "Remove \"${site.title}\" from bookmarks?",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmRemove,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.error,
                    contentColor = colorScheme.onError
                )
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(name = "Browser Home - Light", showBackground = true)
@Composable
fun BrowserHomeDashboardLightPreview() {
    UDMTheme(darkTheme = false, dynamicColor = false) {
        BrowserHomeDashboard(
            isIncognito = false,
            selectedSearchEngine = SearchEngine.DUCKDUCKGO,
            favourites = listOf(
                FavouriteSite(title = "YouTube", url = "https://m.youtube.com", badge = "Video", colorHex = "#FF0033"),
                FavouriteSite(title = "Vimeo", url = "https://vimeo.com", badge = "Video", colorHex = "#00ADEF")
            ),
            onSearchEngineClick = {},
            onBookmarkClicked = {},
            onPasteAndDownload = {},
            onPinSiteClick = {},
            onRemoveFavourite = {}
        )
    }
}

@Preview(name = "Browser Home - Dark", showBackground = true)
@Composable
fun BrowserHomeDashboardDarkPreview() {
    UDMTheme(darkTheme = true, dynamicColor = false) {
        BrowserHomeDashboard(
            isIncognito = false,
            selectedSearchEngine = SearchEngine.DUCKDUCKGO,
            favourites = listOf(
                FavouriteSite(title = "YouTube", url = "https://m.youtube.com", badge = "Video", colorHex = "#FF0033"),
                FavouriteSite(title = "Vimeo", url = "https://vimeo.com", badge = "Video", colorHex = "#00ADEF")
            ),
            onSearchEngineClick = {},
            onBookmarkClicked = {},
            onPasteAndDownload = {},
            onPinSiteClick = {},
            onRemoveFavourite = {}
        )
    }
}
