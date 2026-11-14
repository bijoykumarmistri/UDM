package com.universal.dm.ui.browser

import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universal.dm.data.model.AudioCodec
import com.universal.dm.data.model.DetectedPageMedia
import com.universal.dm.data.model.MediaType
import com.universal.dm.data.model.StreamVariant
import com.universal.dm.data.model.VideoCodec
import com.universal.dm.domain.sniffer.MediaSizeCalculator
import com.universal.dm.ui.settings.LocalAppSettings
import com.universal.dm.ui.theme.CodecAv1Badge
import com.universal.dm.ui.theme.CodecM4aBadge
import java.util.Locale
import kotlin.math.roundToInt

data class QualitySelectionItem(
    val label: String,
    val subLabel: String,
    val badge: String,
    val badgeColor: Color,
    val height: Int,
    val bitrateKbps: Int,
    val matchedVariant: StreamVariant?
)

data class CodecSelectionOption(
    val codecName: String,
    val tag: String,
    val badge: String,
    val featureHighlight: String,
    val sizeText: String,
    val accentColor: Color,
    val matchedVariant: StreamVariant?,
    val extension: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadConfigBottomSheet(
    detectedMedia: DetectedPageMedia,
    onDismiss: () -> Unit,
    onEnqueueDownload: (
        taskTitle: String,
        fileName: String,
        downloadUrl: String,
        pairedAudioUrl: String?,
        mediaType: MediaType,
        qualityLabel: String,
        codecLabel: String,
        threadCount: Int,
        destinationFolder: String,
        totalBytes: Long,
        sourcePageUrl: String,
        formatId: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val containerColor = if (isDark) Color(0xFF07121B) else MaterialTheme.colorScheme.surfaceContainerLow
    val handleColor = if (isDark) Color(0xFF223A4D) else MaterialTheme.colorScheme.outlineVariant

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = containerColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(handleColor)
            )
        }
    ) {
        DownloadConfigContent(
            detectedMedia = detectedMedia,
            onDismiss = onDismiss,
            onEnqueueDownload = onEnqueueDownload
        )
    }
}

