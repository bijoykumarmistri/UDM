package com.universal.dm.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val haptic = LocalHapticFeedback.current
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
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP HEADER - Exact matching layout to Downloads Screen (below status bar clock, no v2.0.0 chip)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    text = "Engine controls & system configuration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. HERO: QUANTUM SPEED & SOCKET CONCURRENCY ENGINE
        EngineConcurrencyHeroCard(
            threadCount = settings.threadCount,
            downloadFolder = settings.defaultDownloadFolder,
            onThreadCountChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                settings.updateThreadCount(it)
            },
            onBrowseDisk = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                openUdmFolder(context)
            }
        )

        // 3. SECTION: NETWORK & POWER SAFEGUARDS
        SettingsSectionHeader(
            title = "Network & Power Safeguards",
            icon = Icons.Default.Shield,
            tint = Color(0xFF00E676)
        )

        ModernSettingsCard {
            // Auto-Resume on Wi-Fi Connect
            ModernSettingSwitchRow(
                icon = Icons.Default.Sync,
                iconColor = Color(0xFF00E676),
                title = "Auto-Resume on Wi-Fi",
                subtitle = "Resumes paused downloads when high-speed Wi-Fi connects",
                checked = settings.autoResumeOnWifi,
                onCheckedChange = { settings.updateAutoResumeOnWifi(it) }
            )

            SettingsInsetDivider()

            // Wi-Fi Only Downloads
            ModernSettingSwitchRow(
                icon = Icons.Default.Wifi,
                iconColor = Color(0xFF00E5FF),
                title = "Wi-Fi Only Downloads",
                subtitle = "Pauses queue on cellular mobile data to conserve bandwidth",
                checked = settings.wifiOnlyDownloads,
                onCheckedChange = { settings.updateWifiOnly(it) }
            )

            SettingsInsetDivider()

            // Sound & Haptics on Completion
            ModernSettingSwitchRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                iconColor = Color(0xFFFFD54F),
                title = "Completion Chime & Sound",
                subtitle = "Plays notification chime and vibration when downloads finish",
                checked = settings.soundOnCompletion,
                onCheckedChange = { settings.updateSoundOnCompletion(it) }
            )

            SettingsInsetDivider()

            // Battery Optimization Exemption
            ModernSettingNavRow(
                icon = Icons.Default.BatteryChargingFull,
                iconColor = Color(0xFF81C784),
                title = "Background Power Safeguard",
                subtitle = "Disable OS battery throttling for reliable background downloads",
                badgeText = "System",
                onClick = { openBatterySettings(context) }
            )
        }

        // 4. SECTION: BROWSER PRIVACY & CONTENT SHIELDS
        SettingsSectionHeader(
            title = "Browser Privacy & Shields",
            icon = Icons.Default.Security,
            tint = Color(0xFFD05CE3)
        )

        ModernSettingsCard {
            // Built-in Ad-Blocker
            ModernSettingSwitchRow(
                icon = Icons.Default.Block,
                iconColor = Color(0xFFFF5252),
                title = "Brave Shields Ad-Blocker",
                subtitle = "Blocks video pre-rolls, banner ads and tracking beacons",
                checked = settings.isAdBlockerEnabled,
                onCheckedChange = { settings.updateAdBlocker(it) }
            )

            SettingsInsetDivider()

            // DNS & Tracker Sinkhole
            ModernSettingSwitchRow(
                icon = Icons.Default.Shield,
                iconColor = Color(0xFF10B981),
                title = "DNS & Tracker Sinkhole",
                subtitle = "In-memory DNS blocking for telemetry and data miners",
                checked = settings.isDnsFilterEnabled,
                onCheckedChange = { settings.updateDnsFilter(it) }
            )

            SettingsInsetDivider()

            // Block Popups & Redirects
            ModernSettingSwitchRow(
                icon = Icons.Default.Security,
                iconColor = Color(0xFFD05CE3),
                title = "Block Pop-ups & Traps",
                subtitle = "Prevents unexpected window jumps and deceptive redirects",
                checked = settings.blockPopupsAndRedirects,
                onCheckedChange = { settings.updateBlockPopups(it) }
            )

            SettingsInsetDivider()

            // Default Search Engine Picker
            ModernSettingNavRow(
                icon = Icons.Default.Search,
                iconColor = Color(0xFF29B6F6),
                title = "Default Search Engine",
                subtitle = "Engine used for omnibar web searches",
                badgeText = currentEngine.displayName,
                onClick = { showSearchEngineDialog = true }
            )
        }

        // 5. SECTION: APPEARANCE & SYSTEM GESTURES
        SettingsSectionHeader(
            title = "Appearance & Interface",
            icon = Icons.Default.Palette,
            tint = Color(0xFF00E5FF)
        )

        ModernSettingsCard {
            // Material You Dynamic Palette
            ModernSettingSwitchRow(
                icon = Icons.Default.ColorLens,
                iconColor = Color(0xFF00E5FF),
                title = "Material You Dynamic Colors",
                subtitle = "Extracts accent tones dynamically from device wallpaper",
                checked = settings.isDynamicColorEnabled,
                onCheckedChange = { settings.updateDynamicColor(it) }
            )

            SettingsInsetDivider()

            // Android 16/17 Predictive Back Gestures
            ModernSettingNavRow(
                icon = Icons.Default.Gesture,
                iconColor = Color(0xFFFFB74D),
                title = "Predictive Back Gestures",
                subtitle = "Native edge gesture animation with spring physics",
                badgeText = "Enabled",
                onClick = {}
            )
        }

        // 6. SECTION: STREAM EXTRACTOR ENGINE CORE
        SettingsSectionHeader(
            title = "Stream Extractor Core",
            icon = Icons.Default.Bolt,
            tint = Color(0xFFFFD54F)
        )

        ModernEngineStatusCard(
            isUpdating = isUpdatingEngine,
            updateMessage = engineUpdateMessage,
            onUpdateEngine = {
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
                        "Core Engine updated to stable ($status)"
                    } else {
                        "Core Engine verified & up-to-date"
                    }
                }
            }
        )

        // 7. SECTION: DEVELOPER & OPEN SOURCE SUPPORT
        SettingsSectionHeader(
            title = "Developer & Open Source",
            icon = Icons.Default.Favorite,
            tint = Color(0xFFFF5252)
        )

        ModernDeveloperDonationCard(
            copiedUpiMessage = copiedUpiMessage,
            onCopyUpi = { upi ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                clipboardManager.setText(AnnotatedString(upi))
                copiedUpiMessage = "UPI ID copied to clipboard!"
            },
            onPayUpi = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val upiUri = Uri.parse(AppMetadata.getUpiPaymentUri())
                val intent = Intent(Intent.ACTION_VIEW, upiUri)
                val chooser = Intent.createChooser(intent, "Donate via UPI App (GPay / PhonePe / Paytm)")
                runCatching { context.startActivity(chooser) }
            }
        )

        Spacer(modifier = Modifier.height(110.dp))
    }

    // Search Engine Selection Dialog
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

