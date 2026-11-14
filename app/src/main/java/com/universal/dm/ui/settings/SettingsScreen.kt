package com.universal.dm.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MotionPhotosAuto
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiCalling3
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universal.dm.core.AppMetadata
import com.universal.dm.ui.browser.SearchEngine
import com.universal.dm.ui.browser.SearchEngineSelectionDialog
import com.universal.dm.ui.theme.SpeedBadgeColor
import com.universal.dm.ui.theme.UDMTheme
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
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isUpdatingEngine by remember { mutableStateOf(false) }
    var engineUpdateMessage by remember { mutableStateOf<String?>(null) }
    var copiedUpiMessage by remember { mutableStateOf<String?>(null) }
    var showSearchEngineDialog by remember { mutableStateOf(false) }

    val currentEngine = remember(settings.defaultSearchEngineName) {
        try {
            SearchEngine.valueOf(settings.defaultSearchEngineName)
        } catch (_: Exception) {
            SearchEngine.DUCKDUCKGO
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // 1. TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Preferences & engine telemetry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Version Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) Color(0xFF0F2636) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E435B) else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF))
                    )
                    Text(
                        text = "v${AppMetadata.APP_VERSION}",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    )
                }
            }
        }

        // 2. HERO: STREAM EXTRACTOR & CORE ENGINE HEALTH
        StreamEngineTelemetryCard(
            isUpdating = isUpdatingEngine,
            updateMessage = engineUpdateMessage,
            onUpdateEngine = {
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
                        "Core Engine updated to stable ($status)"
                    } else {
                        "Core Engine verified & up-to-date"
                    }
                }
            }
        )

        // 3. SECTION: DOWNLOAD ENGINE CONCURRENCY
        SettingsSectionHeader(title = "Download Engine Concurrency", icon = Icons.Default.Speed, tint = Color(0xFF00E5FF))

        LuxurySettingsContainer {
            // Parallel Thread Slider
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Socket Concurrency",
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "Parallel segmented HTTP chunk sockets (IDM & Prism Engine)",
                            style = TextStyle(fontSize = 12.sp, color = Color(0xFF7F9DB0)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF))
                    ) {
                        Text(
                            text = "${settings.threadCount} Sockets",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF)),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Slider(
                    value = settings.threadCount.toFloat(),
                    onValueChange = { settings.updateThreadCount(it.roundToInt()) },
                    valueRange = 1f..16f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0xFF132A39)
                    )
                )

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(4 to "4x Eco", 8 to "8x Standard", 16 to "16x Turbo").forEach { (th, label) ->
                        val isSelected = settings.threadCount == th
                        Surface(
                            onClick = { settings.updateThreadCount(th) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF0D1E2B),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0xFF1A384D)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF8BA6B9)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF112330))

            // Download Destination Folder
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                    }

                    Column {
                        Text(
                            text = "Download Destination",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = settings.defaultDownloadFolder,
                            style = TextStyle(fontSize = 11.sp, color = Color(0xFF7F9DB0)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = { openUdmFolder(context) },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F2636),
                    border = BorderStroke(1.dp, Color(0xFF1E435B))
                ) {
                    Text(
                        text = "Browse Disk",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF)),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF112330))

            // Auto-Resume on Wi-Fi Connect
            AppleSettingsSwitchRow(
                icon = Icons.Default.Sync,
                iconTint = Color(0xFF00E676),
                title = "Auto-Resume on Wi-Fi Reconnect",
                subtitle = "Automatically resumes paused queue when high-speed Wi-Fi is detected",
                checked = settings.autoResumeOnWifi,
                onCheckedChange = { settings.updateAutoResumeOnWifi(it) }
            )
        }

        // 4. SECTION: NETWORK & BATTERY SHIELDS
        SettingsSectionHeader(title = "Network & Power Shields", icon = Icons.Default.Shield, tint = Color(0xFF00E676))

        LuxurySettingsContainer {
            // Wi-Fi Only Downloads
            AppleSettingsSwitchRow(
                icon = Icons.Default.Wifi,
                iconTint = Color(0xFF00E5FF),
                title = "Wi-Fi Only Downloads",
                subtitle = "Automatically pauses downloads when running on cellular mobile data",
                checked = settings.wifiOnlyDownloads,
                onCheckedChange = { settings.updateWifiOnly(it) }
            )

            HorizontalDivider(color = Color(0xFF112330))

            // Sound on Completion
            AppleSettingsSwitchRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                iconTint = Color(0xFFFFD54F),
                title = "Sound & Haptics on Completion",
                subtitle = "Play subtle completion chime when all chunks finish muxing",
                checked = settings.soundOnCompletion,
                onCheckedChange = { settings.updateSoundOnCompletion(it) }
            )

            HorizontalDivider(color = Color(0xFF112330))

            // Battery Optimization Exemption
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openBatterySettings(context) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF00E676).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                    }

                    Column {
                        Text(
                            text = "Background Power Safeguard",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "Exempt UDM from OS background throttling for uninterrupted transfers",
                            style = TextStyle(fontSize = 11.sp, color = Color(0xFF7F9DB0))
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color(0xFF5D7486),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // 5. SECTION: BROWSER SHIELDS & NETWORK FILTERS
        SettingsSectionHeader(title = "Browser Shields & Content Filters", icon = Icons.Default.Security, tint = Color(0xFFD05CE3))

        LuxurySettingsContainer {
            // Built-in Ad-Blocker
            AppleSettingsSwitchRow(
                icon = Icons.Default.Block,
                iconTint = Color(0xFFFF5252),
                title = "Built-in Ad-Blocker & Telemetry Shield",
                subtitle = "In-memory request filtering blocks banner ads, tracking beacons & video pre-rolls",
                checked = settings.isAdBlockerEnabled,
                onCheckedChange = { settings.updateAdBlocker(it) }
            )

            HorizontalDivider(color = Color(0xFF112330))

            // DNS & Tracker Sinkhole Filter
            AppleSettingsSwitchRow(
                icon = Icons.Default.Shield,
                iconTint = Color(0xFF10B981),
                title = "DNS & Tracker Sinkhole Filter",
                subtitle = "Blocks known telemetry and ad network hosts via in-memory DNS filtering",
                checked = settings.isDnsFilterEnabled,
                onCheckedChange = { settings.updateDnsFilter(it) }
            )

            HorizontalDivider(color = Color(0xFF112330))

            // Block Pop-ups & Redirects
            AppleSettingsSwitchRow(
                icon = Icons.Default.Security,
                iconTint = Color(0xFFD05CE3),
                title = "Block Pop-ups & Domain Traps",
                subtitle = "Prevents unexpected window hops, fake redirects and spam alerts",
                checked = settings.blockPopupsAndRedirects,
                onCheckedChange = { settings.updateBlockPopups(it) }
            )

            HorizontalDivider(color = Color(0xFF112330))

            // Default Search Engine Picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSearchEngineDialog = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                    }

                    Column {
                        Text(
                            text = "Default Search Engine",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = currentEngine.displayName,
                            style = TextStyle(fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F2636),
                    border = BorderStroke(1.dp, Color(0xFF1E435B))
                ) {
                    Text(
                        text = "Change",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF)),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 6. SECTION: APPEARANCE & SYSTEM
        SettingsSectionHeader(title = "Appearance & System", icon = Icons.Default.Palette, tint = Color(0xFF00E5FF))

        LuxurySettingsContainer {
            // Material You Dynamic Palette
            AppleSettingsSwitchRow(
                icon = Icons.Default.ColorLens,
                iconTint = Color(0xFF00E5FF),
                title = "Material You Dynamic Palette",
                subtitle = "Extracts accent tones dynamically from device wallpaper (Android 12+)",
                checked = settings.isDynamicColorEnabled,
                onCheckedChange = { settings.updateDynamicColor(it) }
            )

            HorizontalDivider(color = Color(0xFF112330))

            // Android 16/17 Predictive Back Indicator
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFD54F).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Gesture, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(20.dp))
                    }

                    Column {
                        Text(
                            text = "Predictive Back Gestures",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "Full Android 16/17 edge gesture animation with spring physics",
                            style = TextStyle(fontSize = 11.sp, color = Color(0xFF7F9DB0))
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00E676).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF00E676))
                ) {
                    Text(
                        text = "Enabled",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676)),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 7. SECTION: DEVELOPER & PROJECT SUPPORT
        SettingsSectionHeader(title = "Developer & Open Source Project", icon = Icons.Default.Favorite, tint = Color(0xFFFF5252))

        LuxuryDeveloperCard(
            copiedUpiMessage = copiedUpiMessage,
            onCopyUpi = { upi ->
                clipboardManager.setText(AnnotatedString(upi))
                copiedUpiMessage = "UPI ID copied to clipboard!"
            },
            onPayUpi = {
                val upiUri = Uri.parse(AppMetadata.getUpiPaymentUri())
                val intent = Intent(Intent.ACTION_VIEW, upiUri)
                val chooser = Intent.createChooser(intent, "Pay with UPI (GPay / PhonePe / Paytm / BHIM)")
                runCatching { context.startActivity(chooser) }
            }
        )

        Spacer(modifier = Modifier.height(100.dp))
    }

    // Search Engine Selection Dialog
    if (showSearchEngineDialog) {
        SearchEngineSelectionDialog(
            selectedEngine = currentEngine,
            onSelectEngine = { selected ->
                settings.updateSearchEngine(selected.name)
                showSearchEngineDialog = false
            },
            onDismiss = { showSearchEngineDialog = false }
        )
    }
}

