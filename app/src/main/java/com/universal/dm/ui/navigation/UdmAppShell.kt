package com.universal.dm.ui.navigation

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.universal.dm.UdmApplication
import com.universal.dm.core.AppMetadata
import com.universal.dm.core.AppUpdateManager
import com.universal.dm.core.AppUpdateStatus
import com.universal.dm.data.local.DownloadTaskEntity
import com.universal.dm.domain.sniffer.MediaSizeCalculator
import com.universal.dm.ui.browser.BrowserScreen
import com.universal.dm.ui.downloads.DownloadsScreen
import com.universal.dm.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

/**
 * Main Application Shell for Universal Download Manager V2.
 *
 * Implements:
 * - Apple-grade floating pill bottom navigation dock ([UdmBottomNav]).
 * - Fluid, hardware-accelerated spatial page transitions between [Browser, Downloads, Settings].
 * - Visited screen retention for instantaneous zero-latency page switching and state preservation.
 * - Full Android 16, 17 Predictive Back gesture transition animation with scale, offset & corner morphing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdmAppShell(
    modifier: Modifier = Modifier,
    initialSharedUrl: String? = null,
    onSharedUrlHandled: () -> Unit = {}
) {
    val context = LocalContext.current

    var currentScreen by rememberSaveable { mutableStateOf(Screen.Downloads) }
    val backStack = remember { mutableStateListOf(Screen.Downloads) }
    var slideDirection by remember { mutableIntStateOf(1) } // 1: forward, -1: backward

    // Start ONLY with currentScreen so cold start renders Frame 0 instantly.
    // Inactive tabs are composed on-demand when first visited, then retained for zero-latency switching.
    val visitedScreens = remember { mutableStateListOf(currentScreen) }

    // Android 16 & 17 Predictive Back Gesture Animation State
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    // Intercept back gestures across tabs with predictive back animation
    val canPredictiveBack = backStack.size > 1
    PredictiveBackHandler(enabled = canPredictiveBack) { progressFlow: Flow<BackEventCompat> ->
        try {
            progressFlow.collect { backEvent ->
                backProgress = backEvent.progress
                backSwipeEdge = backEvent.swipeEdge
            }
            // Gesture successfully completed -> pop backstack
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
                val target = backStack.last()
                if (!visitedScreens.contains(target)) {
                    visitedScreens.add(target)
                }
                slideDirection = if (target.ordinal > currentScreen.ordinal) 1 else -1
                currentScreen = target
            }
        } catch (_: CancellationException) {
            // User cancelled swipe gesture -> spring back smoothly
        } finally {
            backProgress = 0f
        }
    }

    // Function to navigate to a new screen and update backstack
    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            if (!visitedScreens.contains(screen)) {
                visitedScreens.add(screen)
            }
            slideDirection = if (screen.ordinal > currentScreen.ordinal) 1 else -1
            currentScreen = screen
            if (backStack.lastOrNull() != screen) {
                backStack.add(screen)
            }
        }
    }

    // When an external URL is shared to UDM, immediately transition to the Browser screen
    LaunchedEffect(initialSharedUrl) {
        if (!initialSharedUrl.isNullOrBlank()) {
            navigateTo(Screen.Browser)
        }
    }

    // Dynamically observe active download tasks count from Room without triggering AppSettings writes
    val activeTasks by produceState(initialValue = emptyList<DownloadTaskEntity>(), context) {
        try {
            UdmApplication.getDatabase(context).downloadDao().getActiveTasksFlow().collect {
                value = it
            }
        } catch (_: Exception) {
            // Safe fallback for previews and unit tests
        }
    }

    val displayBadgeCount = activeTasks.size
    val isInspection = LocalInspectionMode.current
    val updateUiState by AppUpdateManager.uiState.collectAsState()

    // Background GitHub release check (throttled every 12 hours)
    LaunchedEffect(isInspection) {
        if (!isInspection) {
            AppUpdateManager.checkForUpdateIfDue(context)
        }
    }

    // Predictive back gesture visual transformations (Android 16, 17 system animation)
    val predictiveScaleState = animateFloatAsState(
        targetValue = if (backProgress > 0f) 1f - (backProgress * 0.08f) else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PredictiveScale"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Floating Capsule Pill Dock with GPU-accelerated 120Hz sliding indicator
            UdmBottomNav(
                currentScreen = currentScreen,
                onScreenSelected = { targetScreen ->
                    navigateTo(targetScreen)
                },
                badgeCount = displayBadgeCount
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .graphicsLayer {
                    val progress = backProgress
                    if (progress > 0f) {
                        val px = progress * 28.dp.toPx()
                        translationX = if (backSwipeEdge == BackEventCompat.EDGE_LEFT) px else -px
                        val s = predictiveScaleState.value
                        scaleX = s
                        scaleY = s
                        shape = RoundedCornerShape(progress * 28.dp.toPx())
                        clip = true
                    } else {
                        translationX = 0f
                        scaleX = 1f
                        scaleY = 1f
                        clip = false
                    }
                }
        ) {
            if (LocalInspectionMode.current) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "${currentScreen.title} Preview Screen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Screen.entries.forEach { screen ->
                    if (visitedScreens.contains(screen)) {
                        AnimatedScreenSlot(
                            screen = screen,
                            isSelected = currentScreen == screen,
                            slideDirection = slideDirection
                        ) {
                            when (screen) {
                                Screen.Browser -> BrowserScreen(
                                    sharedUrl = initialSharedUrl,
                                    onSharedUrlHandled = onSharedUrlHandled,
                                    onDownloadTriggered = { _, _ ->
                                        navigateTo(Screen.Downloads)
                                    }
                                )
                                Screen.Downloads -> DownloadsScreen(
                                    onNavigateToBrowser = { navigateTo(Screen.Browser) }
                                )
                                Screen.Settings -> SettingsScreen()
                            }
                        }
                    }
                }
            }
        }
    }

    if (!isInspection && updateUiState.showAutoPrompt) {
        AppUpdatePromptDialog(
            status = updateUiState.status,
            onDownload = { release ->
                AppUpdateManager.startDownloadAndInstall(context, release)
            },
            onInstall = { apkFile ->
                AppUpdateManager.installApk(context, apkFile)
            },
            onOpenGitHub = { url ->
                AppUpdateManager.openGitHubUrl(context, url)
            },
            onDismiss = {
                AppUpdateManager.dismissAutoPrompt()
            }
        )
    }
}

@Composable
private fun AppUpdatePromptDialog(
    status: AppUpdateStatus,
    onDownload: (com.universal.dm.core.AppReleaseInfo) -> Unit,
    onInstall: (java.io.File) -> Unit,
    onOpenGitHub: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val release = when (status) {
        is AppUpdateStatus.UpdateAvailable -> status.release
        is AppUpdateStatus.Downloading -> status.release
        is AppUpdateStatus.ReadyToInstall -> status.release
        is AppUpdateStatus.Error -> status.release
        else -> null
    } ?: return

    val titleText = when (status) {
        is AppUpdateStatus.Downloading -> "Downloading ${release.tagName}"
        is AppUpdateStatus.ReadyToInstall -> "Ready to install ${release.tagName}"
        is AppUpdateStatus.Error -> "Update download failed"
        else -> "Update available • ${release.tagName}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = titleText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = release.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                if (release.apkSizeBytes > 0L) {
                    Text(
                        text = "Package size: ${MediaSizeCalculator.formatFileSize(release.apkSizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (release.releaseNotes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = release.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }
                when (status) {
                    is AppUpdateStatus.Downloading -> {
                        if (status.totalBytes > 0L) {
                            LinearProgressIndicator(
                                progress = { (status.progressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                            )
                            Text(
                                text = "${MediaSizeCalculator.formatFileSize(status.downloadedBytes)} / ${MediaSizeCalculator.formatFileSize(status.totalBytes)} (${status.progressPercent}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                            )
                        }
                    }
                    is AppUpdateStatus.Error -> {
                        Text(
                            text = status.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> Unit
                }
                TextButton(
                    onClick = { onOpenGitHub(release.htmlUrl.ifBlank { AppMetadata.GITHUB_RELEASES_URL }) }
                ) {
                    Text("View on ${AppMetadata.GITHUB_DISPLAY_LINK}")
                }
            }
        },
        confirmButton = {
            when (status) {
                is AppUpdateStatus.ReadyToInstall -> {
                    Button(onClick = { onInstall(status.apkFile) }) {
                        Text("Install APK")
                    }
                }
                is AppUpdateStatus.Downloading -> {
                    Button(onClick = {}, enabled = false) {
                        Text("${status.progressPercent}%")
                    }
                }
                else -> {
                    Button(onClick = { onDownload(release) }) {
                        Text(if (release.apkDownloadUrl != null) "Download & Install" else "Open GitHub")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

/**
 * Isolated Composable wrapper for each visited screen.
 * Reads animation state strictly inside [graphicsLayer] and [drawWithContent] lambdas
 * so that 200ms page transitions execute directly on the GPU without triggering CPU recompositions.
 */
@Composable
private fun AnimatedScreenSlot(
    screen: Screen,
    isSelected: Boolean,
    slideDirection: Int,
    content: @Composable () -> Unit
) {
    val alphaAnimState = animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "ScreenAlpha_${screen.name}"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (isSelected) 1f else 0f)
            .graphicsLayer {
                val a = alphaAnimState.value
                alpha = a
                val offsetPx = 24.dp.toPx()
                translationX = if (isSelected) {
                    (1f - a) * offsetPx * slideDirection
                } else {
                    (1f - a) * -offsetPx * slideDirection
                }
            }
            .drawWithContent {
                if (isSelected || alphaAnimState.value > 0.005f) {
                    drawContent()
                }
            }
            .semantics {
                if (!isSelected) {
                    @Suppress("DEPRECATION")
                    invisibleToUser()
                }
            }
    ) {
        content()
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun UdmAppShellPreview() {
    com.universal.dm.ui.theme.UDMTheme {
        UdmAppShell()
    }
}