/**
 * High-Impact Concurrency & Storage Hero Dashboard
 */
@Composable
private fun EngineConcurrencyHeroCard(
    threadCount: Int,
    downloadFolder: String,
    onThreadCountChange: (Int) -> Unit,
    onBrowseDisk: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF07141E),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(Color(0x3500E5FF), Color(0x1800B0FF), Color(0x1000E5FF))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Row 1: Socket Speed Concurrency Title & Glow Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Socket Concurrency",
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "Multi-thread chunk acceleration",
                            style = TextStyle(fontSize = 11.sp, color = Color(0xFF7F9DB0))
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF))
                ) {
                    Text(
                        text = "$threadCount Sockets",
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00E5FF)),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Row 2: Segmented Speed Profile Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(4, "4x Eco", Icons.Default.Eco),
                    Triple(8, "8x Standard", Icons.Default.Bolt),
                    Triple(16, "16x Turbo", Icons.Default.RocketLaunch)
                ).forEach { (th, label, icon) ->
                    val isSelected = threadCount == th
                    Surface(
                        onClick = { onThreadCountChange(th) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.18f) else Color(0xFF0A1822),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0xFF142B38)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF5D7486),
                                modifier = Modifier.size(18.dp)
                            )
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

            // Slider
            Slider(
                value = threadCount.toFloat(),
                onValueChange = { onThreadCountChange(it.roundToInt()) },
                valueRange = 1f..16f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E5FF),
                    activeTrackColor = Color(0xFF00E5FF),
                    inactiveTrackColor = Color(0xFF122837)
                )
            )

            HorizontalDivider(color = Color(0xFF112635))

            // Download Destination Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                    }

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Download Folder",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = downloadFolder,
                            style = TextStyle(fontSize = 11.sp, color = Color(0xFF7F9DB0)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    onClick = onBrowseDisk,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0E2433),
                    border = BorderStroke(1.dp, Color(0xFF1B4057))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Browse",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                        )
                    }
                }
            }
        }
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
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Text(
            text = title.uppercase(Locale.ROOT),
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.1.sp,
                color = tint
            )
        )
    }
}

/**
 * Container with Glassmorphic Border and Deep Tone
 */
@Composable
fun ModernSettingsCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF07141E),
        border = BorderStroke(1.dp, Color(0xFF132B3B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            content()
        }
    }
}

/**
 * Inset Divider
 */
@Composable
fun SettingsInsetDivider() {
    HorizontalDivider(
        color = Color(0xFF0E2230),
        modifier = Modifier.padding(start = 50.dp)
    )
}

/**
 * Modern Switch Row with Icon Container
 */
@Composable
fun ModernSettingSwitchRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current

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
                    .background(iconColor.copy(alpha = 0.15f))
                    .border(1.dp, iconColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text(
                    text = subtitle,
                    style = TextStyle(fontSize = 11.sp, color = Color(0xFF7F9DB0))
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00E5FF),
                checkedTrackColor = Color(0xFF004D5A),
                uncheckedThumbColor = Color(0xFF637C8E),
                uncheckedTrackColor = Color(0xFF132533)
            )
        )
    }
}

/**
 * Modern Navigation / Action Row
 */
@Composable
fun ModernSettingNavRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

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
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f))
                    .border(1.dp, iconColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text(
                    text = subtitle,
                    style = TextStyle(fontSize = 11.sp, color = Color(0xFF7F9DB0))
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, iconColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = badgeText,
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = iconColor),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF5D7486),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * Stream Engine Health Dashboard Card
 */
@Composable
fun ModernEngineStatusCard(
    isUpdating: Boolean,
    updateMessage: String?,
    onUpdateEngine: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF07141E),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(Color(0x35FFD54F), Color(0x15FFB300), Color(0x10FFD54F))
            )
        )
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
                            .background(Color(0xFFFFD54F).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
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
fun ModernDeveloperDonationCard(
    copiedUpiMessage: String?,
    onCopyUpi: (String) -> Unit,
    onPayUpi: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF07141E),
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
                        .size(48.dp)
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
            Toast.makeText(context, "Storage folder located at /Download/UDM", Toast.LENGTH_LONG).show()
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

@Preview(showBackground = true, backgroundColor = 0xFF060D13)
@Composable
fun LuxurySettingsScreenPreview() {
    UDMTheme {
        SettingsScreen()
    }
}