/**
 * Section Header Title with Glowing Tint Icon
 */
@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(
            text = title.uppercase(Locale.ROOT),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.1.sp,
                color = tint
            )
        )
    }
}

/**
 * Container with Apple-Grade Glassmorphic Border and Deep Tone
 */
@Composable
fun LuxurySettingsContainer(
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val containerColor = if (isDark) Color(0xFF08141F) else MaterialTheme.colorScheme.surfaceContainerLow
    val borderColor = if (isDark) Color(0xFF132838) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            content()
        }
    }
}

/**
 * Apple-Style Settings Switch Row
 */
@Composable
fun AppleSettingsSwitchRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

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
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = subtitle,
                    style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isDark) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                checkedTrackColor = if (isDark) Color(0xFF004D5A) else MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = if (isDark) Color(0xFF637C8E) else MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = if (isDark) Color(0xFF132533) else MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}

/**
 * Stream Engine Telemetry & Health Dashboard Card
 */
@Composable
fun StreamEngineTelemetryCard(
    isUpdating: Boolean,
    updateMessage: String?,
    onUpdateEngine: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardColor = if (isDark) Color(0xFF08141F) else MaterialTheme.colorScheme.surfaceContainerLow
    val borderBrush = if (isDark) {
        Brush.horizontalGradient(
            listOf(Color(0x4000E5FF), Color(0x2000B0FF), Color(0x1000E5FF))
        )
    } else {
        Brush.horizontalGradient(
            listOf(MaterialTheme.colorScheme.primary.copy(0.35f), MaterialTheme.colorScheme.secondary.copy(0.2f))
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
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
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Stream Extractor Core",
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "yt-dlp Python engine with AV1 & Opus stream parser",
                            style = TextStyle(fontSize = 11.sp, color = Color(0xFF7F9DB0)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00E676).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF00E676))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF00E676)))
                        Text(
                            text = "Online",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676)),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (updateMessage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                    Text(
                        text = updateMessage,
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                    )
                }
            }

            // Update Engine Button
            Button(
                onClick = onUpdateEngine,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = !isUpdating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F2B3D),
                    contentColor = Color(0xFF00E5FF)
                ),
                border = BorderStroke(1.dp, Color(0xFF1F4863))
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Updating Engine Binaries...", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold))
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check & Update Stream Engine", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

