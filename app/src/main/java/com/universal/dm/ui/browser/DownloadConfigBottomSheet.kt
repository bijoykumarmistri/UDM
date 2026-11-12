package com.universal.dm.ui.browser

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val height: Int,
    val bitrateKbps: Int,
    val matchedVariant: StreamVariant?
)

data class CodecSelectionOption(
    val codecName: String,
    val tag: String,
    val badge: String,
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
        sourcePageUrl: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        DownloadConfigContent(
            detectedMedia = detectedMedia,
            onDismiss = onDismiss,
            onEnqueueDownload = onEnqueueDownload
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        sourcePageUrl: String
    ) -> Unit
) {
    val appSettings = LocalAppSettings.current
    val scrollState = rememberScrollState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // 1. Initial State
    var selectedMediaType by remember {
        mutableStateOf(
            if (detectedMedia.videoVariants.isNotEmpty()) MediaType.VIDEO else MediaType.AUDIO
        )
    }

    // Editable Filename
    val initialSanitizedName = remember(detectedMedia.pageTitle) {
        detectedMedia.pageTitle
            .replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
            .trim()
            .take(60)
            .ifEmpty { "download_media" }
    }
    var editableFileName by remember { mutableStateOf(initialSanitizedName) }

    // Advanced options state
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var selectedThreadCount by remember { mutableIntStateOf(appSettings.threadCount) }
    var destinationFolder by remember { mutableStateOf(appSettings.defaultDownloadFolder) }

    // 2. Compute Quality Options with Mathematical Sizing
    val videoQualityOptions = remember(detectedMedia.videoVariants, detectedMedia.durationSeconds) {
        val standardHeights = listOf(
            Triple(2160, "2160p 4K", "Ultra HD"),
            Triple(1440, "1440p QHD", "Quad HD"),
            Triple(1080, "1080p FHD", "Most Popular"),
            Triple(720, "720p HD", "High Def"),
            Triple(480, "480p SD", "Standard"),
            Triple(360, "360p", "Data Saver")
        )

        val availableVariantsByHeight = detectedMedia.videoVariants.groupBy { it.resolutionHeight }
        val maxAvailableHeight = detectedMedia.videoVariants.maxOfOrNull { it.resolutionHeight } ?: 1080

        val result = mutableListOf<QualitySelectionItem>()
        val duration = detectedMedia.durationSeconds

        standardHeights.forEach { (height, label, badge) ->
            val match = availableVariantsByHeight[height]?.firstOrNull()
            val hasNearbyVariant = detectedMedia.videoVariants.any { it.resolutionHeight in (height - 60)..(height + 60) }
            val isSupportedTier = height <= maxAvailableHeight

            if (match != null || hasNearbyVariant || (isSupportedTier && detectedMedia.videoVariants.isNotEmpty())) {
                val variant = match
                    ?: detectedMedia.videoVariants.find { it.resolutionHeight in (height - 60)..(height + 60) }
                    ?: detectedMedia.videoVariants.minByOrNull { kotlin.math.abs(it.resolutionHeight - height) }

                val computedBytes = if ((variant?.fileSizeBytes ?: 0L) > 0 && variant?.resolutionHeight == height) {
                    variant.fileSizeBytes
                } else {
                    MediaSizeCalculator.calculateVideoTotalBytes(
                        height = height,
                        codec = variant?.videoCodec ?: VideoCodec.H264,
                        durationSeconds = duration,
                        explicitVideoBytes = variant?.fileSizeBytes?.takeIf { it > 0 },
                        variantBitrateKbps = variant?.bitrateKbps?.takeIf { it > 0 }
                    )
                }

                result.add(
                    QualitySelectionItem(
                        label = label,
                        subLabel = MediaSizeCalculator.formatFileSize(computedBytes),
                        badge = badge,
                        height = height,
                        bitrateKbps = variant?.bitrateKbps ?: 0,
                        matchedVariant = variant?.copy(fileSizeBytes = computedBytes) ?: variant
                    )
                )
            }
        }

        // Add any remaining unique variants from source streams
        detectedMedia.videoVariants.forEach { v ->
            if (v.resolutionHeight > 0 && result.none { it.height == v.resolutionHeight }) {
                val computedBytes = if (v.fileSizeBytes > 0) {
                    v.fileSizeBytes
                } else {
                    MediaSizeCalculator.calculateVideoTotalBytes(
                        height = v.resolutionHeight,
                        codec = v.videoCodec ?: VideoCodec.H264,
                        durationSeconds = duration,
                        variantBitrateKbps = v.bitrateKbps.takeIf { it > 0 }
                    )
                }
                result.add(
                    QualitySelectionItem(
                        label = v.qualityLabel,
                        subLabel = MediaSizeCalculator.formatFileSize(computedBytes),
                        badge = "Native",
                        height = v.resolutionHeight,
                        bitrateKbps = v.bitrateKbps,
                        matchedVariant = v.copy(fileSizeBytes = computedBytes)
                    )
                )
            }
        }

        if (result.isEmpty()) {
            standardHeights.filter { it.first <= 1080 }.forEach { (height, label, badge) ->
                val fallbackBytes = MediaSizeCalculator.calculateVideoTotalBytes(
                    height = height,
                    codec = VideoCodec.H264,
                    durationSeconds = duration
                )
                result.add(
                    QualitySelectionItem(
                        label = label,
                        subLabel = MediaSizeCalculator.formatFileSize(fallbackBytes),
                        badge = badge,
                        height = height,
                        bitrateKbps = MediaSizeCalculator.getBaselineVideoBitrateKbps(height),
                        matchedVariant = null
                    )
                )
            }
        }
        result.sortedByDescending { it.height }
    }

    val audioQualityOptions = remember(detectedMedia.audioVariants, detectedMedia.durationSeconds) {
        val duration = detectedMedia.durationSeconds
        val maxKbpsVariant = detectedMedia.audioVariants.maxByOrNull { it.bitrateKbps }
        val midKbpsVariant = detectedMedia.audioVariants.find { it.bitrateKbps in 120..192 } ?: detectedMedia.audioVariants.firstOrNull()
        val minKbpsVariant = detectedMedia.audioVariants.minByOrNull { it.bitrateKbps }

        val highBytes = if ((maxKbpsVariant?.fileSizeBytes ?: 0L) > 0 && (maxKbpsVariant?.bitrateKbps ?: 0) in 240..320) {
            maxKbpsVariant!!.fileSizeBytes
        } else {
            MediaSizeCalculator.calculateAudioTotalBytes(256, AudioCodec.M4A, duration)
        }

        val stdBytes = if ((midKbpsVariant?.fileSizeBytes ?: 0L) > 0 && (midKbpsVariant?.bitrateKbps ?: 0) in 120..192) {
            midKbpsVariant!!.fileSizeBytes
        } else {
            MediaSizeCalculator.calculateAudioTotalBytes(128, AudioCodec.M4A, duration)
        }

        val saverBytes = if ((minKbpsVariant?.fileSizeBytes ?: 0L) > 0 && (minKbpsVariant?.bitrateKbps ?: 0) in 48..96) {
            minKbpsVariant!!.fileSizeBytes
        } else {
            MediaSizeCalculator.calculateAudioTotalBytes(64, AudioCodec.M4A, duration)
        }

        listOf(
            QualitySelectionItem(
                label = "High Quality",
                subLabel = "${MediaSizeCalculator.formatFileSize(highBytes)} (~256 kbps)",
                badge = "Crisp Sound",
                height = 0,
                bitrateKbps = 256,
                matchedVariant = maxKbpsVariant?.copy(fileSizeBytes = highBytes) ?: maxKbpsVariant
            ),
            QualitySelectionItem(
                label = "Standard Quality",
                subLabel = "${MediaSizeCalculator.formatFileSize(stdBytes)} (~128 kbps)",
                badge = "Balanced",
                height = 0,
                bitrateKbps = 128,
                matchedVariant = midKbpsVariant?.copy(fileSizeBytes = stdBytes) ?: midKbpsVariant
            ),
            QualitySelectionItem(
                label = "Data Saver",
                subLabel = "${MediaSizeCalculator.formatFileSize(saverBytes)} (~64 kbps)",
                badge = "Minimal Data",
                height = 0,
                bitrateKbps = 64,
                matchedVariant = minKbpsVariant?.copy(fileSizeBytes = saverBytes) ?: minKbpsVariant
            )
        )
    }

    // Selected Quality (Stage 2) - Track with stable height/tier keys so quality changes never glitch or reset
    val defaultVideoHeight = videoQualityOptions.firstOrNull()?.height ?: 1080
    var selectedHeight by remember(detectedMedia.pageUrl) { mutableIntStateOf(defaultVideoHeight) }
    var selectedAudioTier by remember(detectedMedia.pageUrl) { mutableStateOf("standard") }

    val selectedQuality = if (selectedMediaType == MediaType.VIDEO) {
        videoQualityOptions.find { it.height == selectedHeight } ?: videoQualityOptions.firstOrNull()
    } else {
        audioQualityOptions.find {
            when (selectedAudioTier) {
                "high" -> it.label.contains("High", ignoreCase = true)
                "saver" -> it.label.contains("Saver", ignoreCase = true)
                else -> it.label.contains("Standard", ignoreCase = true)
            }
        } ?: audioQualityOptions.firstOrNull()
    }

    // Selected Codec Name (Store stable string key so changing quality never glitches/resets selection)
    var selectedCodecName by remember(selectedMediaType) {
        mutableStateOf(
            if (selectedMediaType == MediaType.VIDEO) "AV1" else "M4A"
        )
    }

    // 3. Compute Codec Options (Stage 3) based on chosen Quality and exact mathematical calculations
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
                badge = "Best Quality • Smallest Size",
                sizeText = MediaSizeCalculator.formatFileSize(av1Bytes),
                accentColor = CodecAv1Badge,
                matchedVariant = av1Match?.copy(fileSizeBytes = av1Bytes) ?: q?.matchedVariant?.copy(fileSizeBytes = av1Bytes),
                extension = "mp4"
            ),
            CodecSelectionOption(
                codecName = "H.264",
                tag = "avc1",
                badge = "Maximum Compatibility",
                sizeText = MediaSizeCalculator.formatFileSize(h264Bytes),
                accentColor = primaryColor,
                matchedVariant = h264Match?.copy(fileSizeBytes = h264Bytes) ?: q?.matchedVariant?.copy(fileSizeBytes = h264Bytes),
                extension = "mp4"
            ),
            CodecSelectionOption(
                codecName = "VP9",
                tag = "vp9",
                badge = "High Definition",
                sizeText = MediaSizeCalculator.formatFileSize(vp9Bytes),
                accentColor = secondaryColor,
                matchedVariant = vp9Match?.copy(fileSizeBytes = vp9Bytes) ?: q?.matchedVariant?.copy(fileSizeBytes = vp9Bytes),
                extension = "mkv"
            )
        )
    }

    val audioCodecOptions = remember(selectedQuality, detectedMedia.audioVariants, detectedMedia.durationSeconds) {
        val q = selectedQuality
        val baseRate = q?.bitrateKbps ?: 128
        val duration = detectedMedia.durationSeconds

        val m4aMatch = detectedMedia.audioVariants.find { it.audioCodec == AudioCodec.M4A }
        val opusMatch = detectedMedia.audioVariants.find { it.audioCodec == AudioCodec.OPUS }
        val mp3Match = detectedMedia.audioVariants.find { it.audioCodec == AudioCodec.MP3 }

        val m4aBytes = if ((m4aMatch?.fileSizeBytes ?: 0L) > 0 && m4aMatch?.bitrateKbps == baseRate) {
            m4aMatch.fileSizeBytes
        } else {
            MediaSizeCalculator.calculateAudioTotalBytes(baseRate, AudioCodec.M4A, duration)
        }

        val opusBytes = if ((opusMatch?.fileSizeBytes ?: 0L) > 0 && opusMatch?.bitrateKbps == baseRate) {
            opusMatch.fileSizeBytes
        } else {
            MediaSizeCalculator.calculateAudioTotalBytes(baseRate, AudioCodec.OPUS, duration)
        }

        val mp3Bytes = if ((mp3Match?.fileSizeBytes ?: 0L) > 0 && mp3Match?.bitrateKbps == baseRate) {
            mp3Match.fileSizeBytes
        } else {
            MediaSizeCalculator.calculateAudioTotalBytes(baseRate, AudioCodec.MP3, duration)
        }

        listOf(
            CodecSelectionOption(
                codecName = "M4A",
                tag = "mp4a",
                badge = "Recommended • Native AAC",
                sizeText = MediaSizeCalculator.formatFileSize(m4aBytes),
                accentColor = CodecM4aBadge,
                matchedVariant = m4aMatch?.copy(fileSizeBytes = m4aBytes) ?: q?.matchedVariant?.copy(fileSizeBytes = m4aBytes),
                extension = "m4a"
            ),
            CodecSelectionOption(
                codecName = "Opus",
                tag = "opus",
                badge = "High Efficiency",
                sizeText = MediaSizeCalculator.formatFileSize(opusBytes),
                accentColor = tertiaryColor,
                matchedVariant = opusMatch?.copy(fileSizeBytes = opusBytes) ?: q?.matchedVariant?.copy(fileSizeBytes = opusBytes),
                extension = "opus"
            ),
            CodecSelectionOption(
                codecName = "MP3",
                tag = "mp3",
                badge = "Universal Audio",
                sizeText = MediaSizeCalculator.formatFileSize(mp3Bytes),
                accentColor = secondaryColor,
                matchedVariant = mp3Match?.copy(fileSizeBytes = mp3Bytes) ?: q?.matchedVariant?.copy(fileSizeBytes = mp3Bytes),
                extension = "mp3"
            )
        )
    }

    // Active Codec derived smoothly from current options and selected name
    val activeCodecOptions = if (selectedMediaType == MediaType.VIDEO) videoCodecOptions else audioCodecOptions
    val selectedCodec = activeCodecOptions.find { it.codecName.equals(selectedCodecName, ignoreCase = true) }
        ?: activeCodecOptions.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // 1. HEADER SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media Thumbnail / Category Icon Box
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedMediaType == MediaType.VIDEO) Icons.Default.Movie else Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Badges row: Domain & Duration
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Domain Badge
                        val domain = runCatching { Uri.parse(detectedMedia.pageUrl).host }.getOrNull() ?: "Web Stream"
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = domain.removePrefix("www.").removePrefix("m."),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = MaterialTheme.shapes.small,
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            border = null
                        )

                        // Duration Badge
                        detectedMedia.durationSeconds?.let { dur ->
                            val m = dur / 60
                            val s = dur % 60
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = String.format(Locale.US, "%02d:%02d", m, s),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = MaterialTheme.shapes.small,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                border = null
                            )
                        }
                    }

                    // Editable Filename TextField
                    OutlinedTextField(
                        value = editableFileName,
                        onValueChange = { editableFileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("File Name") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)

            // 2. STAGE 1: MEDIA TYPE SELECTOR (Video vs Audio)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Select Media Type",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedMediaType == MediaType.VIDEO,
                        onClick = {
                            selectedMediaType = MediaType.VIDEO
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = selectedMediaType == MediaType.VIDEO) {
                                Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    ) {
                        Text("Video (with Audio)", fontWeight = FontWeight.SemiBold)
                    }

                    SegmentedButton(
                        selected = selectedMediaType == MediaType.AUDIO,
                        onClick = {
                            selectedMediaType = MediaType.AUDIO
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = selectedMediaType == MediaType.AUDIO) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    ) {
                        Text("Audio Only", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 3. STAGE 2: QUALITY GRID
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (selectedMediaType == MediaType.VIDEO) "2. Select Resolution" else "2. Select Audio Quality",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val currentOptions = if (selectedMediaType == MediaType.VIDEO) videoQualityOptions else audioQualityOptions

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    currentOptions.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                val isSelected = if (selectedMediaType == MediaType.VIDEO) {
                                    (selectedQuality?.height ?: -1) == item.height
                                } else {
                                    selectedQuality?.label == item.label
                                }
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (selectedMediaType == MediaType.VIDEO) {
                                                selectedHeight = item.height
                                            } else {
                                                selectedAudioTier = when {
                                                    item.label.contains("High", ignoreCase = true) -> "high"
                                                    item.label.contains("Saver", ignoreCase = true) -> "saver"
                                                    else -> "standard"
                                                }
                                            }
                                        }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = MaterialTheme.shapes.medium
                                        ),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = item.subLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        SuggestionChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    text = item.badge,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            shape = MaterialTheme.shapes.extraSmall,
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                            ),
                                            border = null
                                        )
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

            // 4. STAGE 3: CODEC SELECTION CHIPS (Revealed smoothly on quality select)
            AnimatedVisibility(
                visible = selectedQuality != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "3. Select Codec",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    val codecList = if (selectedMediaType == MediaType.VIDEO) videoCodecOptions else audioCodecOptions

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        codecList.forEach { codecOpt ->
                            val isSelected = selectedCodec?.codecName == codecOpt.codecName
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCodecName = codecOpt.codecName }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) codecOpt.accentColor else MaterialTheme.colorScheme.outlineVariant,
                                        shape = MaterialTheme.shapes.medium
                                    ),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) codecOpt.accentColor.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) codecOpt.accentColor else Color.Transparent)
                                                .border(
                                                    width = 2.dp,
                                                    color = codecOpt.accentColor,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = codecOpt.codecName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isSelected) codecOpt.accentColor else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "(${codecOpt.tag})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Text(
                                                text = codecOpt.badge,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Size info
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

            // 5. ADVANCED OPTIONS (Expandable row)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAdvancedExpanded = !isAdvancedExpanded },
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
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Advanced Download Options",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(onClick = { isAdvancedExpanded = !isAdvancedExpanded }) {
                            Icon(
                                imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(visible = isAdvancedExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Thread count
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Parallel Download Threads",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "$selectedThreadCount Threads",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Slider(
                                    value = selectedThreadCount.toFloat(),
                                    onValueChange = { selectedThreadCount = it.roundToInt() },
                                    valueRange = 1f..16f,
                                    steps = 14
                                )
                            }

                            // Destination directory
                            OutlinedTextField(
                                value = destinationFolder,
                                onValueChange = { destinationFolder = it },
                                label = { Text("Download Directory") },
                                leadingIcon = {
                                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // 6. BOTTOM PRIMARY ACTION BUTTON
            val finalCodecName = selectedCodec?.codecName ?: if (selectedMediaType == MediaType.VIDEO) "AV1" else "M4A"
            val finalQualityLabel = selectedQuality?.label ?: if (selectedMediaType == MediaType.VIDEO) "1080p" else "High"
            val finalExtension = selectedCodec?.extension ?: if (selectedMediaType == MediaType.VIDEO) "mp4" else "m4a"

            val finalTotalBytes = if (selectedMediaType == MediaType.VIDEO) {
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
            } else {
                selectedCodec?.matchedVariant?.fileSizeBytes?.takeIf { it > 0 }
                    ?: selectedQuality?.matchedVariant?.fileSizeBytes?.takeIf { it > 0 }
                    ?: MediaSizeCalculator.calculateAudioTotalBytes(
                        targetBitrateKbps = selectedQuality?.bitrateKbps ?: 128,
                        codec = when (finalCodecName) {
                            "Opus" -> AudioCodec.OPUS
                            "MP3" -> AudioCodec.MP3
                            else -> AudioCodec.M4A
                        },
                        durationSeconds = detectedMedia.durationSeconds
                    )
            }

            Button(
                onClick = {
                    val fullFileName = if (editableFileName.endsWith(".$finalExtension", ignoreCase = true)) {
                        editableFileName
                    } else {
                        "$editableFileName.$finalExtension"
                    }

                    val downloadUrl = selectedCodec?.matchedVariant?.url
                        ?: selectedQuality?.matchedVariant?.url
                        ?: detectedMedia.directFileUrl
                        ?: detectedMedia.pageUrl

                    val pairedAudioUrl = selectedCodec?.matchedVariant?.pairedAudioStreamUrl
                        ?: selectedQuality?.matchedVariant?.pairedAudioStreamUrl

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
                        detectedMedia.pageUrl
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Download $finalQualityLabel • $finalCodecName (${MediaSizeCalculator.formatFileSize(finalTotalBytes)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun DownloadConfigBottomSheetPreview() {
    val sampleDetected = DetectedPageMedia(
        pageTitle = "Cosmos Laundromat - 4K Sci-Fi Short Movie",
        pageUrl = "https://m.youtube.com/watch?v=sample",
        thumbnailUrl = null,
        durationSeconds = 730,
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
                fileSizeBytes = 425000000L,
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
                fileSizeBytes = 125000000L,
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
                fileSizeBytes = 24000000L,
                requiresAudioMerge = false
            )
        )
    )

    com.universal.dm.ui.theme.UDMTheme {
        androidx.compose.material3.Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            DownloadConfigContent(
                detectedMedia = sampleDetected,
                onDismiss = {},
                onEnqueueDownload = { _, _, _, _, _, _, _, _, _, _, _ -> }
            )
        }
    }
}
