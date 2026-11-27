package com.universal.dm.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.universal.dm.core.AppMetadata
import com.universal.dm.core.AppReleaseInfo
import com.universal.dm.core.AppUpdateManager
import com.universal.dm.core.AppUpdateStatus
import com.universal.dm.core.AppUpdateUiState
import com.universal.dm.core.LogcatExporter
import com.universal.dm.domain.downloader.DownloadManager
import com.universal.dm.domain.sniffer.MediaSizeCalculator
import com.universal.dm.ui.components.UdmIconSquircle
import com.universal.dm.ui.components.UdmScreenHeader
import com.universal.dm.ui.components.UdmTonalCard
import com.universal.dm.ui.theme.UDMTheme
import com.universal.dm.ui.theme.UdmTextStyles
import com.universal.dm.ui.theme.UdmTheme
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private data class VideoQualityPreset(
    val height: Int,
    val label: String
)

private val VideoQualityPresets = listOf(
    VideoQualityPreset(2160, "Best"),
    VideoQualityPreset(1080, "1080p"),
    VideoQualityPreset(720, "720p"),
    VideoQualityPreset(480, "480p")
)

private val SupportAmountOptions = listOf(
    "50" to "₹50",
    "100" to "₹100",
    "250" to "₹250",
    null to "Custom"
)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isInspection = LocalInspectionMode.current

    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = UdmTheme.extendedColors

    var isUpdatingEngine by remember { mutableStateOf(false) }
    var engineUpdateMessage by remember { mutableStateOf<String?>(null) }
    var isExportingLogs by remember { mutableStateOf(false) }
    var reclaimableCacheBytes by remember { mutableLongStateOf(0L) }
    var isCleaningCache by remember { mutableStateOf(false) }
    var cacheStatusMessage by remember { mutableStateOf<String?>(null) }

    val updateUiState by AppUpdateManager.uiState.collectAsState()
    val installedVersion = remember(context, isInspection) {
        if (isInspection) AppMetadata.APP_VERSION else AppMetadata.getInstalledVersionName(context)
    }

    LaunchedEffect(isInspection) {
        if (!isInspection) {
            reclaimableCacheBytes = runCatching {
                DownloadManager.getInstance(context).getReclaimableCacheBytes()
            }.getOrDefault(0L)
        }
    }

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
        UdmScreenHeader(title = "Settings")

        // 1. APPEARANCE
        SettingsGroupHeader("Appearance")
        SettingsGroupCard {
            SettingSegmentedRow(
                icon = Icons.Default.Palette,
                iconBg = colorScheme.tertiaryContainer,
                iconTint = colorScheme.onTertiaryContainer,
                title = "App theme",
                subtitle = "Choose light, dark, or match device",
                options = AppThemeMode.entries.map { it to it.label },
                selectedOption = settings.themeMode,
                onOptionSelected = { settings.updateThemeMode(it) }
            )

            SettingsInsetDivider()

            val pitchBlackAllowed = settings.themeMode != AppThemeMode.LIGHT
            SettingSwitchRow(
                icon = Icons.Default.DarkMode,
                iconBg = colorScheme.primaryContainer,
                iconTint = colorScheme.onPrimaryContainer,
                title = "Pitch black",
                subtitle = if (pitchBlackAllowed) {
                    "Pure black AMOLED surface in dark mode"
                } else {
                    "Available in System or Dark theme"
                },
                checked = settings.isPitchBlackEnabled,
                enabled = pitchBlackAllowed,
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
                        text = settings.threadCount.toString(),
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

            SettingSegmentedRow(
                icon = Icons.Default.HighQuality,
                iconBg = colorScheme.secondaryContainer,
                iconTint = colorScheme.onSecondaryContainer,
                title = "Default video quality",
                subtitle = "Pre-selected cap for detected & link streams",
                options = VideoQualityPresets.map { it.height to it.label },
                selectedOption = settings.preferredVideoQuality,
                onOptionSelected = { settings.updatePreferredVideoQuality(it) }
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
                subtitle = "Resume interrupted tasks when Wi-Fi connects",
                checked = settings.autoResumeOnWifi,
                onCheckedChange = { settings.updateAutoResumeOnWifi(it) }
            )

            SettingsInsetDivider()

            SettingSwitchRow(
                icon = Icons.Default.Refresh,
                iconBg = colorScheme.tertiaryContainer,
                iconTint = colorScheme.onTertiaryContainer,
                title = "Auto-retry on failure",
                subtitle = "Retry temporary network drops up to 2×",
                checked = settings.autoRetryOnFailure,
                onCheckedChange = { settings.updateAutoRetryOnFailure(it) }
            )

            SettingsInsetDivider()

            SettingSwitchRow(
                icon = Icons.Default.Image,
                iconBg = colorScheme.secondaryContainer,
                iconTint = colorScheme.onSecondaryContainer,
                title = "Embed music cover art",
                subtitle = "Save YouTube thumbnail into downloaded audio",
                checked = settings.embedAudioThumbnail,
                onCheckedChange = { settings.updateEmbedAudioThumbnail(it) }
            )

            SettingsInsetDivider()

            SettingSwitchRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                iconBg = extendedColors.warningContainer,
                iconTint = extendedColors.onWarningContainer,
                title = "Completion sound",
                subtitle = "Play chime when downloads finish",
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

        // 3. PRIVACY (Only protections not already on the Browser page)
        SettingsGroupHeader("Privacy")
        SettingsGroupCard {
            SettingSwitchRow(
                icon = Icons.Default.Shield,
                iconBg = extendedColors.successContainer,
                iconTint = extendedColors.onSuccessContainer,
                title = "Tracker protection",
                subtitle = "Block known analytics & tracking domains",
                checked = settings.isDnsFilterEnabled,
                onCheckedChange = { settings.updateDnsFilter(it) }
            )

            SettingsInsetDivider()

            SettingSwitchRow(
                icon = Icons.Default.Security,
                iconBg = colorScheme.tertiaryContainer,
                iconTint = colorScheme.onTertiaryContainer,
                title = "Block pop-ups",
                subtitle = "Prevent unwanted pop-ups & auto-redirects",
                checked = settings.blockPopupsAndRedirects,
                onCheckedChange = { settings.updateBlockPopups(it) }
            )

            SettingsInsetDivider()

            SettingSwitchRow(
                icon = Icons.Default.Cookie,
                iconBg = colorScheme.secondaryContainer,
                iconTint = colorScheme.onSecondaryContainer,
                title = "Block third-party cookies",
                subtitle = "Reject cross-site tracking cookies",
                checked = settings.blockThirdPartyCookies,
                onCheckedChange = { settings.updateBlockThirdPartyCookies(it) }
            )
        }

        // 4. ENGINE & STORAGE
        SettingsGroupHeader("Engine & Storage")
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
                        icon = Icons.Default.CleaningServices,
                        containerColor = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer,
                        size = 38.dp,
                        iconSize = 19.dp,
                        cornerRadius = 11.dp
                    )
                    Column {
                        Text(
                            text = "Temporary cache",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                cacheStatusMessage != null -> cacheStatusMessage!!
                                reclaimableCacheBytes > 0L -> "${MediaSizeCalculator.formatFileSize(reclaimableCacheBytes)} reclaimable"
                                else -> "No leftover temp segments"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (cacheStatusMessage != null) extendedColors.success else colorScheme.onSurfaceVariant,
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
                            isCleaningCache = true
                            val freed = if (!isInspection) {
                                runCatching {
                                    DownloadManager.getInstance(context).clearReclaimableCache()
                                }.getOrDefault(0L)
                            } else 0L
                            reclaimableCacheBytes = 0L
                            isCleaningCache = false
                            cacheStatusMessage = if (freed > 0L) {
                                "Freed ${MediaSizeCalculator.formatFileSize(freed)}"
                            } else {
                                "Already clean"
                            }
                        }
                    },
                    enabled = !isCleaningCache && reclaimableCacheBytes > 0L,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (isCleaningCache) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Clean", style = MaterialTheme.typography.labelMedium)
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
        }

        // 5. APP UPDATE (GitHub Releases + Direct APK Download)
        SettingsGroupHeader("App Update")
        AppUpdateCard(
            installedVersion = installedVersion,
            updateUiState = updateUiState,
            onCheckForUpdates = {
                if (!isInspection) {
                    scope.launch {
                        AppUpdateManager.checkForUpdate(context, isManual = true)
                    }
                }
            },
            onDownloadUpdate = { release ->
                if (!isInspection) {
                    AppUpdateManager.startDownloadAndInstall(context, release)
                }
            },
            onInstallUpdate = { apkFile ->
                if (!isInspection) {
                    AppUpdateManager.installApk(context, apkFile)
                }
            },
            onOpenGitHub = { url ->
                if (!isInspection) {
                    AppUpdateManager.openGitHubUrl(context, url)
                }
            }
        )

        // 6. SUPPORT DEVELOPMENT (Redesigned)
        SettingsGroupHeader("Support Development")
        SupportDevelopmentCard(
            onCopyUpi = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("UPI ID", AppMetadata.DONATION_UPI_ID))
            },
            onLaunchUpi = { amount ->
                val upiUri = AppMetadata.getUpiPaymentUri(amount).toUri()
                val intent = Intent(Intent.ACTION_VIEW, upiUri)
                val chooser = Intent.createChooser(intent, "Donate via UPI")
                runCatching { context.startActivity(chooser) }.onFailure {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("UPI ID", AppMetadata.DONATION_UPI_ID))
                    Toast.makeText(
                        context,
                        "No UPI app found — UPI ID copied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        Text(
            text = "${AppMetadata.APP_SHORT_NAME} v$installedVersion",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}

@Composable
private fun AppUpdateCard(
    installedVersion: String,
    updateUiState: AppUpdateUiState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: (AppReleaseInfo) -> Unit,
    onInstallUpdate: (java.io.File) -> Unit,
    onOpenGitHub: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = UdmTheme.extendedColors
    val haptic = LocalHapticFeedback.current
    val status = updateUiState.status

    val activeRelease: AppReleaseInfo? = when (status) {
        is AppUpdateStatus.UpToDate -> status.latestRelease
        is AppUpdateStatus.UpdateAvailable -> status.release
        is AppUpdateStatus.Downloading -> status.release
        is AppUpdateStatus.ReadyToInstall -> status.release
        is AppUpdateStatus.Error -> status.release
        else -> null
    }

    val iconVector = when (status) {
        is AppUpdateStatus.UpToDate -> Icons.Default.CheckCircle
        is AppUpdateStatus.UpdateAvailable,
        is AppUpdateStatus.ReadyToInstall -> Icons.Default.SystemUpdate
        is AppUpdateStatus.Downloading -> Icons.Default.Download
        else -> Icons.Default.SystemUpdate
    }

    val iconBg = when (status) {
        is AppUpdateStatus.UpToDate -> extendedColors.successContainer
        is AppUpdateStatus.UpdateAvailable,
        is AppUpdateStatus.ReadyToInstall -> colorScheme.primaryContainer
        is AppUpdateStatus.Error -> colorScheme.errorContainer
        else -> colorScheme.primaryContainer
    }

    val iconTint = when (status) {
        is AppUpdateStatus.UpToDate -> extendedColors.onSuccessContainer
        is AppUpdateStatus.UpdateAvailable,
        is AppUpdateStatus.ReadyToInstall -> colorScheme.onPrimaryContainer
        is AppUpdateStatus.Error -> colorScheme.onErrorContainer
        else -> colorScheme.onPrimaryContainer
    }

    val statusTitle = when (status) {
        AppUpdateStatus.Idle -> "${AppMetadata.APP_SHORT_NAME} v$installedVersion"
        AppUpdateStatus.Checking -> "Checking GitHub releases…"
        is AppUpdateStatus.UpToDate -> "Up to date • v${status.currentVersion}"
        is AppUpdateStatus.UpdateAvailable -> "Update available • ${status.release.tagName}"
        is AppUpdateStatus.Downloading -> "Downloading ${status.release.tagName}…"
        is AppUpdateStatus.ReadyToInstall -> "Ready to install • ${status.release.tagName}"
        is AppUpdateStatus.Error -> if (status.release != null) {
            "Download failed • ${status.release.tagName}"
        } else {
            "Update check failed"
        }
    }

    val statusSubtitle = when (status) {
        AppUpdateStatus.Idle -> "Tap Check to look for new APK releases"
        AppUpdateStatus.Checking -> "Fetching latest release from ${AppMetadata.GITHUB_DISPLAY_LINK}"
        is AppUpdateStatus.UpToDate -> status.latestRelease?.let {
            "Latest on GitHub: ${it.tagName}"
        } ?: "You have the latest installed build"
        is AppUpdateStatus.UpdateAvailable -> {
            val sizeText = if (status.release.apkSizeBytes > 0L) {
                " • ${MediaSizeCalculator.formatFileSize(status.release.apkSizeBytes)}"
            } else ""
            "Installed v${status.currentVersion}$sizeText"
        }
        is AppUpdateStatus.Downloading -> if (status.totalBytes > 0L) {
            "${MediaSizeCalculator.formatFileSize(status.downloadedBytes)} / ${MediaSizeCalculator.formatFileSize(status.totalBytes)} (${status.progressPercent}%)"
        } else {
            "${MediaSizeCalculator.formatFileSize(status.downloadedBytes)} downloaded"
        }
        is AppUpdateStatus.ReadyToInstall -> status.release.apkFileName ?: "APK ready for installation"
        is AppUpdateStatus.Error -> status.message
    }

    val subtitleColor = when (status) {
        is AppUpdateStatus.UpToDate -> extendedColors.success
        is AppUpdateStatus.UpdateAvailable,
        is AppUpdateStatus.ReadyToInstall -> colorScheme.primary
        is AppUpdateStatus.Error -> colorScheme.error
        else -> colorScheme.onSurfaceVariant
    }

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
                    icon = iconVector,
                    containerColor = iconBg,
                    contentColor = iconTint,
                    size = 38.dp,
                    iconSize = 19.dp,
                    cornerRadius = 11.dp
                )
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            when (status) {
                AppUpdateStatus.Checking -> {
                    FilledTonalButton(
                        onClick = {},
                        enabled = false,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                is AppUpdateStatus.UpdateAvailable -> {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDownloadUpdate(status.release)
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (status.release.apkDownloadUrl != null) "Download" else "Open",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                is AppUpdateStatus.Downloading -> {
                    FilledTonalButton(
                        onClick = {},
                        enabled = false,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${status.progressPercent}%",
                            style = UdmTextStyles.TabularMetricSmall
                        )
                    }
                }
                is AppUpdateStatus.ReadyToInstall -> {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onInstallUpdate(status.apkFile)
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Install", style = MaterialTheme.typography.labelMedium)
                    }
                }
                is AppUpdateStatus.Error -> {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (status.release != null) {
                                onDownloadUpdate(status.release)
                            } else {
                                onCheckForUpdates()
                            }
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Retry", style = MaterialTheme.typography.labelMedium)
                    }
                }
                else -> {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCheckForUpdates()
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Check", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (status is AppUpdateStatus.Downloading) {
            if (status.totalBytes > 0L) {
                LinearProgressIndicator(
                    progress = { (status.progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                )
            }
        }

        val showNotesRelease = when (status) {
            is AppUpdateStatus.UpdateAvailable -> status.release
            is AppUpdateStatus.ReadyToInstall -> status.release
            else -> null
        }
        if (showNotesRelease != null && showNotesRelease.releaseNotes.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = showNotesRelease.title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = showNotesRelease.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        SettingsInsetDivider()

        val targetGitHubUrl = when {
            status is AppUpdateStatus.UpdateAvailable -> status.release.htmlUrl
            status is AppUpdateStatus.ReadyToInstall -> status.release.htmlUrl
            activeRelease != null -> activeRelease.htmlUrl
            else -> AppMetadata.GITHUB_RELEASES_URL
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenGitHub(targetGitHubUrl)
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
                    icon = Icons.Default.Code,
                    containerColor = colorScheme.surfaceContainerHighest,
                    contentColor = colorScheme.onSurface,
                    size = 38.dp,
                    iconSize = 19.dp,
                    cornerRadius = 11.dp
                )
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = "GitHub repository & releases",
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = AppMetadata.GITHUB_DISPLAY_LINK,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open GitHub",
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun SupportDevelopmentCard(
    onCopyUpi: () -> Unit,
    onLaunchUpi: (amountInr: String?) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = UdmTheme.extendedColors
    val haptic = LocalHapticFeedback.current

    var selectedAmount by remember { mutableStateOf<String?>("100") }
    var copiedFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(copiedFeedback) {
        if (copiedFeedback) {
            delay(2200)
            copiedFeedback = false
        }
    }

    UdmTonalCard(
        shape = RoundedCornerShape(22.dp),
        containerColor = colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                        icon = Icons.Default.Favorite,
                        containerColor = colorScheme.tertiaryContainer,
                        contentColor = colorScheme.onTertiaryContainer,
                        size = 42.dp,
                        iconSize = 21.dp,
                        cornerRadius = 13.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Keep ${AppMetadata.APP_SHORT_NAME} ad-free",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Built independently by ${AppMetadata.DEVELOPER_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "UPI",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Quick amount selector pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SupportAmountOptions.forEach { (amountValue, label) ->
                    val isSelected = selectedAmount == amountValue
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer,
                        animationSpec = tween(150),
                        label = "SupportAmountBg"
                    )
                    val chipContent by animateColorAsState(
                        targetValue = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                        animationSpec = tween(150),
                        label = "SupportAmountText"
                    )

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedAmount = amountValue
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = chipBg,
                        border = if (isSelected) {
                            BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.55f))
                        } else null
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = chipContent,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // UPI ID copy pill + Donate CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCopyUpi()
                        copiedFeedback = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (copiedFeedback) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                            contentDescription = "Copy UPI ID",
                            tint = if (copiedFeedback) extendedColors.success else colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (copiedFeedback) "UPI ID copied" else AppMetadata.DONATION_UPI_ID,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = if (copiedFeedback) extendedColors.success else colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLaunchUpi(selectedAmount)
                    },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedAmount?.let { "Donate ₹$it" } ?: "Donate",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
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
private fun <T> SettingSegmentedRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            Column(modifier = Modifier.weight(1f)) {
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

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { (value, label) ->
                    val selected = value == selectedOption
                    val pillBg by animateColorAsState(
                        targetValue = if (selected) colorScheme.primaryContainer else Color.Transparent,
                        animationSpec = tween(150),
                        label = "SegmentBg"
                    )
                    val pillText by animateColorAsState(
                        targetValue = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                        animationSpec = tween(150),
                        label = "SegmentText"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(pillBg)
                            .clickable {
                                if (!selected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOptionSelected(value)
                                }
                            }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = pillText,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) {
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
            enabled = enabled,
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
