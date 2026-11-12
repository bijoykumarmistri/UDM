package com.universal.dm.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import com.universal.dm.UdmApplication
import com.universal.dm.data.local.DownloadTaskEntity
import com.universal.dm.ui.browser.BrowserScreen
import com.universal.dm.ui.downloads.DownloadsScreen
import com.universal.dm.ui.finished.FinishedScreen
import com.universal.dm.ui.settings.LocalAppSettings
import com.universal.dm.ui.settings.SettingsScreen

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

    // When an external URL is shared to UDM, immediately transition to the Browser screen
    LaunchedEffect(initialSharedUrl) {
        if (!initialSharedUrl.isNullOrBlank()) {
            currentScreen = Screen.Browser
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
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
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "UDM Engine",
                        tint = MaterialTheme.colorScheme.primary,
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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp
            ) {
                Screen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            if (screen.hasBadge && displayBadgeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ) {
                                            Text(text = "$displayBadgeCount")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width / 4 } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> -width / 4 } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width / 4 } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> width / 4 } + fadeOut())
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
                                currentScreen = Screen.Downloads
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