@Composable
fun DownloadConfigContent(
    detectedMedia: DetectedPageMedia,
    onDismiss: () -> Unit,
    onEnqueueDownload: (
        taskTitle: String,
        fileName: String,
        downloadUrl: String,
        pairedAudioUrl: String?,
        mediaType: MediaType,
        qualityLabel: String,
        codecLabel: String,
        threadCount: Int,
        destinationFolder: String,
        totalBytes: Long,
        sourcePageUrl: String,
        formatId: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val appSettings = LocalAppSettings.current
    val scrollState = rememberScrollState()

    // 1. Initial Media Type State & Direct File Recognition
    val isDirectFile = detectedMedia.isDirectFile ||
            (detectedMedia.videoVariants.isEmpty() && detectedMedia.audioVariants.isEmpty())

    val detectedExtension = remember(detectedMedia) {
        val fromVariant = detectedMedia.directFileVariant?.containerExt?.takeIf { it.isNotBlank() }
        val fromUrl = detectedMedia.directFileUrl?.let { Uri.parse(it).path?.substringAfterLast('.', "") }?.takeIf { it.isNotBlank() }
        val fromTitle = detectedMedia.pageTitle.substringAfterLast('.', "").takeIf { it.isNotBlank() && it.length <= 5 }
        (fromVariant ?: fromUrl ?: fromTitle ?: "bin").lowercase(Locale.ROOT)
    }

    var selectedMediaType by remember(detectedMedia.pageUrl) {
        mutableStateOf(
            when {
                isDirectFile -> MediaType.DIRECT_FILE
                detectedMedia.videoVariants.isNotEmpty() -> MediaType.VIDEO
                else -> MediaType.AUDIO
            }
        )
    }

    // Editable Filename
    val initialSanitizedName = remember(detectedMedia.pageTitle, detectedExtension) {
        val raw = detectedMedia.pageTitle.trim()
        val stripped = if (detectedExtension.isNotBlank() && raw.endsWith(".$detectedExtension", ignoreCase = true)) {
            raw.substringBeforeLast('.')
        } else {
            raw
        }
        stripped
            .replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
            .trim()
            .take(65)
            .ifEmpty { if (isDirectFile) "download_file" else "download_media" }
    }
    var editableFileName by remember(detectedMedia.pageUrl) { mutableStateOf(initialSanitizedName) }

    // Advanced Options State
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var selectedThreadCount by remember { mutableIntStateOf(appSettings.threadCount.coerceIn(1, 16)) }
    var destinationFolder by remember { mutableStateOf(appSettings.defaultDownloadFolder) }
    var isWifiOnly by remember { mutableStateOf(appSettings.wifiOnlyDownloads) }

    // Storage calculation
    val freeStorageBytes = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            48_000_000_000L
        }
    }

    // 2. Compute Quality Options with Mathematical Sizing & Luxury Badges
    val videoQualityOptions = remember(detectedMedia.videoVariants, detectedMedia.durationSeconds) {
        val standardHeights = listOf(
            Triple(2160, "2160p 4K", Pair("Ultra HD", Color(0xFFFFB300))),
            Triple(1440, "1440p QHD", Pair("Quad HD", Color(0xFF8B5CF6))),
            Triple(1080, "1080p FHD", Pair("Most Popular", Color(0xFF00E5FF))),
            Triple(720, "720p HD", Pair("High Def", Color(0xFF00B0FF))),
            Triple(480, "480p SD", Pair("Standard", Color(0xFF4DD0E1))),
            Triple(360, "360p", Pair("Data Saver", Color(0xFF80CBC4)))
        )

        val availableVariantsByHeight = detectedMedia.videoVariants.groupBy { it.resolutionHeight }
        val maxAvailableHeight = detectedMedia.videoVariants.maxOfOrNull { it.resolutionHeight } ?: 1080

        val result = mutableListOf<QualitySelectionItem>()
        val duration = detectedMedia.durationSeconds

        standardHeights.forEach { (height, label, badgePair) ->
            val match = availableVariantsByHeight[height]?.firstOrNull()
            val hasNearbyVariant = detectedMedia.videoVariants.find { it.resolutionHeight in (height - 60)..(height + 60) }

            if (match != null || hasNearbyVariant != null) {
                val variant = match ?: hasNearbyVariant

                val isExactOrNearby = variant != null && kotlin.math.abs(variant.resolutionHeight - height) <= 60
                val computedBytes = if (isExactOrNearby && (variant?.fileSizeBytes ?: 0L) > 0) {
                    variant!!.fileSizeBytes
                } else {
                    MediaSizeCalculator.calculateVideoTotalBytes(
                        height = height,
                        codec = variant?.videoCodec ?: VideoCodec.H264,
                        durationSeconds = duration,
                        variantBitrateKbps = variant?.bitrateKbps?.takeIf { it > 0 },
                        requiresAudioMerge = variant?.requiresAudioMerge ?: false
                    )
                }

                result.add(
                    QualitySelectionItem(
                        label = label,
                        subLabel = MediaSizeCalculator.formatFileSize(computedBytes),
                        badge = badgePair.first,
                        badgeColor = badgePair.second,
                        height = height,
                        bitrateKbps = variant?.bitrateKbps ?: 0,
                        matchedVariant = variant?.copy(fileSizeBytes = computedBytes) ?: variant
                    )
                )
            }
        }

        // Add remaining unique variants from source streams
        detectedMedia.videoVariants.forEach { v ->
            if (v.resolutionHeight > 0 && result.none { kotlin.math.abs(it.height - v.resolutionHeight) <= 60 }) {
                val computedBytes = if (v.fileSizeBytes > 0) {
                    v.fileSizeBytes
                } else {
                    MediaSizeCalculator.calculateVideoTotalBytes(
                        height = v.resolutionHeight,
                        codec = v.videoCodec ?: VideoCodec.H264,
                        durationSeconds = duration,
                        variantBitrateKbps = v.bitrateKbps.takeIf { it > 0 },
                        requiresAudioMerge = v.requiresAudioMerge
                    )
                }
                result.add(
                    QualitySelectionItem(
                        label = v.qualityLabel,
                        subLabel = MediaSizeCalculator.formatFileSize(computedBytes),
                        badge = "Native",
                        badgeColor = Color(0xFF64B5F6),
                        height = v.resolutionHeight,
                        bitrateKbps = v.bitrateKbps,
                        matchedVariant = v.copy(fileSizeBytes = computedBytes)
                    )
                )
            }
        }

        if (result.isEmpty() && detectedMedia.directFileUrl != null && !isDirectFile) {
            standardHeights.filter { it.first <= 1080 }.forEach { (height, label, badgePair) ->
                val fallbackBytes = MediaSizeCalculator.calculateVideoTotalBytes(
                    height = height,
                    codec = VideoCodec.H264,
                    durationSeconds = duration,
                    requiresAudioMerge = false
                )
                result.add(
                    QualitySelectionItem(
                        label = label,
                        subLabel = MediaSizeCalculator.formatFileSize(fallbackBytes),
                        badge = badgePair.first,
                        badgeColor = badgePair.second,
                        height = height,
                        bitrateKbps = MediaSizeCalculator.getBaselineVideoBitrateKbps(height),
                        matchedVariant = null
                    )
                )
            }
        }
        result.sortedByDescending { it.height }
    }

    val audioQualityOptions = remember(detectedMedia.audioVariants, detectedMedia.durationSeconds, detectedMedia.directFileUrl, isDirectFile) {
        if (isDirectFile || (detectedMedia.audioVariants.isEmpty() && detectedMedia.directFileUrl == null)) {
            emptyList<QualitySelectionItem>()
        } else {
            val duration = detectedMedia.durationSeconds
            val maxKbpsVariant = detectedMedia.audioVariants.maxByOrNull { it.bitrateKbps }
            val midKbpsVariant = detectedMedia.audioVariants.find { it.bitrateKbps in 120..192 } ?: detectedMedia.audioVariants.firstOrNull()
            val minKbpsVariant = detectedMedia.audioVariants.minByOrNull { it.bitrateKbps }

            val masterBytes = MediaSizeCalculator.calculateAudioTotalBytes(320, AudioCodec.M4A, duration)
            val studioBytes = MediaSizeCalculator.calculateAudioTotalBytes(256, AudioCodec.M4A, duration)
            val stdBytes = MediaSizeCalculator.calculateAudioTotalBytes(128, AudioCodec.M4A, duration)
            val saverBytes = MediaSizeCalculator.calculateAudioTotalBytes(64, AudioCodec.M4A, duration)

            listOf(
                QualitySelectionItem(
                    label = "320 kbps Master",
                    subLabel = "${MediaSizeCalculator.formatFileSize(masterBytes)} (Audiophile)",
                    badge = "Ultra Hi-Fi",
                    badgeColor = Color(0xFFFFD700),
                    height = 0,
                    bitrateKbps = 320,
                    matchedVariant = maxKbpsVariant?.copy(fileSizeBytes = masterBytes, bitrateKbps = 320) ?: maxKbpsVariant
                ),
                QualitySelectionItem(
                    label = "256 kbps Studio",
                    subLabel = "${MediaSizeCalculator.formatFileSize(studioBytes)} (High Fidelity)",
                    badge = "Studio Grade",
                    badgeColor = Color(0xFFB388FF),
                    height = 0,
                    bitrateKbps = 256,
                    matchedVariant = maxKbpsVariant?.copy(fileSizeBytes = studioBytes, bitrateKbps = 256) ?: maxKbpsVariant
                ),
                QualitySelectionItem(
                    label = "128 kbps Standard",
                    subLabel = "${MediaSizeCalculator.formatFileSize(stdBytes)} (Balanced)",
                    badge = "Crisp AAC",
                    badgeColor = Color(0xFF00E5FF),
                    height = 0,
                    bitrateKbps = 128,
                    matchedVariant = midKbpsVariant?.copy(fileSizeBytes = stdBytes, bitrateKbps = 128) ?: midKbpsVariant
                ),
                QualitySelectionItem(
                    label = "64 kbps Saver",
                    subLabel = "${MediaSizeCalculator.formatFileSize(saverBytes)} (Compact)",
                    badge = "Data Saver",
                    badgeColor = Color(0xFF4DD0E1),
                    height = 0,
                    bitrateKbps = 64,
                    matchedVariant = minKbpsVariant?.copy(fileSizeBytes = saverBytes, bitrateKbps = 64) ?: minKbpsVariant
                )
            )
        }
    }

    // Selected Quality (Stage 2)
    val defaultVideoHeight = videoQualityOptions.firstOrNull()?.height ?: 1080
    var selectedHeight by remember(detectedMedia.pageUrl) { mutableIntStateOf(defaultVideoHeight) }
    var selectedAudioTier by remember(detectedMedia.pageUrl) { mutableStateOf("studio") }

    val selectedQuality = if (selectedMediaType == MediaType.VIDEO) {
        videoQualityOptions.find { it.height == selectedHeight } ?: videoQualityOptions.firstOrNull()
    } else {
        audioQualityOptions.find {
            when (selectedAudioTier) {
                "master" -> it.bitrateKbps == 320
                "studio", "high" -> it.bitrateKbps == 256
                "saver" -> it.bitrateKbps == 64
                else -> it.bitrateKbps == 128
            }
        } ?: audioQualityOptions.find { it.bitrateKbps == 256 } ?: audioQualityOptions.firstOrNull()
    }

    // Selected Codec Name
    var selectedCodecName by remember(selectedMediaType) {
        mutableStateOf(if (selectedMediaType == MediaType.VIDEO) "AV1" else "M4A")
    }

    // 3. Compute Codec Options (Stage 3)
    val videoCodecOptions = remember(selectedQuality, detectedMedia.videoVariants, detectedMedia.durationSeconds) {
        val q = selectedQuality
        val height = q?.height ?: 1080
        val duration = detectedMedia.durationSeconds

        val matchingVariants = detectedMedia.videoVariants.filter {
            kotlin.math.abs(it.resolutionHeight - height) <= 80
        }

        val av1Match = matchingVariants.find { it.videoCodec == VideoCodec.AV1 }
        val h264Match = matchingVariants.find { it.videoCodec == VideoCodec.H264 }
        val vp9Match = matchingVariants.find { it.videoCodec == VideoCodec.VP9 }

        val av1Bytes = if ((av1Match?.fileSizeBytes ?: 0L) > 0) {
            av1Match!!.fileSizeBytes
        } else {
            MediaSizeCalculator.calculateVideoTotalBytes(
                height = height,
                codec = VideoCodec.AV1,
                durationSeconds = duration,
                variantBitrateKbps = av1Match?.bitrateKbps?.takeIf { it > 0 }
            )
        }

        val h264Bytes = if ((h264Match?.fileSizeBytes ?: 0L) > 0) {
            h264Match!!.fileSizeBytes
        } else {
            MediaSizeCalculator.calculateVideoTotalBytes(
                height = height,
                codec = VideoCodec.H264,
                durationSeconds = duration,
                variantBitrateKbps = h264Match?.bitrateKbps?.takeIf { it > 0 }
            )
        }

        val vp9Bytes = if ((vp9Match?.fileSizeBytes ?: 0L) > 0) {
            vp9Match!!.fileSizeBytes
        } else {
            MediaSizeCalculator.calculateVideoTotalBytes(
                height = height,
                codec = VideoCodec.VP9,
                durationSeconds = duration,
                variantBitrateKbps = vp9Match?.bitrateKbps?.takeIf { it > 0 }
            )
        }

        listOf(
            CodecSelectionOption(
                codecName = "AV1",
                tag = "av01",
                badge = "Next-Gen • 35% Smaller • Hardware Decoded",
                featureHighlight = "Save up to 40% bandwidth with cutting-edge visual fidelity",
                sizeText = MediaSizeCalculator.formatFileSize(av1Bytes),
                accentColor = CodecAv1Badge,
                matchedVariant = av1Match?.copy(fileSizeBytes = av1Bytes) ?: q?.matchedVariant?.copy(fileSizeBytes = av1Bytes),
                extension = "mp4"
            ),
            CodecSelectionOption(
                codecName = "H.264",
                tag = "avc1",
                badge = "Universal Playback • Full Compatibility",
                featureHighlight = "Plays seamlessly on any phone, Smart TV, or PC without transcoding",
                sizeText = MediaSizeCalculator.formatFileSize(h264Bytes),
                accentColor = Color(0xFF00E5FF),
                matchedVariant = h264Match?.copy(fileSizeBytes = h264Bytes) ?: q?.matchedVariant?.copy(fileSizeBytes = h264Bytes),
                extension = "mp4"
            ),
            CodecSelectionOption(
                codecName = "VP9",
                tag = "vp9",
                badge = "High Dynamic Range • YouTube Standard",
                featureHighlight = "Optimized for Google streams with rich color depth and 60fps",
                sizeText = MediaSizeCalculator.formatFileSize(vp9Bytes),
                accentColor = Color(0xFF0084FF),
                matchedVariant = vp9Match?.copy(fileSizeBytes = vp9Bytes) ?: q?.matchedVariant?.copy(fileSizeBytes = vp9Bytes),
                extension = "mkv"
            )
        )
    }

    val audioCodecOptions = remember(selectedQuality, detectedMedia.audioVariants, detectedMedia.durationSeconds) {
        val q = selectedQuality
        val baseRate = q?.bitrateKbps ?: 256
        val duration = detectedMedia.durationSeconds

        val m4aMatch = detectedMedia.audioVariants.find { it.audioCodec == AudioCodec.M4A }
        val opusMatch = detectedMedia.audioVariants.find { it.audioCodec == AudioCodec.OPUS }
        val mp3Match = detectedMedia.audioVariants.find { it.audioCodec == AudioCodec.MP3 }

        val m4aBytes = MediaSizeCalculator.calculateAudioTotalBytes(baseRate, AudioCodec.M4A, duration)
        val opusBytes = MediaSizeCalculator.calculateAudioTotalBytes(baseRate, AudioCodec.OPUS, duration)
        val mp3Bytes = MediaSizeCalculator.calculateAudioTotalBytes(baseRate, AudioCodec.MP3, duration)

        listOf(
            CodecSelectionOption(
                codecName = "M4A",
                tag = "mp4a",
                badge = "Apple & Android Native AAC • Recommended",
                featureHighlight = "Hardware-accelerated AAC encoding at $baseRate kbps with full fidelity",
                sizeText = MediaSizeCalculator.formatFileSize(m4aBytes),
                accentColor = CodecM4aBadge,
                matchedVariant = m4aMatch?.copy(fileSizeBytes = m4aBytes, bitrateKbps = baseRate)
                    ?: q?.matchedVariant?.copy(fileSizeBytes = m4aBytes, bitrateKbps = baseRate),
                extension = "m4a"
            ),
            CodecSelectionOption(
                codecName = "Opus",
                tag = "opus",
                badge = "Next-Gen Audio • Pristine 48kHz Clarity",
                featureHighlight = "Ultra-low distortion Opus stream at $baseRate kbps preserving studio harmonics",
                sizeText = MediaSizeCalculator.formatFileSize(opusBytes),
                accentColor = Color(0xFF38EF7D),
                matchedVariant = opusMatch?.copy(fileSizeBytes = opusBytes, bitrateKbps = baseRate)
                    ?: q?.matchedVariant?.copy(fileSizeBytes = opusBytes, bitrateKbps = baseRate),
                extension = "opus"
            ),
            CodecSelectionOption(
                codecName = "MP3",
                tag = "mp3",
                badge = "Universal Legacy Audio • 320kbps Ready",
                featureHighlight = "LAME-encoded MP3 at $baseRate kbps for high-end audio gear and car stereos",
                sizeText = MediaSizeCalculator.formatFileSize(mp3Bytes),
                accentColor = Color(0xFFFFB300),
                matchedVariant = mp3Match?.copy(fileSizeBytes = mp3Bytes, bitrateKbps = baseRate)
                    ?: q?.matchedVariant?.copy(fileSizeBytes = mp3Bytes, bitrateKbps = baseRate),
                extension = "mp3"
            )
        )
    }

    val activeCodecOptions = if (selectedMediaType == MediaType.VIDEO) videoCodecOptions else audioCodecOptions
    val selectedCodec = activeCodecOptions.find { it.codecName.equals(selectedCodecName, ignoreCase = true) }
        ?: activeCodecOptions.firstOrNull()

    // Sizing and Duration calculations for CTA & Gauge
    val finalCodecName = when (selectedMediaType) {
        MediaType.DIRECT_FILE -> detectedExtension.uppercase(Locale.ROOT)
        MediaType.VIDEO -> selectedCodec?.codecName ?: "AV1"
        MediaType.AUDIO -> selectedCodec?.codecName ?: "M4A"
    }

    val finalQualityLabel = when (selectedMediaType) {
        MediaType.DIRECT_FILE -> detectedMedia.directFileVariant?.qualityLabel ?: "${detectedExtension.uppercase(Locale.ROOT)} File"
        MediaType.VIDEO -> selectedQuality?.label ?: "1080p FHD"
        MediaType.AUDIO -> "${selectedQuality?.bitrateKbps ?: 256} kbps"
    }

    val finalExtension = when (selectedMediaType) {
        MediaType.DIRECT_FILE -> detectedExtension
        MediaType.VIDEO -> selectedCodec?.extension ?: "mp4"
        MediaType.AUDIO -> selectedCodec?.extension ?: "m4a"
    }

    val finalTotalBytes = when (selectedMediaType) {
        MediaType.DIRECT_FILE -> detectedMedia.directFileVariant?.fileSizeBytes?.takeIf { it > 0 } ?: 0L
        MediaType.VIDEO -> {
            selectedCodec?.matchedVariant?.fileSizeBytes?.takeIf { it > 0 }
                ?: selectedQuality?.matchedVariant?.fileSizeBytes?.takeIf { it > 0 }
                ?: MediaSizeCalculator.calculateVideoTotalBytes(
                    height = selectedQuality?.height ?: 1080,
                    codec = when (finalCodecName) {
                        "AV1" -> VideoCodec.AV1
                        "VP9" -> VideoCodec.VP9
                        else -> VideoCodec.H264
                    },
                    durationSeconds = detectedMedia.durationSeconds
                )
        }
        MediaType.AUDIO -> {
            selectedCodec?.matchedVariant?.fileSizeBytes?.takeIf { it > 0 }
                ?: selectedQuality?.matchedVariant?.fileSizeBytes?.takeIf { it > 0 }
                ?: MediaSizeCalculator.calculateAudioTotalBytes(
                    targetBitrateKbps = selectedQuality?.bitrateKbps ?: 256,
                    codec = when (finalCodecName) {
                        "Opus" -> AudioCodec.OPUS
                        "MP3" -> AudioCodec.MP3
                        else -> AudioCodec.M4A
                    },
                    durationSeconds = detectedMedia.durationSeconds
                )
        }
    }

    // Estimated download time at 65 Mbps (representative LTE / 5G / Wi-Fi)
    val estimatedSeconds = (finalTotalBytes.toDouble() / (8_000_000)).coerceAtLeast(1.0).roundToInt()
    val etaString = if (estimatedSeconds < 60) "~${estimatedSeconds}s on Wi-Fi" else "~${estimatedSeconds / 60}m ${estimatedSeconds % 60}s"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // =========================================================================
        // 1. TOP MEDIA IDENTITY CARD (CYBER LUXURY MEDIA PLATE)
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1924)),
            border = BorderStroke(1.dp, Color(0xFF1B3244))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Futuristic Glowing Media Emblem
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    when (selectedMediaType) {
                                        MediaType.DIRECT_FILE -> listOf(Color(0xFF00E676), Color(0xFF00897B))
                                        MediaType.VIDEO -> listOf(Color(0xFF00E5FF), Color(0xFF0052D4))
                                        MediaType.AUDIO -> listOf(Color(0xFFB388FF), Color(0xFF651FFF))
                                    }
                                )
                            )
                            .border(
                                1.dp,
                                when (selectedMediaType) {
                                    MediaType.DIRECT_FILE -> Color(0xFF00E676).copy(0.7f)
                                    MediaType.VIDEO -> Color(0xFF00E5FF).copy(0.7f)
                                    MediaType.AUDIO -> Color(0xFFB388FF).copy(0.7f)
                                },
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (selectedMediaType) {
                                MediaType.DIRECT_FILE -> when (detectedExtension) {
                                    "apk", "xapk", "apks" -> Icons.Default.Android
                                    "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> Icons.Default.FolderZip
                                    "pdf" -> Icons.Default.PictureAsPdf
                                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                                }
                                MediaType.VIDEO -> Icons.Default.Movie
                                MediaType.AUDIO -> Icons.Default.Audiotrack
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Media Badges & Host Info
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Domain Pill
                            val domain = runCatching { Uri.parse(detectedMedia.pageUrl).host }.getOrNull() ?: "Web Stream"
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF132738))
                                    .border(1.dp, Color(0xFF1E3F59), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = domain.removePrefix("www.").removePrefix("m."),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFB0CEDB)
                                    )
                                }
                            }

                            // Duration Pill
                            detectedMedia.durationSeconds?.let { dur ->
                                val m = dur / 60
                                val s = dur % 60
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF132738))
                                        .border(1.dp, Color(0xFF1E3F59), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%02d:%02d", m, s),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFB0CEDB)
                                        )
                                    }
                                }
                            }
                        }

                        // Stream Quality Telemetry Tag
                        Text(
                            text = when (selectedMediaType) {
                                MediaType.DIRECT_FILE -> "Direct ${detectedExtension.uppercase(Locale.ROOT)} File Available"
                                MediaType.VIDEO -> "High-Definition Video Stream Available"
                                MediaType.AUDIO -> "Direct Hi-Res Audio Stream Available"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7591A3)
                        )
                    }

                    // Direct Link Copy Button
                    IconButton(
                        onClick = {
                            val directUrl = detectedMedia.directFileUrl ?: detectedMedia.pageUrl
                            clipboardManager.setText(AnnotatedString(directUrl))
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            Toast.makeText(context, "Direct media stream link copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF132738))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Stream URL",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Glassmorphic Filename Editor
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF07121B),
                    border = BorderStroke(1.dp, Color(0xFF1B3244))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )

                        BasicTextField(
                            value = editableFileName,
                            onValueChange = { editableFileName = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(Color(0xFF00E5FF)),
                            singleLine = true
                        )

                        // Extension Tag Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (selectedMediaType) {
                                        MediaType.DIRECT_FILE -> Color(0xFF00E676).copy(alpha = 0.15f)
                                        MediaType.VIDEO -> Color(0xFF142B3A)
                                        MediaType.AUDIO -> Color(0xFF651FFF).copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = ".$finalExtension",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (selectedMediaType) {
                                    MediaType.DIRECT_FILE -> Color(0xFF00E676)
                                    MediaType.VIDEO -> Color(0xFF00E5FF)
                                    MediaType.AUDIO -> Color(0xFFB388FF)
                                }
                            )
                        }

                        if (editableFileName.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF7591A3),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { editableFileName = "" }
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. STORAGE GAUGE & SPEED TELEMETRY BAR
        // =========================================================================
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0B1722),
            border = BorderStroke(1.dp, Color(0xFF162C3D))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = if (freeStorageBytes > finalTotalBytes * 2) Color(0xFF00E5FF) else Color(0xFFFFB300),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Free: ${MediaSizeCalculator.formatFileSize(freeStorageBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB0CEDB),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Download: ${MediaSizeCalculator.formatFileSize(finalTotalBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7591A3)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFF38EF7D),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = etaString,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38EF7D),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (selectedMediaType == MediaType.DIRECT_FILE) {
            // Direct File Specification Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1924)),
                border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF00E676).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (detectedExtension) {
                                        "apk", "xapk", "apks" -> Icons.Default.Android
                                        "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> Icons.Default.FolderZip
                                        "pdf" -> Icons.Default.PictureAsPdf
                                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                                    },
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = finalQualityLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Native File Extension: .${detectedExtension.lowercase(Locale.ROOT)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF88A4B5)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF00E676).copy(alpha = 0.18f))
                                .border(1.dp, Color(0xFF00E676).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = detectedExtension.uppercase(Locale.ROOT),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E676)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1B3244))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Detected Size", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7591A3))
                            Text(
                                text = if (finalTotalBytes > 0) MediaSizeCalculator.formatFileSize(finalTotalBytes) else "Server Stream (Dynamic)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Download Protocol", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7591A3))
                            Text(
                                text = "Multi-Socket Parallel ($selectedThreadCount Threads)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // 3. STAGE 1: MEDIA TYPE SELECTOR (APPLE FLOATING PILL SLIDER)
            // =========================================================================
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "1. Select Media Type",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
            )

            // Apple Floating Segmented Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0C1924),
                border = BorderStroke(1.dp, Color(0xFF1A3345))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Video Option
                    val isVideo = selectedMediaType == MediaType.VIDEO
                    val videoSource = remember { MutableInteractionSource() }
                    val isVideoPressed by videoSource.collectIsPressedAsState()
                    val videoScale by animateFloatAsState(
                        targetValue = if (isVideoPressed) 0.97f else 1f,
                        animationSpec = spring(stiffness = 500f),
                        label = "videoScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = videoScale
                                scaleY = videoScale
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isVideo) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF00E5FF).copy(0.25f), Color(0xFF0084FF).copy(0.35f))
                                    )
                                } else {
                                    SolidColor(Color.Transparent)
                                }
                            )
                            .border(
                                width = if (isVideo) 1.5.dp else 0.dp,
                                color = if (isVideo) Color(0xFF00E5FF) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(interactionSource = videoSource, indication = null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedMediaType = MediaType.VIDEO
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isVideo) Icons.Default.CheckCircle else Icons.Default.Movie,
                                contentDescription = null,
                                tint = if (isVideo) Color(0xFF00E5FF) else Color(0xFF6B8799),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Video (with Audio)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isVideo) FontWeight.Bold else FontWeight.Medium,
                                color = if (isVideo) Color.White else Color(0xFF7A97A8)
                            )
                        }
                    }

                    // Audio Option
                    val isAudio = selectedMediaType == MediaType.AUDIO
                    val audioSource = remember { MutableInteractionSource() }
                    val isAudioPressed by audioSource.collectIsPressedAsState()
                    val audioScale by animateFloatAsState(
                        targetValue = if (isAudioPressed) 0.97f else 1f,
                        animationSpec = spring(stiffness = 500f),
                        label = "audioScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = audioScale
                                scaleY = audioScale
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isAudio) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFB388FF).copy(0.25f), Color(0xFF651FFF).copy(0.35f))
                                    )
                                } else {
                                    SolidColor(Color.Transparent)
                                }
                            )
                            .border(
                                width = if (isAudio) 1.5.dp else 0.dp,
                                color = if (isAudio) Color(0xFFB388FF) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(interactionSource = audioSource, indication = null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedMediaType = MediaType.AUDIO
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isAudio) Icons.Default.CheckCircle else Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = if (isAudio) Color(0xFFB388FF) else Color(0xFF6B8799),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Audio Only",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isAudio) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAudio) Color.White else Color(0xFF7A97A8)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 4. FAST PRESET QUICK-PICKS (ONE-TAP SPEED RECOMMENDATIONS)
        // =========================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Best Quality Preset
            PresetQuickPill(
                label = "Best Quality",
                icon = Icons.Default.HighQuality,
                accentColor = Color(0xFFFFB300),
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (selectedMediaType == MediaType.VIDEO) {
                    val highest = videoQualityOptions.firstOrNull()?.height ?: 1080
                    selectedHeight = highest
                    selectedCodecName = "AV1"
                } else {
                    selectedAudioTier = "master"
                    selectedCodecName = "M4A"
                }
            }

            // Balanced Preset
            PresetQuickPill(
                label = "Balanced",
                icon = Icons.Default.Bolt,
                accentColor = Color(0xFF00E5FF),
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (selectedMediaType == MediaType.VIDEO) {
                    val balanced = videoQualityOptions.find { it.height in 720..1080 }?.height ?: 1080
                    selectedHeight = balanced
                    selectedCodecName = "H.264"
                } else {
                    selectedAudioTier = "standard"
                    selectedCodecName = "M4A"
                }
            }

            // Data Saver Preset
            PresetQuickPill(
                label = "Data Saver",
                icon = Icons.Default.Storage,
                accentColor = Color(0xFF38EF7D),
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (selectedMediaType == MediaType.VIDEO) {
                    val lowest = videoQualityOptions.lastOrNull()?.height ?: 480
                    selectedHeight = lowest
                    selectedCodecName = "AV1"
                } else {
                    selectedAudioTier = "saver"
                    selectedCodecName = "Opus"
                }
            }
        }

        // =========================================================================
        // 5. STAGE 2: RESOLUTION / AUDIO QUALITY SELECTION GRID
        // =========================================================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedMediaType == MediaType.VIDEO) "2. Select Resolution" else "2. Select Audio Quality",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )

                Text(
                    text = if (selectedMediaType == MediaType.VIDEO) "${videoQualityOptions.size} tiers available" else "Audiophile Grade",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B8799)
                )
            }

            val currentOptions = if (selectedMediaType == MediaType.VIDEO) videoQualityOptions else audioQualityOptions

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                currentOptions.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            val isSelected = if (selectedMediaType == MediaType.VIDEO) {
                                (selectedQuality?.height ?: -1) == item.height
                            } else {
                                selectedQuality?.bitrateKbps == item.bitrateKbps
                            }

                            val cardSource = remember { MutableInteractionSource() }
                            val isCardPressed by cardSource.collectIsPressedAsState()
                            val cardScale by animateFloatAsState(
                                targetValue = if (isCardPressed) 0.96f else 1f,
                                animationSpec = spring(stiffness = 500f),
                                label = "cardScale"
                            )

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        scaleX = cardScale
                                        scaleY = cardScale
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF082635) else Color(0xFF0A1824),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF162D3E)
                                ),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (selectedMediaType == MediaType.VIDEO) {
                                        selectedHeight = item.height
                                    } else {
                                        selectedAudioTier = when (item.bitrateKbps) {
                                            320 -> "master"
                                            256 -> "studio"
                                            64 -> "saver"
                                            else -> "standard"
                                        }
                                    }
                                },
                                interactionSource = cardSource
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Header: Label & Checkmark
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF00E5FF) else Color.White
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00E5FF)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color(0xFF001F29),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Size highlight
                                    Text(
                                        text = item.subLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color(0xFFC8EEF7) else Color(0xFF88A4B5)
                                    )

                                    // Badge Pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(item.badgeColor.copy(alpha = if (isSelected) 0.22f else 0.12f))
                                            .border(
                                                width = 1.dp,
                                                color = item.badgeColor.copy(alpha = if (isSelected) 0.6f else 0.25f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    ) {
                                        Text(
                                            text = item.badge,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = item.badgeColor
                                        )
                                    }
                                }
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 6. STAGE 3: CODEC SELECTION (ENCODING & COMPRESSION ARCHITECTURE)
        // =========================================================================
        AnimatedVisibility(
            visible = selectedQuality != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. Select Encoding Codec",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                    Text(
                        text = "Hardware Optimized",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B8799)
                    )
                }

                val codecList = if (selectedMediaType == MediaType.VIDEO) videoCodecOptions else audioCodecOptions

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    codecList.forEach { codecOpt ->
                        val isSelected = selectedCodec?.codecName == codecOpt.codecName
                        val codecSource = remember { MutableInteractionSource() }
                        val isCodecPressed by codecSource.collectIsPressedAsState()
                        val codecScale by animateFloatAsState(
                            targetValue = if (isCodecPressed) 0.98f else 1f,
                            animationSpec = spring(stiffness = 500f),
                            label = "codecScale"
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = codecScale
                                    scaleY = codecScale
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) codecOpt.accentColor.copy(alpha = 0.12f) else Color(0xFF0A1824),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) codecOpt.accentColor else Color(0xFF162D3E)
                            ),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCodecName = codecOpt.codecName
                            },
                            interactionSource = codecSource
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Glowing Radio Selector Ring
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = 2.dp,
                                                color = if (isSelected) codecOpt.accentColor else Color(0xFF334B5C),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(codecOpt.accentColor)
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = codecOpt.codecName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) codecOpt.accentColor else Color.White
                                            )
                                            Text(
                                                text = "(${codecOpt.tag})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF6B8799)
                                            )
                                        }

                                        Text(
                                            text = codecOpt.badge,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF90AAB9),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Size badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(codecOpt.accentColor.copy(alpha = 0.15f))
                                        .border(1.dp, codecOpt.accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = codecOpt.sizeText,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = codecOpt.accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // =========================================================================
        // 7. ADVANCED DOWNLOAD CONFIGURATION (COLLAPSIBLE TURBO CARD)
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1824)),
            border = BorderStroke(1.dp, Color(0xFF162D3E))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isAdvancedExpanded = !isAdvancedExpanded
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Advanced Download Options",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "$selectedThreadCount Parallel Threads • ${if (isWifiOnly) "Wi-Fi Only" else "All Networks"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7591A3)
                            )
                        }
                    }

                    IconButton(onClick = { isAdvancedExpanded = !isAdvancedExpanded }) {
                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF)
                        )
                    }
                }

                AnimatedVisibility(visible = isAdvancedExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HorizontalDivider(color = Color(0xFF162D3E))

                        // Thread Count Turbo Controller
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF00E5FF).copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = "Multi-Socket Accelerator",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "$selectedThreadCount Turbo Sockets",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                            }

                            Text(
                                text = "Splits download into concurrent parallel parts to bypass ISP single-stream throttles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7591A3)
                            )

                            Slider(
                                value = selectedThreadCount.toFloat(),
                                onValueChange = { selectedThreadCount = it.roundToInt() },
                                valueRange = 1f..16f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E5FF),
                                    activeTrackColor = Color(0xFF00E5FF),
                                    inactiveTrackColor = Color(0xFF1A3345)
                                )
                            )
                        }

                        // Wi-Fi Only Toggle (Enhanced pill container with zero text-switch overlap)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF07131D),
                            border = BorderStroke(1.dp, Color(0xFF162D3E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF00E5FF).copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Wifi,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Download on Wi-Fi Only",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = "Wait for Wi-Fi connection to prevent cellular data charges",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF7591A3)
                                        )
                                    }
                                }

                                Switch(
                                    checked = isWifiOnly,
                                    onCheckedChange = {
                                        isWifiOnly = it
                                        appSettings.wifiOnlyDownloads = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00E5FF),
                                        checkedTrackColor = Color(0xFF004D59),
                                        uncheckedThumbColor = Color(0xFF6B8799),
                                        uncheckedTrackColor = Color(0xFF142B3A)
                                    )
                                )
                            }
                        }

                        // Destination Directory
                        OutlinedTextField(
                            value = destinationFolder,
                            onValueChange = { destinationFolder = it },
                            label = { Text("Save Destination Directory", color = Color(0xFF7591A3)) },
                            leadingIcon = {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF1B3244),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF07121B),
                                unfocusedContainerColor = Color(0xFF07121B)
                            )
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 8. PRIMARY ACTION CTA BUTTON (ELECTRIC GLOWING PILL)
        // =========================================================================
        val buttonSource = remember { MutableInteractionSource() }
        val isButtonPressed by buttonSource.collectIsPressedAsState()
        val buttonScale by animateFloatAsState(
            targetValue = if (isButtonPressed) 0.97f else 1f,
            animationSpec = spring(stiffness = 500f),
            label = "buttonScale"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                }
                .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = if (selectedMediaType == MediaType.DIRECT_FILE) Color(0x6600E676) else Color(0x6600E5FF))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        if (selectedMediaType == MediaType.DIRECT_FILE) {
                            listOf(Color(0xFF00E676), Color(0xFF00B0FF))
                        } else {
                            listOf(Color(0xFF00E5FF), Color(0xFF0084FF))
                        }
                    )
                )
                .clickable(interactionSource = buttonSource, indication = null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    val fullFileName = if (editableFileName.endsWith(".$finalExtension", ignoreCase = true)) {
                        editableFileName
                    } else {
                        "$editableFileName.$finalExtension"
                    }

                    val downloadUrl = if (selectedMediaType == MediaType.DIRECT_FILE) {
                        detectedMedia.directFileVariant?.url ?: detectedMedia.directFileUrl ?: detectedMedia.pageUrl
                    } else {
                        selectedCodec?.matchedVariant?.url
                            ?: selectedQuality?.matchedVariant?.url
                            ?: detectedMedia.directFileUrl
                    }

                    if (downloadUrl == null) {
                        Toast.makeText(context, "No stream URL found for download", Toast.LENGTH_SHORT).show()
                        return@clickable
                    }

                    val pairedAudioUrl = if (selectedMediaType == MediaType.DIRECT_FILE) null else {
                        selectedCodec?.matchedVariant?.pairedAudioStreamUrl
                            ?: selectedQuality?.matchedVariant?.pairedAudioStreamUrl
                    }

                    val formatId = if (selectedMediaType == MediaType.DIRECT_FILE) "direct" else {
                        selectedCodec?.matchedVariant?.formatId
                            ?: selectedQuality?.matchedVariant?.formatId
                    }

                    onEnqueueDownload(
                        detectedMedia.pageTitle,
                        fullFileName,
                        downloadUrl,
                        pairedAudioUrl,
                        selectedMediaType,
                        finalQualityLabel,
                        finalCodecName,
                        selectedThreadCount,
                        destinationFolder,
                        finalTotalBytes,
                        detectedMedia.pageUrl,
                        formatId
                    )
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color(0xFF001F29),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedMediaType == MediaType.DIRECT_FILE) {
                        "Download ${detectedExtension.uppercase(Locale.ROOT)} (${if (finalTotalBytes > 0) MediaSizeCalculator.formatFileSize(finalTotalBytes) else "Direct File"})"
                    } else {
                        "Download $finalQualityLabel • $finalCodecName (${MediaSizeCalculator.formatFileSize(finalTotalBytes)})"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF001F29)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

