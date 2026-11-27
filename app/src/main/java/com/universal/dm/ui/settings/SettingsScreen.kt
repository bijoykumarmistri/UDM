package com.universal.dm.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.universal.dm.core.AppMetadata
import com.universal.dm.core.LogcatExporter
import com.universal.dm.domain.sniffer.MediaSizeCalculator
import com.universal.dm.ui.browser.SearchEngine
import com.universal.dm.ui.browser.SearchEngineSelectionDialog
import com.universal.dm.ui.components.UdmIconSquircle
import com.universal.dm.ui.components.UdmScreenHeader
import com.universal.dm.ui.components.UdmTonalCard
import com.universal.dm.ui.theme.UDMTheme
import com.universal.dm.ui.theme.UdmTextStyles
import com.universal.dm.ui.theme.UdmTheme
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = UdmTheme.extendedColors

    var isUpdatingEngine by remember { mutableStateOf(false) }
    var engineUpdateMessage by remember { mutableStateOf<String?>(null) }
    var isExportingLogs by remember { mutableStateOf(false) }
    var copiedUpiMessage by remember { mutableStateOf<String?>(null) }
    var showSearchEngineDialog by remember { mutableStateOf(false) }

    val exportAndShareLogs: () -> Unit = {
        if (!isExportingLogs) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            scope.launch {
                isExportingLogs = true
                runCatching {
                    val file = LogcatExporter.exportDiagnosticLog(context)
                    LogcatExporter.shareLogFile(context, file)
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        "Failed to export logs: ${err.localizedMessage ?: "Unknown error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                isExportingLogs = false
            }
        }
    }

    val currentEngine = remember(settings.defaultSearchEngineName) {
        try {
            SearchEngine.valueOf(settings.defaultSearchEngineName)
        } catch (_: Exception) {
            SearchEngine.DUCKDUCKGO
        }
    }

    val freeStorageText = remember {
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            "${MediaSizeCalculator.formatFileSize(freeBytes)} free"
        }.getOrDefault("Ready")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        UdmScreenHeader(
            title = "Settings",
            actions = {
                IconButton(
                    onClick = exportAndShareLogs,
                    enabled = !isExportingLogs
                ) {
                    if (isExportingLogs) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share diagnostic logs",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )

        // 1. APPEARANCE
        SettingsGroupHeader("Appearance")
        SettingsGroupCard {
            SettingSwitchRow(
                icon = Icons.Default.DarkMode,
                iconBg = colorScheme.primaryContainer,
                iconTint = colorScheme.onPrimaryContainer,
                title = "Pitch black",
                subtitle = "Pure black background in dark mode",
                checked = settings.isPitchBlackEnabled,
                onCheckedChange = { settings.updatePitchBlack(it) }
            )
        }

        // 2. DOWNLOADS
        SettingsGroupHeader("Downloads")
        SettingsGroupCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UdmIconSquircle(
                            icon = Icons.Default.Speed,
                            containerColor = colorScheme.primaryContainer,
                            contentColor = colorScheme.onPrimaryContainer,
                            size = 38.dp,
                            iconSize = 19.dp,
                            cornerRadius = 11.dp
                        )
                        Column {
                            Text(
                                text = "Parallel connections",
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Splits files into concurrent parts",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "${settings.threadCount}",
                        style = UdmTextStyles.TabularMetricSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                Slider(
                    value = settings.threadCount.toFloat(),
                    onValueChange = {
                        val v = it.roundToInt()
                        if (v != settings.threadCount) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            settings.updateThreadCount(v)
                        }
                    },
                    valueRange = 1f..16f,
                    steps = 14
                )
            }

            SettingsInsetDivider()

            SettingNavRow(
                icon = Icons.Default.Folder,
                iconBg = colorScheme.secondaryContainer,
                iconTint = colorScheme.onSecondaryContainer,
                title = "Save location",
                subtitle = "${settings.defaultDownloadFolder} • $freeStorageText",
                onClick = { openUdmFolder(context) }
            )

            SettingsInsetDivider()

            SettingSwitchRow(
                icon = Icons.Default.Wifi,
                iconBg = colorScheme.primaryContainer,
                iconTint = colorScheme.onPrimaryContainer,
                title = "Wi-Fi only",
                subtitle = "Pause downloads on mobile data",
                checked = settings.wifiOnlyDownloads,
                onCheckedChange = { settings.updateWifiOnly(it) }
            )

            SettingsInsetDivider()

            SettingSwitchRow(
                icon = Icons.Default.Sync,
                iconBg = extendedColors.successContainer,
                iconTint = extendedColors.onSuccessContainer,
                title = "Auto-resume on Wi-Fi",
                subtitle = "Resume paused tasks automatically",
                checked = settings.autoResumeOnWifi,
                onCheckedChange = { settings.updateAutoResumeOnWifi(it) }
            )

            SettingsInsetDivider()

            SettingSwitchRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                iconBg = extendedColors.warningContainer,
                iconTint = extendedColors.onWarningContainer,
                title = "Completion sound",
                subtitle = "Play chime when finished",
                checked = settings.soundOnCompletion,
                onCheckedChange = { settings.updateSoundOnCompletion(it) }
            )

            SettingsInsetDivider()

            SettingNavRow(
                icon = Icons.Default.BatteryChargingFull,
                iconBg = extendedColors.successContainer,
                iconTint = extendedColors.onSuccessContainer,
                title = "Background battery",
                subtitle = "Allow unrestricted background downloads",
                onClick = { openBatterySettings(context) }
            )
        }

        // 3. BROWSER & PRIVACY
        SettingsGroupHeader("Browser & Privacy")
        SettingsGroupCard {
            SettingNavRow(
                icon = Icons.Default.Search,
                iconBg = colorScheme.primaryContainer,
                iconTint = colorScheme.onPrimaryContainer,
                title = "Search engine",
                subtitle = currentEngine.displayName,
                onClick = { showSearchEngineDialog = true }
            )

            SettingsInsetDivider()

            SettingSwitchRow(
                icon = Icons.Default.Block,
                iconBg = colorScheme.errorContainer,
                iconTint = colorScheme.onErrorContainer,
                title = "Ad blocker",
                subtitle = "Block page and video ads",
                checked = settings.isAdBlockerEnabled,
                onCheckedChange = { settings.updateAdBlocker(it) }
            )

            SettingsInsetDivider()

            SettingSwitchRow(
                icon = Icons.Default.Shield,
                iconBg = extendedColors.successContainer,
                iconTint = extendedColors.onSuccessContainer,
                title = "Tracker protection",
                subtitle = "Block known tracking domains",
                checked = settings.isDnsFilterEnabled,
                onCheckedChange = { settings.updateDnsFilter(it) }
            )

            SettingsInsetDivider()

            SettingSwitchRow(
                icon = Icons.Default.Security,
                iconBg = colorScheme.tertiaryContainer,
                iconTint = colorScheme.onTertiaryContainer,
                title = "Block pop-ups",
                subtitle = "Prevent unwanted redirects",
                checked = settings.blockPopupsAndRedirects,
                onCheckedChange = { settings.updateBlockPopups(it) }
            )
        }

        // 4. EXTRACTOR & SUPPORT
        SettingsGroupHeader("Engine & Support")
        SettingsGroupCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UdmIconSquircle(
                        icon = Icons.Default.Bolt,
                        containerColor = extendedColors.warningContainer,
                        contentColor = extendedColors.onWarningContainer,
                        size = 38.dp,
                        iconSize = 19.dp,
                        cornerRadius = 11.dp
                    )
                    Column {
                        Text(
                            text = "Media extractor",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = engineUpdateMessage ?: "yt-dlp video & audio core",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (engineUpdateMessage != null) extendedColors.success else colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            isUpdatingEngine = true
                            engineUpdateMessage = null
                            val status = withContext(Dispatchers.IO) {
                                runCatching {
                                    YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
                                }.getOrNull()
                            }
                            isUpdatingEngine = false
                            engineUpdateMessage = if (status != null) {
                                "Updated ($status)"
                            } else {
                                "Already up to date"
                            }
                        }
                    },
                    enabled = !isUpdatingEngine,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (isUpdatingEngine) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Update", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            SettingsInsetDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isExportingLogs, onClick = exportAndShareLogs)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UdmIconSquircle(
                        icon = Icons.Default.BugReport,
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer,
                        size = 38.dp,
                        iconSize = 19.dp,
                        cornerRadius = 11.dp
                    )
                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            text = "Share logcat",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (isExportingLogs) "Collecting full logs & diagnostics…" else "Export full logs & device info for bug fixing",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isExportingLogs) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share logcat",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            SettingsInsetDivider()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UdmIconSquircle(
                        icon = Icons.Default.Favorite,
                        containerColor = colorScheme.errorContainer,
                        contentColor = colorScheme.onErrorContainer,
                        size = 38.dp,
                        iconSize = 19.dp,
                        cornerRadius = 11.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Support development",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Made by ${AppMetadata.DEVELOPER_NAME} • Free & ad-free",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.surfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppMetadata.DONATION_UPI_ID,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = colorScheme.onSurface
                        )
                        if (copiedUpiMessage != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = extendedColors.success,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = copiedUpiMessage!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = extendedColors.success
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("UPI ID", AppMetadata.DONATION_UPI_ID))
                                copiedUpiMessage = "Copied"
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy UPI ID",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val upiUri = AppMetadata.getUpiPaymentUri().toUri()
                                val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                val chooser = Intent.createChooser(intent, "Donate via UPI")
                                runCatching { context.startActivity(chooser) }
                            },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Donate", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        Text(
            text = "UDM v${AppMetadata.APP_VERSION}",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }

    if (showSearchEngineDialog) {
        SearchEngineSelectionDialog(
            selectedEngine = currentEngine,
            onSelectEngine = { selected ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                settings.updateSearchEngine(selected.name)
                showSearchEngineDialog = false
            },
            onDismiss = { showSearchEngineDialog = false }
        )
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, top = 2.dp)
    )
}

