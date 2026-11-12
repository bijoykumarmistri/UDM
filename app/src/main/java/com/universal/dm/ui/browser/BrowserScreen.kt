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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universal.dm.domain.adblock.AdBlockerEngine
import com.universal.dm.domain.downloader.DownloadManager
import com.universal.dm.domain.sniffer.MediaSniffer
import com.universal.dm.ui.components.UdmBrandLogo
import com.universal.dm.ui.components.UdmHeroHeader
import com.universal.dm.ui.settings.LocalAppSettings
import com.universal.dm.ui.theme.CodecAv1Badge
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
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            viewModel.handleSharedUrl(sharedUrl)
            onSharedUrlHandled()
        }
    }

    // Intercept back presses in WebView
    BackHandler(enabled = uiState.currentLoadedUrl != null || uiState.isTabSwitcherOpen) {
        when {
            uiState.isTabSwitcherOpen -> viewModel.toggleTabSwitcher()
            webViewInstance?.canGoBack() == true -> webViewInstance?.goBack()
            else -> viewModel.clearToHome()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Sleek Material You Omnibar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (uiState.isIncognito) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Incognito Notice Banner if in Incognito Mode
                    if (uiState.isIncognito) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CodecAv1Badge.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
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
                                        tint = CodecAv1Badge,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Incognito Mode • Browsing history & cookies not saved",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CodecAv1Badge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = "Leave",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { viewModel.setIncognitoMode(false) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Home or Back Navigation Button
                        if (uiState.currentLoadedUrl != null) {
                            IconButton(
                                onClick = {
                                    if (webViewInstance?.canGoBack() == true) {
                                        webViewInstance?.goBack()
                                    } else {
                                        viewModel.clearToHome()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.clearToHome() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home",
                                    tint = if (uiState.isIncognito) CodecAv1Badge else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Omnibar Search & URL Field
                        OutlinedTextField(
                            value = uiState.urlInput,
                            onValueChange = { viewModel.onUrlInputChanged(it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            placeholder = {
                                Text(
                                    text = uiState.selectedSearchEngine.hint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                IconButton(
                                    onClick = { viewModel.setSearchEngineDialogVisible(true) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.currentLoadedUrl != null) Icons.Default.Lock else Icons.Default.Search,
                                        contentDescription = "Change Search Engine",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (uiState.isIncognito) CodecAv1Badge else MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            trailingIcon = {
                                if (uiState.urlInput.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onUrlInputChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(25.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                viewModel.loadUrl(uiState.urlInput)
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedBorderColor = if (uiState.isIncognito) CodecAv1Badge else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        // Ad-Block Shield Chip
                        FilterChip(
                            selected = appSettings.isAdBlockerEnabled,
                            onClick = { appSettings.isAdBlockerEnabled = !appSettings.isAdBlockerEnabled },
                            label = {
                                Text(
                                    text = if (appSettings.isAdBlockerEnabled) "${uiState.blockedAdsCount}" else "Off",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Ad-Blocker",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (appSettings.isAdBlockerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            },
                            shape = MaterialTheme.shapes.medium,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )

                        // Tab Switcher Button with Active Tab Count Badge
                        IconButton(
                            onClick = { viewModel.toggleTabSwitcher() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = if (uiState.isIncognito) CodecAv1Badge else MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("${uiState.tabs.size}")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tab,
                                    contentDescription = "Tabs",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Browser More Options Menu
                        Box {
                            IconButton(
                                onClick = { viewModel.toggleMenu() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = uiState.isMenuExpanded,
                                onDismissRequest = { viewModel.dismissMenu() }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("New Tab") },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                    onClick = {
                                        viewModel.openNewTab(isIncognito = false)
                                        viewModel.dismissMenu()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (uiState.isIncognito) "Exit Incognito Tab" else "New Incognito Tab") },
                                    leadingIcon = { Icon(Icons.Default.VpnLock, contentDescription = null, tint = CodecAv1Badge) },
                                    onClick = {
                                        viewModel.setIncognitoMode(!uiState.isIncognito)
                                        viewModel.dismissMenu()
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Search Engine: ${uiState.selectedSearchEngine.displayName}") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    onClick = {
                                        viewModel.setSearchEngineDialogVisible(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Desktop Mode")
                                            Switch(
                                                checked = uiState.isDesktopMode,
                                                onCheckedChange = { viewModel.toggleDesktopMode() },
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.DesktopWindows, contentDescription = null) },
                                    onClick = { viewModel.toggleDesktopMode() }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("JavaScript")
                                            Switch(
                                                checked = uiState.isJavaScriptEnabled,
                                                onCheckedChange = { viewModel.toggleJavaScript() },
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Javascript, contentDescription = null) },
                                    onClick = { viewModel.toggleJavaScript() }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Share Page") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        val url = uiState.currentLoadedUrl
                                        if (!url.isNullOrBlank()) {
                                            val sendIntent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND
                                                putExtra(android.content.Intent.EXTRA_TEXT, url)
                                                type = "text/plain"
                                            }
                                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Link"))
                                        }
                                        viewModel.dismissMenu()
                                    },
                                    enabled = uiState.currentLoadedUrl != null
                                )
                                DropdownMenuItem(
                                    text = { Text("Reload") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    onClick = {
                                        webViewInstance?.reload()
                                        viewModel.dismissMenu()
                                    },
                                    enabled = uiState.currentLoadedUrl != null
                                )
                            }
                        }
                    }

                    // Loading progress indicator
                    if (uiState.isLoading) {
                        LinearProgressIndicator(
                            progress = { uiState.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = if (uiState.isIncognito) CodecAv1Badge else MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }

            // 2. Viewport: Dashboard vs WebView
            if (uiState.currentLoadedUrl == null) {
                BrowserHomeDashboard(
                    isIncognito = uiState.isIncognito,
                    selectedSearchEngine = uiState.selectedSearchEngine,
                    onSearchEngineClick = { viewModel.setSearchEngineDialogVisible(true) },
                    onBookmarkClicked = { url -> viewModel.loadUrl(url) },
                    onPasteAndDownload = { pasteUrl ->
                        viewModel.onUrlInputChanged(pasteUrl)
                        viewModel.loadUrl(pasteUrl)
                    }
                )
            } else {
                // Track last URL loaded from the user or address bar so in-page navigations (YouTube video clicks) are never rolled back
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
                                // Disable caching and cookies in incognito mode
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
                                        return false // Let WebView load HTTP/HTTPS links internally
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
                                    // Guard: Do not intercept subordinate requests when on a known platform (YouTube, etc.) where page URL is the source
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
                                    url?.let {
                                        lastLoadedProgrammaticUrl = it
                                        viewModel.onPageStarted(it)
                                    }
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
                                    url?.let {
                                        lastLoadedProgrammaticUrl = it
                                        viewModel.onPageFinished(it, view?.title)
                                    }
                                    viewModel.onNavigationStateChanged(canGoBack(), canGoForward())

                                    // Brave Shields cosmetic & scriptlet injection
                                    if (appSettings.isAdBlockerEnabled) {
                                        view?.evaluateJavascript(AdBlockerEngine.BRAVE_SHIELD_CSS_JS, null)
                                        view?.evaluateJavascript(AdBlockerEngine.BRAVE_YOUTUBE_SHIELD_JS, null)
                                    }

                                    // Inject DOM media scanner for embedded video/audio elements & SPA navigation hook
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
                        // Dynamically update JavaScript toggle and Desktop mode user-agent
                        webView.settings.javaScriptEnabled = uiState.isJavaScriptEnabled
                        if (uiState.isDesktopMode) {
                            webView.settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        } else {
                            webView.settings.userAgentString = null
                        }

                        // Only load URL when user or ViewModel issues a new programmatic URL command
                        if (uiState.currentLoadedUrl != null && uiState.currentLoadedUrl != lastLoadedProgrammaticUrl) {
                            lastLoadedProgrammaticUrl = uiState.currentLoadedUrl
                            webView.loadUrl(uiState.currentLoadedUrl!!)
                        }
                    }
                )
            }
        }

        // 3. Floating Action Button: Download Media Found vs Paste URL
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            AnimatedContent(
                targetState = Triple(
                    uiState.detectedMediaState != null,
                    uiState.isExtractingMedia,
                    uiState.detectedMediaState
                ),
                label = "FabStateTransition"
            ) { (hasMedia, isExtracting, media) ->
                when {
                    isExtracting -> {
                        ExtendedFloatingActionButton(
                            onClick = {},
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.large,
                            icon = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            text = { Text("Sniffing Streams...") }
                        )
                    }

                    hasMedia && media != null -> {
                        val totalVariants = media.videoVariants.size + media.audioVariants.size
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.showBottomSheet() },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = MaterialTheme.shapes.large,
                            icon = {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.tertiary,
                                            contentColor = MaterialTheme.colorScheme.onTertiary
                                        ) {
                                            Text("$totalVariants")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download Found Media"
                                    )
                                }
                            },
                            text = {
                                Text(
                                    text = "Download Media (${totalVariants} Streams)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }

                    else -> {
                        ExtendedFloatingActionButton(
                            onClick = {
                                val text = clipboardManager.getText()?.text
                                if (!text.isNullOrBlank()) {
                                    viewModel.loadUrl(text)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = MaterialTheme.shapes.large,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste URL"
                                )
                            },
                            text = { Text("Paste URL") }
                        )
                    }
                }
            }
        }

        // 4. Tab Switcher Sheet
        if (uiState.isTabSwitcherOpen) {
            TabSwitcherSheet(
                tabs = uiState.tabs,
                activeTabId = uiState.activeTabId,
                onDismiss = { viewModel.toggleTabSwitcher() },
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

        // 6. Signature Cascading Media Download Configuration Sheet
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

@Composable
fun BrowserHomeDashboard(
    isIncognito: Boolean = false,
    selectedSearchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    onSearchEngineClick: () -> Unit = {},
    onBookmarkClicked: (String) -> Unit,
    onPasteAndDownload: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    var directLinkInput by remember { mutableStateOf("") }

    val bookmarks = listOf(
        Triple("YouTube", "https://m.youtube.com", Icons.Default.Sensors),
        Triple("Vimeo", "https://vimeo.com", Icons.Default.Download),
        Triple("SoundCloud", "https://soundcloud.com", Icons.Default.Speed),
        Triple("Dailymotion", "https://dailymotion.com", Icons.Default.Lock),
        Triple("Archive.org", "https://archive.org", Icons.Default.Security),
        Triple("DuckDuckGo", "https://duckduckgo.com", Icons.Default.Search)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Identity Hero Header
        UdmHeroHeader(
            modifier = Modifier.fillMaxWidth()
        )

        // Incognito Status Card (if in Incognito tab)
        if (isIncognito) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CodecAv1Badge.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CodecAv1Badge.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnLock,
                            contentDescription = null,
                            tint = CodecAv1Badge,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Private Browsing Active",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CodecAv1Badge
                        )
                        Text(
                            text = "Searches, history, cookies, and temporary caches are deleted immediately when closing this tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Search Engine Selector Pill Chip
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSearchEngineClick() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Active Search Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedSearchEngine.displayName} (${selectedSearchEngine.hint})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Hero Card: Paste Link to Download
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Fast Stream Sniffer & Downloader",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Paste any streaming video, audio, or direct file link to inspect media streams and download with multi-threading.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = directLinkInput,
                        onValueChange = { directLinkInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Paste media link here...") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    IconButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                directLinkInput = clip
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste")
                    }
                }

                Button(
                    onClick = {
                        if (directLinkInput.isNotBlank()) {
                            onPasteAndDownload(directLinkInput)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sniff & Download Stream")
                }
            }
        }

        // Section: Bookmarks
        Text(
            text = "Supported Media Sites",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(bookmarks) { (title, url, icon) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBookmarkClicked(url) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Section: Real Privacy & Ad-Blocker Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Ad & Tracker Shield Active",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "In-memory DNS filter blocking tracking scripts and pop-up spam in real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BrowserHomeDashboardPreview() {
    UDMTheme {
        BrowserHomeDashboard(
            isIncognito = false,
            selectedSearchEngine = SearchEngine.DUCKDUCKGO,
            onSearchEngineClick = {},
            onBookmarkClicked = {},
            onPasteAndDownload = {}
        )
    }
}
