package com.universal.dm.ui.navigation

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universal.dm.UdmApplication
import com.universal.dm.data.local.DownloadTaskEntity
import com.universal.dm.ui.browser.BrowserScreen
import com.universal.dm.ui.downloads.DownloadsScreen
import com.universal.dm.ui.finished.FinishedScreen
import com.universal.dm.ui.settings.LocalAppSettings
import com.universal.dm.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

/**
 * Main Application Shell for Universal Download Manager V2.
 *
 * Implements:
 * - Apple-grade floating pill bottom navigation dock ([UdmBottomNav]).
 * - Fluid, physics-based page transitions between [Browser, Download, Finished, Settings].
 * - Full Android 16, 17 Predictive Back gesture transition animation with scale, offset & corner morphing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdmAppShell(
    modifier: Modifier = Modifier,
    initialSharedUrl: String? = null,
    onSharedUrlHandled: () -> Unit = {}
) {
    val settings = LocalAppSettings.current
    val context = LocalContext.current

    var currentScreen by rememberSaveable { mutableStateOf(Screen.Downloads) }
    val backStack = remember { mutableStateListOf(Screen.Downloads) }

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
                currentScreen = backStack.last()
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

    // Dynamically observe active download tasks count from Room
    val activeTasks by produceState(initialValue = emptyList<DownloadTaskEntity>(), context) {
        try {
            UdmApplication.getDatabase(context).downloadDao().getActiveTasksFlow().collect {
                value = it
            }
        } catch (_: Exception) {
            // Safe fallback for previews and unit tests
        }
    }

    val liveActiveCount = activeTasks.size
    LaunchedEffect(liveActiveCount) {
        settings.activeDownloadsCount = liveActiveCount
    }

    val displayBadgeCount = if (liveActiveCount > 0) liveActiveCount else settings.activeDownloadsCount

    // Predictive back gesture visual transformations (Android 16, 17 system animation)
    val predictiveScale by animateFloatAsState(
        targetValue = if (backProgress > 0f) 1f - (backProgress * 0.08f) else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PredictiveScale"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (currentScreen) {
                                Screen.Browser -> "Browser & Sniffer"
                                Screen.Downloads -> "Active Downloads"
                                Screen.Finished -> "Downloaded Library"
                                Screen.Settings -> "UDM Preferences"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentScreen == Screen.Downloads && liveActiveCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$liveActiveCount ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF001F24)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "UDM Engine",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(24.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            // Floating Capsule Pill Dock matching reference design
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
                    val shiftPx = if (progress > 0f) {
                        val px = progress * 28.dp.toPx()
                        if (backSwipeEdge == BackEventCompat.EDGE_LEFT) px else -px
                    } else 0f
                    translationX = shiftPx
                    scaleX = predictiveScale
                    scaleY = predictiveScale
                    if (progress > 0f) {
                        shape = RoundedCornerShape(progress * 28.dp.toPx())
                        clip = true
                    }
                }
        ) {
            // Apple-style silky smooth fluid page transitions
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val isForward = targetState.ordinal > initialState.ordinal
                    val slideOffsetFraction = 0.08f // 8% subtle Apple slide
                    val appleEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
                    val smoothSpring = spring<Float>(
                        dampingRatio = 0.88f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                    val smoothIntOffsetSpring = spring<androidx.compose.ui.unit.IntOffset>(
                        dampingRatio = 0.88f,
                        stiffness = Spring.StiffnessMediumLow
                    )

                    if (isForward) {
                        (slideInHorizontally(
                            animationSpec = smoothIntOffsetSpring,
                            initialOffsetX = { (it * slideOffsetFraction).toInt() }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 240, easing = appleEasing)
                        ) + scaleIn(
                            initialScale = 0.97f,
                            animationSpec = smoothSpring
                        )) togetherWith (slideOutHorizontally(
                            animationSpec = smoothIntOffsetSpring,
                            targetOffsetX = { (-it * slideOffsetFraction).toInt() }
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 200, easing = appleEasing)
                        ) + scaleOut(
                            targetScale = 0.97f,
                            animationSpec = smoothSpring
                        ))
                    } else {
                        (slideInHorizontally(
                            animationSpec = smoothIntOffsetSpring,
                            initialOffsetX = { (-it * slideOffsetFraction).toInt() }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 240, easing = appleEasing)
                        ) + scaleIn(
                            initialScale = 0.97f,
                            animationSpec = smoothSpring
                        )) togetherWith (slideOutHorizontally(
                            animationSpec = smoothIntOffsetSpring,
                            targetOffsetX = { (it * slideOffsetFraction).toInt() }
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 200, easing = appleEasing)
                        ) + scaleOut(
                            targetScale = 0.97f,
                            animationSpec = smoothSpring
                        ))
                    }
                },
                label = "ScreenTransition"
            ) { screen ->
                if (LocalInspectionMode.current) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "${screen.title} Preview Screen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    when (screen) {
                        Screen.Browser -> BrowserScreen(
                            sharedUrl = initialSharedUrl,
                            onSharedUrlHandled = onSharedUrlHandled,
                            onDownloadTriggered = { _, _ ->
                                navigateTo(Screen.Downloads)
                            }
                        )
                        Screen.Downloads -> DownloadsScreen()
                        Screen.Finished -> FinishedScreen()
                        Screen.Settings -> SettingsScreen()
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun UdmAppShellPreview() {
    com.universal.dm.ui.theme.UDMTheme {
        UdmAppShell()
    }
}