@Composable
private fun SettingsGroupCard(
    content: @Composable () -> Unit
) {
    UdmTonalCard(
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsInsetDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        modifier = Modifier.padding(start = 50.dp)
    )
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UdmIconSquircle(
                icon = icon,
                containerColor = iconBg,
                contentColor = iconTint,
                size = 38.dp,
                iconSize = 19.dp,
                cornerRadius = 11.dp
            )

            Column(modifier = Modifier.padding(end = 10.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(it)
            }
        )
    }
}

@Composable
private fun SettingNavRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UdmIconSquircle(
                icon = icon,
                containerColor = iconBg,
                contentColor = iconTint,
                size = 38.dp,
                iconSize = 19.dp,
                cornerRadius = 11.dp
            )

            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
        )
    }
}

private fun openUdmFolder(context: Context) {
    try {
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "UDM")
        if (!folder.exists()) folder.mkdirs()

        val docUri = DocumentsContract.buildRootUri("com.android.externalstorage.documents", "primary")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(docUri, DocumentsContract.Document.MIME_TYPE_DIR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val dmIntent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dmIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Saved to /Download/UDM", Toast.LENGTH_LONG).show()
        }
    }
}

private fun openBatterySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val genericIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(genericIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Open system battery settings", Toast.LENGTH_SHORT).show()
        }
    }
}

@Preview(name = "Settings - Light", showBackground = true)
@Composable
fun LuxurySettingsScreenLightPreview() {
    UDMTheme(darkTheme = false, dynamicColor = false) {
        SettingsScreen()
    }
}

@Preview(name = "Settings - Dark", showBackground = true)
@Composable
fun LuxurySettingsScreenDarkPreview() {
    UDMTheme(darkTheme = true, dynamicColor = false) {
        SettingsScreen()
    }
}

@Preview(name = "Settings - Pitch Black", showBackground = true)
@Composable
fun LuxurySettingsScreenPitchBlackPreview() {
    UDMTheme(darkTheme = true, dynamicColor = false, pitchBlack = true) {
        SettingsScreen()
    }
}
