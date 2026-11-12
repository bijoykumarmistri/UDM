package com.universal.dm.ui.settings

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppSettingsState {
    var threadCount by mutableIntStateOf(8)
    var defaultDownloadFolder by mutableStateOf("/storage/emulated/0/Download/UDM")
    var isAdBlockerEnabled by mutableStateOf(true)
    var blockPopupsAndRedirects by mutableStateOf(true)
    var wifiOnlyDownloads by mutableStateOf(false)
    var isDynamicColorEnabled by mutableStateOf(true)
    var isDarkThemeEnabled by mutableStateOf<Boolean?>(null) // null means follow system theme
    var activeDownloadsCount by mutableIntStateOf(0)
}

val LocalAppSettings = compositionLocalOf { AppSettingsState() }