/**
 * Fast Preset Quick-Pick Pill Button
 */
@Composable
private fun PresetQuickPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 500f),
        label = "pillScale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0A1824),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DownloadConfigBottomSheetPreview() {
    val sampleDetected = DetectedPageMedia(
        pageTitle = "Gemini 4 Carbon Is Here _Fully Test",
        pageUrl = "https://m.youtube.com/watch?v=sample",
        thumbnailUrl = null,
        durationSeconds = 1066,
        videoVariants = listOf(
            StreamVariant(
                formatId = "av01_4k",
                url = "https://example.com/video_4k.mp4",
                mediaType = MediaType.VIDEO,
                qualityLabel = "2160p 4K",
                resolutionHeight = 2160,
                bitrateKbps = 15000,
                videoCodec = VideoCodec.AV1,
                audioCodec = null,
                containerExt = "mp4",
                fileSizeBytes = 2810000000L,
                requiresAudioMerge = true
            ),
            StreamVariant(
                formatId = "avc1_1080p",
                url = "https://example.com/video_1080p.mp4",
                mediaType = MediaType.VIDEO,
                qualityLabel = "1080p FHD",
                resolutionHeight = 1080,
                bitrateKbps = 4500,
                videoCodec = VideoCodec.H264,
                audioCodec = null,
                containerExt = "mp4",
                fileSizeBytes = 815000000L,
                requiresAudioMerge = true
            )
        ),
        audioVariants = listOf(
            StreamVariant(
                formatId = "m4a_audio",
                url = "https://example.com/audio.m4a",
                mediaType = MediaType.AUDIO,
                qualityLabel = "256 kbps",
                resolutionHeight = 0,
                bitrateKbps = 256,
                videoCodec = null,
                audioCodec = AudioCodec.M4A,
                containerExt = "m4a",
                fileSizeBytes = 32500000L,
                requiresAudioMerge = false
            )
        )
    )

    com.universal.dm.ui.theme.UDMTheme {
        Surface(
            color = Color(0xFF07121B)
        ) {
            DownloadConfigContent(
                detectedMedia = sampleDetected,
                onDismiss = {},
                onEnqueueDownload = { _, _, _, _, _, _, _, _, _, _, _, _ -> }
            )
        }
    }
}
