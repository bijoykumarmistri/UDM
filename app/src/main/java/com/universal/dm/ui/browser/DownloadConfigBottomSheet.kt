package com.universal.dm.ui.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.universal.dm.data.model.AudioCodec
import com.universal.dm.data.model.DetectedPageMedia
import com.universal.dm.data.model.MediaType
import com.universal.dm.data.model.StreamVariant
import com.universal.dm.data.model.VideoCodec
import com.universal.dm.domain.sniffer.MediaSizeCalculator
import com.universal.dm.ui.components.UdmIconSquircle
import com.universal.dm.ui.settings.LocalAppSettings
import com.universal.dm.ui.theme.UDMTheme
import com.universal.dm.ui.theme.UdmTextStyles
import com.universal.dm.ui.theme.UdmTheme
import java.util.Locale
import kotlin.math.roundToInt

data class QualitySelectionItem(
    val label: String,
    val sizeLabel: String,
    val height: Int,
    val bitrateKbps: Int,
    val matchedVariant: StreamVariant?
)

data class CodecSelectionOption(
    val codecName: String,
    val sizeText: String,
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
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colorScheme.surfaceContainerLow
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
    val appSettings = LocalAppSettings.current
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = UdmTheme.extendedColors

    val isDirectFile = detectedMedia.isDirectFile ||
        (detectedMedia.videoVariants.isEmpty() && detectedMedia.audioVariants.isEmpty())

    val detectedExtension = remember(detectedMedia) {
        val fromVariant = detectedMedia.directFileVariant?.containerExt?.takeIf { it.isNotBlank() }
        val fromUrl = detectedMedia.directFileUrl?.let { it.toUri().path?.substringAfterLast('.', "") }?.takeIf { it.isNotBlank() }
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

    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var selectedThreadCount by remember { mutableIntStateOf(appSettings.threadCount.coerceIn(1, 16)) }
    var destinationFolder by remember { mutableStateOf(appSettings.defaultDownloadFolder) }
    var isWifiOnly by remember { mutableStateOf(appSettings.wifiOnlyDownloads) }

    val freeStorageBytes = remember {
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        }.getOrDefault(48_000_000_000L)
    }

    val videoQualityOptions = remember(detectedMedia.videoVariants, detectedMedia.durationSeconds) {
        val standardHeights = listOf(
            2160 to "2160p 4K",
            1440 to "1440p",
            1080 to "1080p",
            720 to "720p",
            480 to "480p",
            360 to "360p"
        )

        val availableVariantsByHeight = detectedMedia.videoVariants.groupBy { it.resolutionHeight }
        val result = mutableListOf<QualitySelectionItem>()
        val duration = detectedMedia.durationSeconds

        standardHeights.forEach { (height, label) ->
            val match = availableVariantsByHeight[height]?.firstOrNull()
            val hasNearbyVariant = detectedMedia.videoVariants.find { it.resolutionHeight in (height - 60)..(height + 60) }

            if (match != null || hasNearbyVariant != null) {
                val variant = match ?: hasNearbyVariant
                val isExactOrNearby = variant != null && kotlin.math.abs(variant.resolutionHeight - height) <= 60
                val computedBytes = if (isExactOrNearby && (variant.fileSizeBytes) > 0) {
                    variant.fileSizeBytes
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
                        sizeLabel = MediaSizeCalculator.formatFileSize(computedBytes),
                        height = height,
                        bitrateKbps = variant?.bitrateKbps ?: 0,
                        matchedVariant = variant?.copy(fileSizeBytes = computedBytes) ?: variant
                    )
                )
            }
        }

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
                        sizeLabel = MediaSizeCalculator.formatFileSize(computedBytes),
                        height = v.resolutionHeight,
                        bitrateKbps = v.bitrateKbps,
                        matchedVariant = v.copy(fileSizeBytes = computedBytes)
                    )
                )
            }
        }

        if (result.isEmpty() && detectedMedia.directFileUrl != null && !isDirectFile) {
            standardHeights.filter { it.first <= 1080 }.forEach { (height, label) ->
                val fallbackBytes = MediaSizeCalculator.calculateVideoTotalBytes(
                    height = height,
                    codec = VideoCodec.H264,
                    durationSeconds = duration,
                    requiresAudioMerge = false
                )
                result.add(
                    QualitySelectionItem(
                        label = label,
                        sizeLabel = MediaSizeCalculator.formatFileSize(fallbackBytes),
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
            emptyList()
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
                QualitySelectionItem("320 kbps", MediaSizeCalculator.formatFileSize(masterBytes), 0, 320, maxKbpsVariant?.copy(fileSizeBytes = masterBytes, bitrateKbps = 320) ?: maxKbpsVariant),
                QualitySelectionItem("256 kbps", MediaSizeCalculator.formatFileSize(studioBytes), 0, 256, maxKbpsVariant?.copy(fileSizeBytes = studioBytes, bitrateKbps = 256) ?: maxKbpsVariant),
                QualitySelectionItem("128 kbps", MediaSizeCalculator.formatFileSize(stdBytes), 0, 128, midKbpsVariant?.copy(fileSizeBytes = stdBytes, bitrateKbps = 128) ?: midKbpsVariant),
                QualitySelectionItem("64 kbps", MediaSizeCalculator.formatFileSize(saverBytes), 0, 64, minKbpsVariant?.copy(fileSizeBytes = saverBytes, bitrateKbps = 64) ?: minKbpsVariant)
            )
        }
    }

    val targetHeight = appSettings.preferredVideoQuality
    val defaultVideoHeight = videoQualityOptions
        .filter { it.height <= targetHeight }
        .maxByOrNull { it.height }?.height
        ?: videoQualityOptions.lastOrNull()?.height
        ?: 1080
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

    var selectedCodecName by remember(selectedMediaType) {
        mutableStateOf(if (selectedMediaType == MediaType.VIDEO) "AV1" else "M4A")
    }

    val videoCodecOptions = remember(selectedQuality, detectedMedia.videoVariants, detectedMedia.durationSeconds) {
        val q = selectedQuality
        val height = q?.height ?: 1080
        val duration = detectedMedia.durationSeconds
        val matchingVariants = detectedMedia.videoVariants.filter { kotlin.math.abs(it.resolutionHeight - height) <= 80 }

        val av1Match = matchingVariants.find { it.videoCodec == VideoCodec.AV1 }
        val h264Match = matchingVariants.find { it.videoCodec == VideoCodec.H264 }
        val vp9Match = matchingVariants.find { it.videoCodec == VideoCodec.VP9 }

        val av1Bytes = av1Match?.fileSizeBytes?.takeIf { it > 0 } ?: MediaSizeCalculator.calculateVideoTotalBytes(height, VideoCodec.AV1, duration, variantBitrateKbps = av1Match?.bitrateKbps?.takeIf { it > 0 })
        val h264Bytes = h264Match?.fileSizeBytes?.takeIf { it > 0 } ?: MediaSizeCalculator.calculateVideoTotalBytes(height, VideoCodec.H264, duration, variantBitrateKbps = h264Match?.bitrateKbps?.takeIf { it > 0 })
        val vp9Bytes = vp9Match?.fileSizeBytes?.takeIf { it > 0 } ?: MediaSizeCalculator.calculateVideoTotalBytes(height, VideoCodec.VP9, duration, variantBitrateKbps = vp9Match?.bitrateKbps?.takeIf { it > 0 })

        listOf(
            CodecSelectionOption("AV1", MediaSizeCalculator.formatFileSize(av1Bytes), av1Match?.copy(fileSizeBytes = av1Bytes) ?: q?.matchedVariant?.copy(fileSizeBytes = av1Bytes), "mp4"),
            CodecSelectionOption("H.264", MediaSizeCalculator.formatFileSize(h264Bytes), h264Match?.copy(fileSizeBytes = h264Bytes) ?: q?.matchedVariant?.copy(fileSizeBytes = h264Bytes), "mp4"),
            CodecSelectionOption("VP9", MediaSizeCalculator.formatFileSize(vp9Bytes), vp9Match?.copy(fileSizeBytes = vp9Bytes) ?: q?.matchedVariant?.copy(fileSizeBytes = vp9Bytes), "mkv")
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
            CodecSelectionOption("M4A", MediaSizeCalculator.formatFileSize(m4aBytes), m4aMatch?.copy(fileSizeBytes = m4aBytes, bitrateKbps = baseRate) ?: q?.matchedVariant?.copy(fileSizeBytes = m4aBytes, bitrateKbps = baseRate), "m4a"),
            CodecSelectionOption("Opus", MediaSizeCalculator.formatFileSize(opusBytes), opusMatch?.copy(fileSizeBytes = opusBytes, bitrateKbps = baseRate) ?: q?.matchedVariant?.copy(fileSizeBytes = opusBytes, bitrateKbps = baseRate), "opus"),
            CodecSelectionOption("MP3", MediaSizeCalculator.formatFileSize(mp3Bytes), mp3Match?.copy(fileSizeBytes = mp3Bytes, bitrateKbps = baseRate) ?: q?.matchedVariant?.copy(fileSizeBytes = mp3Bytes, bitrateKbps = baseRate), "mp3")
        )
    }

    val activeCodecOptions = if (selectedMediaType == MediaType.VIDEO) videoCodecOptions else audioCodecOptions
    val selectedCodec = activeCodecOptions.find { it.codecName.equals(selectedCodecName, ignoreCase = true) }
        ?: activeCodecOptions.firstOrNull()

    val finalCodecName = when (selectedMediaType) {
        MediaType.DIRECT_FILE -> detectedExtension.uppercase(Locale.ROOT)
        MediaType.VIDEO -> selectedCodec?.codecName ?: "AV1"
        MediaType.AUDIO -> selectedCodec?.codecName ?: "M4A"
    }

    val finalQualityLabel = when (selectedMediaType) {
        MediaType.DIRECT_FILE -> detectedMedia.directFileVariant?.qualityLabel ?: "${detectedExtension.uppercase(Locale.ROOT)} File"
        MediaType.VIDEO -> selectedQuality?.label ?: "1080p"
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

    val emblemBgColor = when (selectedMediaType) {
        MediaType.DIRECT_FILE -> extendedColors.successContainer
        MediaType.VIDEO -> colorScheme.primaryContainer
        MediaType.AUDIO -> colorScheme.tertiaryContainer
    }
    val emblemContentColor = when (selectedMediaType) {
        MediaType.DIRECT_FILE -> extendedColors.onSuccessContainer
        MediaType.VIDEO -> colorScheme.onPrimaryContainer
        MediaType.AUDIO -> colorScheme.onTertiaryContainer
    }

    val domain = remember(detectedMedia.pageUrl) {
        runCatching { detectedMedia.pageUrl.toUri().host?.removePrefix("www.")?.removePrefix("m.") }.getOrNull() ?: "Web"
    }
    val durationText = remember(detectedMedia.durationSeconds) {
        detectedMedia.durationSeconds?.let { dur ->
            String.format(Locale.US, "%02d:%02d", dur / 60, dur % 60)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HEADER & EDITABLE FILE NAME
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UdmIconSquircle(
                    icon = when (selectedMediaType) {
                        MediaType.DIRECT_FILE -> when (detectedExtension) {
                            "apk", "xapk", "apks" -> Icons.Default.Android
                            "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> Icons.Default.FolderZip
                            "pdf" -> Icons.Default.PictureAsPdf
                            else -> Icons.AutoMirrored.Filled.InsertDriveFile
                        }
                        MediaType.VIDEO -> Icons.Default.Movie
                        MediaType.AUDIO -> Icons.Default.Audiotrack
                    },
                    containerColor = emblemBgColor,
                    contentColor = emblemContentColor,
                    size = 46.dp,
                    iconSize = 23.dp,
                    cornerRadius = 14.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDirectFile) "Download file" else "Download media",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                    val metaLine = listOfNotNull(
                        domain,
                        durationText,
                        "${MediaSizeCalculator.formatFileSize(freeStorageBytes)} free"
                    ).joinToString(" • ")
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        val directUrl = detectedMedia.directFileUrl ?: detectedMedia.pageUrl
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Stream URL", directUrl))
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy link",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Filename Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )

                    BasicTextField(
                        value = editableFileName,
                        onValueChange = { editableFileName = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(colorScheme.primary),
                        singleLine = true
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(emblemBgColor)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = ".$finalExtension",
                            style = MaterialTheme.typography.labelSmall,
                            color = emblemContentColor
                        )
                    }

                    if (editableFileName.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { editableFileName = "" }
                        )
                    }
                }
            }
        }

        // 2. DIRECT FILE SUMMARY OR VIDEO/AUDIO SELECTORS
        if (selectedMediaType == MediaType.DIRECT_FILE) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("File type", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        Text(
                            text = ".${detectedExtension.lowercase(Locale.ROOT)} • $finalQualityLabel",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Size", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        Text(
                            text = if (finalTotalBytes > 0) MediaSizeCalculator.formatFileSize(finalTotalBytes) else "Unknown",
                            style = UdmTextStyles.TabularMetricSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.primary
                        )
                    }
                }
            }
        } else {
            // Media Type Segmented Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(colorScheme.surfaceContainer)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple(MediaType.VIDEO, "Video", Icons.Default.Movie),
                    Triple(MediaType.AUDIO, "Audio only", Icons.Default.Audiotrack)
                ).forEach { (type, label, icon) ->
                    val selected = selectedMediaType == type
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedMediaType = type
                        },
                        modifier = Modifier.weight(1f),
                        shape = CircleShape,
                        color = if (selected) colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quality Grid (2 columns)
            val currentOptions = if (selectedMediaType == MediaType.VIDEO) videoQualityOptions else audioQualityOptions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (selectedMediaType == MediaType.VIDEO) "Resolution" else "Audio quality",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurfaceVariant
                )

                currentOptions.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            val isSelected = if (selectedMediaType == MediaType.VIDEO) {
                                (selectedQuality?.height ?: -1) == item.height
                            } else {
                                selectedQuality?.bitrateKbps == item.bitrateKbps
                            }

                            Surface(
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
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                            ),
                                            color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                        )
                                        Text(
                                            text = item.sizeLabel,
                                            style = UdmTextStyles.TabularMetricSmall,
                                            color = if (isSelected) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
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

            // Format / Codec 3-pill Row
            if (selectedQuality != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Format",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeCodecOptions.forEach { codecOpt ->
                            val isSelected = selectedCodec?.codecName == codecOpt.codecName
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCodecName = codecOpt.codecName
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) colorScheme.secondaryContainer else colorScheme.surfaceContainer
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = codecOpt.codecName,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) colorScheme.onSecondaryContainer else colorScheme.onSurface
                                    )
                                    Text(
                                        text = codecOpt.sizeText,
                                        style = UdmTextStyles.TabularMetricSmall.copy(fontSize = 11.sp),
                                        color = if (isSelected) colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. COLLAPSED OPTIONS
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAdvancedExpanded = !isAdvancedExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Options • $selectedThreadCount connections${if (isWifiOnly) " • Wi-Fi" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = isAdvancedExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Parallel connections", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                Text("$selectedThreadCount", style = UdmTextStyles.TabularMetricSmall, color = colorScheme.primary)
                            }
                            Slider(
                                value = selectedThreadCount.toFloat(),
                                onValueChange = { selectedThreadCount = it.roundToInt() },
                                valueRange = 1f..16f,
                                steps = 14
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Wi-Fi only", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurface)
                            Switch(
                                checked = isWifiOnly,
                                onCheckedChange = {
                                    isWifiOnly = it
                                    appSettings.wifiOnlyDownloads = it
                                }
                            )
                        }

                        OutlinedTextField(
                            value = destinationFolder,
                            onValueChange = { destinationFolder = it },
                            label = { Text("Save folder") },
                            leadingIcon = {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // 4. DOWNLOAD CTA
        val ctaContainerColor = if (selectedMediaType == MediaType.DIRECT_FILE) extendedColors.success else colorScheme.primary
        val ctaContentColor = if (selectedMediaType == MediaType.DIRECT_FILE) extendedColors.onSuccess else colorScheme.onPrimary

        Button(
            onClick = {
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
                    Toast.makeText(context, "No download URL found", Toast.LENGTH_SHORT).show()
                    return@Button
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
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = ctaContainerColor,
                contentColor = ctaContentColor
            )
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedMediaType == MediaType.DIRECT_FILE) {
                    if (finalTotalBytes > 0) "Download (${MediaSizeCalculator.formatFileSize(finalTotalBytes)})" else "Download"
                } else {
                    "Download $finalQualityLabel (${MediaSizeCalculator.formatFileSize(finalTotalBytes)})"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Preview(name = "Download Config - Light", showBackground = true)
@Composable
fun DownloadConfigBottomSheetLightPreview() {
    val sampleDetected = DetectedPageMedia(
        pageTitle = "Sample 4K Video",
        pageUrl = "https://m.youtube.com/watch?v=sample",
        thumbnailUrl = null,
        durationSeconds = 1066,
        videoVariants = listOf(
            StreamVariant("av01_4k", "https://example.com/v4k.mp4", MediaType.VIDEO, "2160p 4K", 2160, 15000, VideoCodec.AV1, null, "mp4", 2810000000L, true),
            StreamVariant("avc1_1080p", "https://example.com/v1080.mp4", MediaType.VIDEO, "1080p", 1080, 4500, VideoCodec.H264, null, "mp4", 815000000L, true)
        ),
        audioVariants = listOf(
            StreamVariant("m4a_audio", "https://example.com/a.m4a", MediaType.AUDIO, "256 kbps", 0, 256, null, AudioCodec.M4A, "m4a", 32500000L, false)
        )
    )

    UDMTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            DownloadConfigContent(
                detectedMedia = sampleDetected,
                onDismiss = {},
                onEnqueueDownload = { _, _, _, _, _, _, _, _, _, _, _, _ -> }
            )
        }
    }
}

@Preview(name = "Download Config - Dark", showBackground = true)
@Composable
fun DownloadConfigBottomSheetDarkPreview() {
    val sampleDetected = DetectedPageMedia(
        pageTitle = "Sample 4K Video",
        pageUrl = "https://m.youtube.com/watch?v=sample",
        thumbnailUrl = null,
        durationSeconds = 1066,
        videoVariants = listOf(
            StreamVariant("av01_4k", "https://example.com/v4k.mp4", MediaType.VIDEO, "2160p 4K", 2160, 15000, VideoCodec.AV1, null, "mp4", 2810000000L, true)
        ),
        audioVariants = emptyList()
    )

    UDMTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            DownloadConfigContent(
                detectedMedia = sampleDetected,
                onDismiss = {},
                onEnqueueDownload = { _, _, _, _, _, _, _, _, _, _, _, _ -> }
            )
        }
    }
}
