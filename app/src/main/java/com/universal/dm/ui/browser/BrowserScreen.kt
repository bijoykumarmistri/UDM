package com.universal.dm.ui.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universal.dm.domain.adblock.AdBlockerEngine
import com.universal.dm.domain.downloader.DownloadManager
import com.universal.dm.domain.sniffer.MediaSniffer
import com.universal.dm.ui.settings.LocalAppSettings
import com.universal.dm.ui.theme.CodecAv1Badge
import com.universal.dm.ui.theme.CodecM4aBadge
import com.universal.dm.ui.theme.UDMTheme
import kotlinx.coroutines.launch

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
            Text("Browser & Media Sniffer Preview", style = MaterialTheme.typography.titleMedium)
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

    // Smart back navigation that escapes redirect loops (e.g. Vimeo) and returns home smoothly
    val handleSmartBackNavigation: () -> Unit = {
        when {
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
                        val curHost = curItem?.url?.let { runCatching { java.net.URI(it).host?.removePrefix("www.") }.getOrNull() }
                        val prevHost = prevItem?.url?.let { runCatching { java.net.URI(it).host?.removePrefix("www.") }.getOrNull() }
                        val prevUrl = prevItem?.url ?: ""

                        if (curHost == prevHost || prevUrl.contains("vimeo.com", ignoreCase = true)) {
                            // User originally opened Vimeo from home, and it redirected to index 1.
                            // Going back should directly return to Home dashboard!
                            webView.stopLoading()
                            webView.clearHistory()
                            webViewInstance = null
                            viewModel.clearToHome()
                        } else {
                            webView.goBack()
                        }
                    } else {
                        // Check if previous item was an auto-redirect
                        val curItem = history.getItemAtIndex(curIndex)
                        val prevItem = history.getItemAtIndex(curIndex - 1)
                        val curHost = curItem?.url?.let { runCatching { java.net.URI(it).host?.removePrefix("www.") }.getOrNull() }
                        val prevHost = prevItem?.url?.let { runCatching { java.net.URI(it).host?.removePrefix("www.") }.getOrNull() }

                        if (curHost != null && curHost == prevHost && curIndex >= 2) {
                            val prevPrevItem = history.getItemAtIndex(curIndex - 2)
                            val prevPrevHost = prevPrevItem?.url?.let { runCatching { java.net.URI(it).host?.removePrefix("www.") }.getOrNull() }
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

    // Intercept back presses in WebView
    BackHandler(enabled = uiState.currentLoadedUrl != null || uiState.isTabSwitcherOpen) {
        handleSmartBackNavigation()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Sleek Apple-Grade Glassmorphic Omnibar
            AppleOmnibar(
                uiState = uiState,
                appSettingsAdBlocker = appSettings.isAdBlockerEnabled,
                onAdBlockerToggle = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    appSettings.isAdBlockerEnabled = !appSettings.isAdBlockerEnabled
                },
                onUrlChanged = { viewModel.onUrlInputChanged(it) },
                onGo = { viewModel.loadUrl(uiState.urlInput) },
                onClear = { viewModel.onUrlInputChanged("") },
                onBack = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    handleSmartBackNavigation()
                },
                onHome = {
                    handleHomeNavigation()
                },
                onPinCurrentPage = {
                    uiState.currentLoadedUrl?.let { pageUrl ->
                        viewModel.pinFavourite(
                            title = uiState.pageTitle ?: "Pinned Site",
                            url = pageUrl,
                            badge = "Pinned"
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Pinned to Favourite!")
                        }
                    }
                },
                onOpenSearchEngineDialog = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setSearchEngineDialogVisible(true)
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
                onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                onToggleJavaScript = { viewModel.toggleJavaScript() },
                onShare = {
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
                },
                onReload = {
                    webViewInstance?.reload()
                    viewModel.dismissMenu()
                }
            )

            // 2. Viewport: Dashboard vs WebView
            if (uiState.currentLoadedUrl == null) {
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
                // Programmatic URL tracking to prevent in-page navigations (YouTube clicks) from rolling back
                var lastLoadedProgrammaticUrl by remember { mutableStateOf<String?>(null) }

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            @SuppressLint("SetJavaScriptEnabled")
                            settings.javaScriptEnabled = uiState.isJavaScriptEnabled
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.setSupportMultipleWindows(true)
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                            if (uiState.isDesktopMode) {
                                settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            }

                            if (uiState.isIncognito) {
                                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                clearHistory()
                                clearCache(true)
                            }

                            val mainHandler = Handler(Looper.getMainLooper())

                            // Add Javascript Interface for HTML5 video/audio DOM sniffing & SPA navigation
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onMediaFound(mediaUrl: String) {
                                    val currentMedia = viewModel.uiState.value.detectedMediaState
                                    val isChunkOrBlob = mediaUrl.startsWith("blob:") || mediaUrl.contains("videoplayback")
                                    if (currentMedia == null && !isChunkOrBlob) {
                                        mainHandler.post {
                                            viewModel.triggerMediaExtraction(mediaUrl, title)
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun onUrlChanged(newUrl: String) {
                                    mainHandler.post {
                                        lastLoadedProgrammaticUrl = newUrl
                                        viewModel.onPageStarted(newUrl)
                                    }
                                }
                            }, "UdmMediaBridge")

                            // Intercept standard downloads (e.g. .zip, .apk, .mp4)
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
                                    return runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW, request.url).apply {
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

                                    // Ad-blocker evaluation
                                    if (appSettings.isAdBlockerEnabled &&
                                        viewModel.adBlockerEngine.shouldBlockRequest(reqUrl)
                                    ) {
                                        mainHandler.post { viewModel.onAdBlocked() }
                                        return viewModel.adBlockerEngine.createEmptyResourceResponse()
                                    }

                                    // Sniff media stream requests (.m3u8, .mpd, .mp4, .m4a)
                                    val urlString = reqUrl.toString()
                                    val pageUrl = viewModel.uiState.value.currentLoadedUrl ?: ""
                                    val isOnKnownPlatform = viewModel.mediaSniffer.isSupportedPlatformUrl(pageUrl)

                                    if (!isOnKnownPlatform && viewModel.mediaSniffer.isMediaOrDownloadableUrl(urlString)) {
                                        val currentMedia = viewModel.uiState.value.detectedMediaState
                                        val isChunkOrSegment = urlString.contains("videoplayback") || urlString.contains("range=") || urlString.contains(".ts")
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
                                    viewModel.onPageStarted(url)
                                    viewModel.onNavigationStateChanged(canGoBack(), canGoForward())
                                }

                                override fun onPageCommitVisible(view: WebView?, url: String?) {
                                    super.onPageCommitVisible(view, url)
                                    if (appSettings.isAdBlockerEnabled) {
                                        view?.evaluateJavascript(AdBlockerEngine.BRAVE_SHIELD_CSS_JS, null)
                                        view?.evaluateJavascript(AdBlockerEngine.BRAVE_YOUTUBE_SHIELD_JS, null)
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (url.isNullOrBlank() || url.equals("about:blank", ignoreCase = true)) return
                                    lastLoadedProgrammaticUrl = url
                                    viewModel.onPageFinished(url, view?.title)
                                    viewModel.onNavigationStateChanged(canGoBack(), canGoForward())

                                    if (appSettings.isAdBlockerEnabled) {
                                        view?.evaluateJavascript(AdBlockerEngine.BRAVE_SHIELD_CSS_JS, null)
                                        view?.evaluateJavascript(AdBlockerEngine.BRAVE_YOUTUBE_SHIELD_JS, null)
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
                                    if (isUserGesture && resultMsg != null) {
                                        val transport = resultMsg.obj as? WebView.WebViewTransport
                                        val tempWebView = WebView(view?.context ?: return false).apply {
                                            webViewClient = object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                                                    request?.url?.toString()?.let { targetUrl ->
                                                        lastLoadedProgrammaticUrl = targetUrl
                                                        view.loadUrl(targetUrl)
                                                    }
                                                    return true
                                                }
                                            }
                                        }
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
                        webView.settings.javaScriptEnabled = uiState.isJavaScriptEnabled
                        if (uiState.isDesktopMode) {
                            webView.settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        } else {
                            webView.settings.userAgentString = null
                        }

                        if (uiState.currentLoadedUrl != null && uiState.currentLoadedUrl != lastLoadedProgrammaticUrl) {
                            lastLoadedProgrammaticUrl = uiState.currentLoadedUrl
                            webView.loadUrl(uiState.currentLoadedUrl!!)
                        }
                    }
                )
            }
        }

        // 3. Apple Dynamic Island Media Sniffer Banner (Floats cleanly above the bottom navigation dock)
        val hasMedia = uiState.detectedMediaState != null
        val media = uiState.detectedMediaState
        val isExtracting = uiState.isExtractingMedia

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp, start = 16.dp, end = 16.dp)
        ) {
            AnimatedVisibility(
                visible = hasMedia || isExtracting,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn() + scaleIn(initialScale = 0.92f),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut() + scaleOut(targetScale = 0.92f)
            ) {
                if (isExtracting) {
                    // Extracting Progress Pill
                    Box(
                        modifier = Modifier
                            .shadow(16.dp, CircleShape, spotColor = Color(0x99000000))
                            .clip(CircleShape)
                            .background(Color(0xF5081520))
                            .border(1.dp, Color(0xFF00B4D8).copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF00E5FF)
                            )
                            Text(
                                text = "Sniffing Media Streams...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                } else if (media != null) {
                    // Media Detected Dynamic Island
                    val totalVariants = media.videoVariants.size + media.audioVariants.size
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val islandScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                        label = "IslandScale"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = islandScale
                                scaleY = islandScale
                            }
                            .shadow(24.dp, CircleShape, spotColor = Color(0xAA000000), ambientColor = Color(0x4400E5FF))
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xF508202C),
                                        Color(0xF505151F)
                                    )
                                )
                            )
                            .border(1.5.dp, Color(0xFF00E5FF), CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.showBottomSheet()
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Streams",
                                    tint = Color(0xFF001F24),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$totalVariants Streams Available",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "READY",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF00E5FF)
                                        )
                                    }
                                }
                                Text(
                                    text = "Tap to configure formats & download",
                                    fontSize = 11.sp,
                                    color = Color(0xFF90A4AE)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Tab Switcher Sheet
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

        // 5. Search Engine Selection Dialog
        if (uiState.isSearchEngineDialogVisible) {
            SearchEngineSelectionDialog(
                selectedEngine = uiState.selectedSearchEngine,
                onSelectEngine = { engine -> viewModel.selectSearchEngine(engine) },
                onDismiss = { viewModel.setSearchEngineDialogVisible(false) }
            )
        }

        // 6. Pin Favourite Dialog
        if (uiState.isPinSiteDialogVisible) {
            PinFavouriteDialog(
                onDismiss = { viewModel.setPinSiteDialogVisible(false) },
                onPin = { title, url, colorHex ->
                    viewModel.pinFavourite(title, url, "Pinned", colorHex)
                    scope.launch {
                        snackbarHostState.showSnackbar("Pinned \"$title\" to Favourite")
                    }
                }
            )
        }

        // 7. Remove Favourite Confirmation Dialog
        favouriteToRemove?.let { site ->
            RemoveFavouriteConfirmDialog(
                site = site,
                onDismiss = { favouriteToRemove = null },
                onConfirmRemove = {
                    viewModel.removeFavourite(site.id)
                    favouriteToRemove = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Removed \"${site.title}\" from Favourite")
                    }
                }
            )
        }

        // 6. Download Configuration Sheet
        if (uiState.showMediaBottomSheet && uiState.detectedMediaState != null) {
            DownloadConfigBottomSheet(
                detectedMedia = uiState.detectedMediaState!!,
                onDismiss = { viewModel.hideBottomSheet() },
                onEnqueueDownload = { title, fileName, downloadUrl, pairedAudioUrl, mediaType, qualityLabel, codecLabel, threadCount, destinationFolder, totalBytes, sourcePageUrl ->
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
                            threadCount = threadCount,
                            totalBytes = totalBytes
                        )
                        snackbarHostState.showSnackbar("Enqueued: $fileName ($qualityLabel • $codecLabel)")
                    }
                    onDownloadTriggered(downloadUrl, title)
                    viewModel.hideBottomSheet()
                }
            )
        }

        // Snackbar Host for download confirmations
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}