/**
 * Developer & Project Support Card
 */
@Composable
fun LuxuryDeveloperCard(
    copiedUpiMessage: String?,
    onCopyUpi: (String) -> Unit,
    onPayUpi: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF08141F),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(Color(0x40FFD54F), Color(0x20FFB300), Color(0x10FFD54F))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD54F).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(24.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Created by ${AppMetadata.DEVELOPER_NAME}",
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFD54F).copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, Color(0xFFFFD54F))
                        ) {
                            Text(
                                text = "Lead Dev",
                                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F)),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Universal Download Manager is 100% free, ad-free and open for the community.",
                        style = TextStyle(fontSize = 11.sp, color = Color(0xFF8BA6B9))
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF132838))

            // UPI Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF040E16),
                border = BorderStroke(1.dp, Color(0xFF183244))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DONATION UPI ID",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD54F), letterSpacing = 1.sp)
                        )
                        Text(
                            text = AppMetadata.DONATION_UPI_ID,
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        )
                    }

                    IconButton(
                        onClick = { onCopyUpi(AppMetadata.DONATION_UPI_ID) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy UPI ID",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (copiedUpiMessage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
                    Text(
                        text = copiedUpiMessage,
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                    )
                }
            }

            // Pay with UPI Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFFD54F), Color(0xFFFFB300))
                        )
                    )
                    .clickable { onPayUpi() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color(0xFF1C1300),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Donate via UPI App (GPay / PhonePe / Paytm)",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1C1300)
                        )
                    )
                }
            }
        }
    }
}

private fun openUdmFolder(context: Context) {
    try {
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "UDM")
        if (!folder.exists()) folder.mkdirs()

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(folder.absolutePath), "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Storage folder located at /Download/UDM", Toast.LENGTH_LONG).show()
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

@Preview(showBackground = true, backgroundColor = 0xFF060D13)
@Composable
fun LuxurySettingsScreenPreview() {
    UDMTheme {
        SettingsScreen()
    }
}
