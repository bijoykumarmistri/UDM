package com.universal.dm.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Universal Download Manager V2 Navigation Destinations.
 * Reflects the 4 primary app pages:
 * - [Browser]: Web Browser & Video/Audio Media Sniffer
 * - [Downloads]: Active multi-threaded download queue & chunk monitor
 * - [Finished]: Downloaded Media Library & built-in Media3 player
 * - [Settings]: Engine preferences, network, adblocker & appearance
 */
enum class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val hasBadge: Boolean = false
) {
    Browser(
        route = "browser",
        title = "Browser",
        selectedIcon = Icons.Filled.Public,
        unselectedIcon = Icons.Outlined.Public
    ),
    Downloads(
        route = "downloads",
        title = "Download",
        selectedIcon = Icons.Filled.Download,
        unselectedIcon = Icons.Outlined.Download,
        hasBadge = true
    ),
    Finished(
        route = "finished",
        title = "Finished",
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder
    ),
    Settings(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    )
}