/**
 * Apple-grade Glassmorphic Omnibar Composable.
 */
@Composable
fun AppleOmnibar(
    uiState: BrowserUiState,
    appSettingsAdBlocker: Boolean,
    onAdBlockerToggle: () -> Unit,
    onUrlChanged: (String) -> Unit,
    onGo: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onPinCurrentPage: () -> Unit,
    onOpenSearchEngineDialog: () -> Unit,
    onToggleTabs: () -> Unit,
    onToggleMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onNewTab: () -> Unit,
    onToggleIncognito: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleJavaScript: () -> Unit,
    onShare: () -> Unit,
    onReload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF071118))
    ) {
        // Incognito Banner
        AnimatedVisibility(
            visible = uiState.isIncognito,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x338B5CF6))
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
                        tint = Color(0xFFCFBCFF),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Incognito Tab • History & cookies isolated",
                        fontSize = 11.5.sp,
                        color = Color(0xFFCFBCFF),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Leave",
                    fontSize = 11.5.sp,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onToggleIncognito() }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.currentLoadedUrl != null) {
                // Back Button (with Long Press for Home)
                AnimatedIconButton(
                    onClick = onBack,
                    onLongClick = onHome,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back (Hold for Home)",
                    tint = if (uiState.isIncognito) Color(0xFFCFBCFF) else Color(0xFF00E5FF)
                )

                // Dedicated Home Button
                AnimatedIconButton(
                    onClick = onHome,
                    icon = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = Color(0xFF90A4AE)
                )
            } else {
                // Home indicator on dashboard
                AnimatedIconButton(
                    onClick = onHome,
                    icon = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = if (uiState.isIncognito) Color(0xFFCFBCFF) else Color(0xFF00E5FF)
                )
            }

            // Capsule Omnibar Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D1B26))
                    .border(
                        width = 1.dp,
                        color = if (uiState.isIncognito) Color(0xFF8B5CF6).copy(alpha = 0.5f) else Color(0xFF142B39),
                        shape = CircleShape
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Search Engine icon button with tap animation
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF07121A))
                            .clickable { onOpenSearchEngineDialog() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.currentLoadedUrl != null) Icons.Default.Lock else Icons.Default.Search,
                            contentDescription = "Search Engine: ${uiState.selectedSearchEngine.displayName}",
                            tint = if (uiState.isIncognito) Color(0xFFCFBCFF) else Color(0xFF00E5FF),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.urlInput.isEmpty()) {
                            Text(
                                text = uiState.selectedSearchEngine.hint,
                                color = Color(0xFF6B8092),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        BasicTextField(
                            value = uiState.urlInput,
                            onValueChange = onUrlChanged,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color(0xFF00E5FF)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { onGo() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Clear button animated
                    AnimatedVisibility(
                        visible = uiState.urlInput.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF90A4AE),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Ad-Block Shield Pill
            AdBlockShieldPill(
                isEnabled = appSettingsAdBlocker,
                blockedCount = uiState.blockedAdsCount,
                onClick = onAdBlockerToggle
            )

            // Tab Switcher Pill Button
            TabCountButton(
                tabCount = uiState.tabs.size,
                isIncognito = uiState.isIncognito,
                onClick = onToggleTabs
            )

            // More Options Dropdown
            Box {
                AnimatedIconButton(
                    onClick = onToggleMenu,
                    icon = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = Color(0xFF90A4AE)
                )

                DropdownMenu(
                    expanded = uiState.isMenuExpanded,
                    onDismissRequest = onDismissMenu,
                    modifier = Modifier
                        .background(Color(0xFF0B1721))
                        .border(1.dp, Color(0xFF142B39), RoundedCornerShape(16.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Home / Dashboard", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF00E5FF)) },
                        onClick = {
                            onHome()
                            onDismissMenu()
                        }
                    )
                    if (uiState.currentLoadedUrl != null) {
                        DropdownMenuItem(
                            text = { Text("Pin to Favourite", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF00E5FF)) },
                            onClick = {
                                onPinCurrentPage()
                                onDismissMenu()
                            }
                        )
                    }
                    HorizontalDivider(color = Color(0xFF142B39))
                    DropdownMenuItem(
                        text = { Text("New Tab", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00E5FF)) },
                        onClick = onNewTab
                    )
                    DropdownMenuItem(
                        text = { Text(if (uiState.isIncognito) "Exit Incognito Tab" else "New Incognito Tab", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.VpnLock, contentDescription = null, tint = Color(0xFFCFBCFF)) },
                        onClick = onToggleIncognito
                    )
                    HorizontalDivider(color = Color(0xFF142B39))
                    DropdownMenuItem(
                        text = { Text("Search Engine: ${uiState.selectedSearchEngine.displayName}", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF)) },
                        onClick = onOpenSearchEngineDialog
                    )
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Desktop Mode", color = Color.White)
                                Switch(
                                    checked = uiState.isDesktopMode,
                                    onCheckedChange = { onToggleDesktopMode() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00E5FF),
                                        checkedTrackColor = Color(0xFF004D59)
                                    )
                                )
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = Color(0xFF90A4AE)) },
                        onClick = onToggleDesktopMode
                    )
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("JavaScript", color = Color.White)
                                Switch(
                                    checked = uiState.isJavaScriptEnabled,
                                    onCheckedChange = { onToggleJavaScript() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00E5FF),
                                        checkedTrackColor = Color(0xFF004D59)
                                    )
                                )
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Javascript, contentDescription = null, tint = Color(0xFF90A4AE)) },
                        onClick = onToggleJavaScript
                    )
                    HorizontalDivider(color = Color(0xFF142B39))
                    DropdownMenuItem(
                        text = { Text("Share Page", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF90A4AE)) },
                        onClick = onShare,
                        enabled = uiState.currentLoadedUrl != null
                    )
                    DropdownMenuItem(
                        text = { Text("Reload", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF90A4AE)) },
                        onClick = onReload,
                        enabled = uiState.currentLoadedUrl != null
                    )
                }
            }
        }

        // Sleek Gradient Progress Indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color(0xFF071118))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(uiState.progress / 100f)
                        .height(2.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF0077B6)
                                )
                            )
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "ButtonScale"
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun AdBlockShieldPill(
    isEnabled: Boolean,
    blockedCount: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "ShieldScale"
    )

    Box(
        modifier = Modifier
            .height(34.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(if (isEnabled) Color(0x33005B66) else Color(0xFF0E1A24))
            .border(
                width = 1.dp,
                color = if (isEnabled) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color(0xFF1B3242),
                shape = CircleShape
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Shield",
                tint = if (isEnabled) Color(0xFF00E5FF) else Color(0xFF6B8092),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isEnabled) "$blockedCount" else "Off",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) Color(0xFF00E5FF) else Color(0xFF6B8092)
            )
        }
    }
}

@Composable
fun TabCountButton(
    tabCount: Int,
    isIncognito: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "TabScale"
    )

    Box(
        modifier = Modifier
            .size(34.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0E1A24))
            .border(
                width = 1.dp,
                color = if (isIncognito) Color(0xFF8B5CF6) else Color(0xFF00A8C6),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$tabCount",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = if (isIncognito) Color(0xFFCFBCFF) else Color(0xFF00E5FF)
        )
    }
}

/**
 * World-Class Redesigned Browser Home Dashboard.
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
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var directLinkInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Brand Header
        if (isIncognito) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x228B5CF6))
                    .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnLock,
                        contentDescription = null,
                        tint = Color(0xFFCFBCFF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Incognito Tab • History, cookies & caches will be discarded on tab close.",
                        fontSize = 12.sp,
                        color = Color(0xFFCFBCFF),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 2. Refined Typographic Header with Quick Search Engine Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Start Page",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Fast media sniffer & ad-free streaming",
                    fontSize = 12.sp,
                    color = Color(0xFF78909C)
                )
            }

            // Sleek Search Engine Quick-Switch Pill
            val engineInteraction = remember { MutableInteractionSource() }
            val isEnginePressed by engineInteraction.collectIsPressedAsState()
            val engineScale by animateFloatAsState(
                targetValue = if (isEnginePressed) 0.92f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "EngineScale"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = engineScale
                        scaleY = engineScale
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF091622))
                    .border(1.dp, Color(0xFF142B39), CircleShape)
                    .clickable(interactionSource = engineInteraction, indication = null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSearchEngineClick()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = selectedSearchEngine.displayName,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // 3. Apple-Grade Quick Stream Sniffer & Paste Hub
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF08141E)),
            border = BorderStroke(1.dp, Color(0xFF132B39))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Stream Sniffer & Downloader",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                    }

                    // Quick Paste chip from clipboard
                    val clipText = clipboardManager.getText()?.text
                    if (!clipText.isNullOrBlank() && (clipText.startsWith("http://") || clipText.startsWith("https://"))) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x3300E5FF))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    directLinkInput = clipText
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Paste Link",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        }
                    }
                }

                // Input + Sniff Capsule Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF050E15))
                        .border(1.dp, Color(0xFF163246), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (directLinkInput.isEmpty()) {
                            Text(
                                text = "Paste video, audio, or web URL...",
                                color = Color(0xFF6B8092),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        BasicTextField(
                            value = directLinkInput,
                            onValueChange = { directLinkInput = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color(0xFF00E5FF)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                if (directLinkInput.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPasteAndDownload(directLinkInput)
                                }
                            }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (directLinkInput.isNotEmpty()) {
                        IconButton(
                            onClick = { directLinkInput = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF90A4AE),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Sniff Action Pill
                    val sniffInteraction = remember { MutableInteractionSource() }
                    val isSniffPressed by sniffInteraction.collectIsPressedAsState()
                    val sniffScale by animateFloatAsState(
                        targetValue = if (isSniffPressed) 0.90f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                        label = "SniffScale"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = sniffScale
                                scaleY = sniffScale
                            }
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFF0091EA)
                                    )
                                )
                            )
                            .clickable(interactionSource = sniffInteraction, indication = null) {
                                if (directLinkInput.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPasteAndDownload(directLinkInput)
                                }
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF001F24),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Sniff",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = Color(0xFF001F24)
                            )
                        }
                    }
                }
            }
        }

        // Favourite Section (User Customized Speed-Dial Portals)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Favourite",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D2535))
                            .border(1.dp, Color(0xFF1B445E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${favourites.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }

                // Pin Site pill button
                val pinInteraction = remember { MutableInteractionSource() }
                val isPinPressed by pinInteraction.collectIsPressedAsState()
                val pinScale by animateFloatAsState(
                    targetValue = if (isPinPressed) 0.92f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    label = "PinScale"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = pinScale
                            scaleY = pinScale
                        }
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.45f), CircleShape)
                        .clickable(interactionSource = pinInteraction, indication = null) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPinSiteClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Pin Site",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Pin Site",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
            }

            if (favourites.isEmpty()) {
                // Empty state card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPinSiteClick() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF091622)),
                    border = BorderStroke(1.dp, Color(0xFF142B39))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No favourite sites pinned yet",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Tap here to pin your favourite streaming portals & websites",
                            fontSize = 11.5.sp,
                            color = Color(0xFF889DA8)
                        )
                    }
                }
            } else {
                // Responsive grid: 3 columns
                val rowCount = (favourites.size + 2) / 3
                val gridHeight = (rowCount * 115).dp

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight),
                    userScrollEnabled = false,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(favourites, key = { it.id }) { site ->
                        val siteInteraction = remember { MutableInteractionSource() }
                        val isSitePressed by siteInteraction.collectIsPressedAsState()
                        val siteScale by animateFloatAsState(
                            targetValue = if (isSitePressed) 0.90f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            label = "SiteScale"
                        )
                        val accentColor = remember(site.colorHex) { parseHexColor(site.colorHex) }
                        val siteIcon = remember(site.url) { getFavouriteIcon(site.url) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = siteScale
                                    scaleY = siteScale
                                }
                                .clip(RoundedCornerShape(18.dp))
                                .combinedClickable(
                                    interactionSource = siteInteraction,
                                    indication = null,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onBookmarkClicked(site.url)
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onRemoveFavourite(site)
                                    }
                                ),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF091622)),
                            border = BorderStroke(1.dp, Color(0xFF142B39))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.15f))
                                            .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = siteIcon,
                                            contentDescription = site.title,
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = site.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = site.badge,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E5FF),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Quick Delete button in corner
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onRemoveFavourite(site)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color(0xFF546E7A),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Privacy & Ad-Block Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF08141E)),
            border = BorderStroke(1.dp, Color(0xFF132B39))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF003847))
                        .border(1.dp, Color(0xFF00E5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Brave Shields & DNS Filter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "In-memory request filtering blocks intrusive trackers & pop-up spam in real-time.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF78909C)
                    )
                }
            }
        }

        // Bottom space so content is never obscured by the floating bottom navigation bar
        Spacer(modifier = Modifier.height(96.dp))
    }
}

/**
 * Helper to identify popular platforms or provide a clean default icon.
 */
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

/**
 * Safely parse a color hex string like #00E5FF into a Jetpack Compose Color.
 */
fun parseHexColor(hex: String, fallback: Color = Color(0xFF00E5FF)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

/**
 * Glassmorphic Dialog to Pin a custom site into Favourites.
 */
@Composable
fun PinFavouriteDialog(
    initialUrl: String? = null,
    initialTitle: String? = null,
    onDismiss: () -> Unit,
    onPin: (title: String, url: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle ?: "") }
    var url by remember { mutableStateOf(initialUrl ?: "") }
    var selectedColor by remember { mutableStateOf("#00E5FF") }
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    val colors = listOf(
        "#00E5FF" to "Neon Cyan",
        "#FF0033" to "Crimson",
        "#FF5500" to "Orange",
        "#0066DC" to "Blue",
        "#8B5CF6" to "Purple",
        "#EAA610" to "Gold"
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF081521),
            border = BorderStroke(1.dp, Color(0xFF142B39)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Pin to Favourite",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Speed-dial shortcut on browser home",
                                fontSize = 11.5.sp,
                                color = Color(0xFF889DA8)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF90A4AE),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Site Name Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Site Name",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. Vimeo, Reddit, GitHub...", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF050E15),
                            unfocusedContainerColor = Color(0xFF050E15),
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF1A3547),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // URL Input with Paste Shortcut
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Site URL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                        val clip = clipboardManager.getText()?.text
                        if (!clip.isNullOrBlank() && (clip.startsWith("http://") || clip.startsWith("https://") || clip.contains("."))) {
                            Text(
                                text = "Paste Clipboard",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    url = clip
                                    if (title.isBlank()) {
                                        title = runCatching { java.net.URI(clip).host?.removePrefix("www.")?.substringBefore(".")?.replaceFirstChar { it.uppercase() } }.getOrNull() ?: ""
                                    }
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = { Text("https://example.com", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF050E15),
                            unfocusedContainerColor = Color(0xFF050E15),
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF1A3547),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Radiant Color Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Accent Color",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90A4AE)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colors.forEach { (hex, _) ->
                            val parsed = parseHexColor(hex)
                            val isSelected = selectedColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
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
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFF0091EA)
                                    )
                                )
                            )
                            .clickable {
                                if (url.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val cleanTitle = title.ifBlank {
                                        runCatching { java.net.URI(url).host?.removePrefix("www.") }.getOrNull() ?: "Site"
                                    }
                                    onPin(cleanTitle, url, selectedColor)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pin to Favourite",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF001F24)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Confirmation dialog before removing a site from Favourites.
 */
@Composable
fun RemoveFavouriteConfirmDialog(
    site: FavouriteSite,
    onDismiss: () -> Unit,
    onConfirmRemove: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B1721),
            border = BorderStroke(1.dp, Color(0xFF142B39)),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FF0033)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Remove from Favourite?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Text(
                    text = "Do you want to remove \"${site.title}\" (${site.url}) from your Favourite speed-dial list?",
                    fontSize = 12.5.sp,
                    color = Color(0xFF90A4AE)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFD32F2F))
                            .clickable { onConfirmRemove() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Remove",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF071118)
@Composable
fun BrowserHomeDashboardPreview() {
    UDMTheme {
        BrowserHomeDashboard(
            isIncognito = false,
            selectedSearchEngine = SearchEngine.DUCKDUCKGO,
            favourites = listOf(
                FavouriteSite(title = "YouTube", url = "https://m.youtube.com", badge = "4K AV1", colorHex = "#FF0033"),
                FavouriteSite(title = "Vimeo", url = "https://vimeo.com", badge = "1080p Studio", colorHex = "#00ADEF")
            ),
            onSearchEngineClick = {},
            onBookmarkClicked = {},
            onPasteAndDownload = {},
            onPinSiteClick = {},
            onRemoveFavourite = {}
        )
    }
}
