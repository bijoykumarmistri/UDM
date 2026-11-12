package com.universal.dm.ui.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universal.dm.data.local.DownloadChunkEntity
import com.universal.dm.data.local.DownloadTaskEntity
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.data.model.MediaType
import com.universal.dm.domain.downloader.DownloadEngineHelper
import com.universal.dm.ui.theme.CodecAv1Badge
import com.universal.dm.ui.theme.CodecM4aBadge
import com.universal.dm.ui.theme.SpeedBadgeColor
import com.universal.dm.ui.theme.UDMTheme
import java.util.Locale

private val SamplePreviewTask = DownloadTaskEntity(
    id = "sample_task",
    title = "Cosmos 4K Ultra HD Nature Documentary (AV1)",
    sourcePageUrl = "https://example.com/watch?v=123",
    downloadUrl = "https://example.com/video.mp4",
    mediaType = MediaType.VIDEO,
    qualityLabel = "2160p 4K",
    codecLabel = "AV1",
    fileName = "Cosmos_4K_Nature.mp4",
    filePath = "/storage/emulated/0/Download/UDM/Cosmos_4K_Nature.mp4",
    totalBytes = 412_000_000L,
    downloadedBytes = 188_000_000L,
    speedBytesPerSec = 14_500_000L,
    status = DownloadStatus.DOWNLOADING,
    threadCount = 8
)

private val SamplePreviewChunks = listOf(
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 0, startByte = 0, endByte = 100, currentByte = 100, isCompleted = true),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 1, startByte = 100, endByte = 200, currentByte = 180, isCompleted = false),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 2, startByte = 200, endByte = 300, currentByte = 260, isCompleted = false),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 3, startByte = 300, endByte = 400, currentByte = 340, isCompleted = false),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 4, startByte = 400, endByte = 500, currentByte = 430, isCompleted = false),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 5, startByte = 500, endByte = 600, currentByte = 520, isCompleted = false),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 6, startByte = 600, endByte = 700, currentByte = 610, isCompleted = false),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 7, startByte = 700, endByte = 800, currentByte = 700, isCompleted = false)
)

@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier
) {
    DownloadsScreen(modifier = modifier, viewModel = null)
}

@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel?
) {
    val isInspection = LocalInspectionMode.current
    if (isInspection) {
        DownloadsScreenContent(
            modifier = modifier,
            tasks = listOf(SamplePreviewTask),
            chunksMap = mapOf("sample_task" to SamplePreviewChunks),
            onPauseTask = {},
            onResumeTask = {},
            onCancelTask = {},
            onRetryTask = {},
            onPauseAll = {},
            onResumeAll = {},
            onEnqueueManualDownload = { _, _, _ -> }
        )
    } else {
        val vm: DownloadsViewModel = viewModel ?: viewModel()
        val tasks by vm.activeTasks.collectAsState()
        val chunksMap by vm.chunksMap.collectAsState()

        DownloadsScreenContent(
            modifier = modifier,
            tasks = tasks,
            chunksMap = chunksMap,
            onPauseTask = { vm.pauseTask(it) },
            onResumeTask = { vm.resumeTask(it) },
            onCancelTask = { vm.cancelTask(it) },
            onRetryTask = { vm.resumeTask(it) },
            onPauseAll = { vm.pauseAll() },
            onResumeAll = { vm.resumeAll() },
            onEnqueueManualDownload = { url, name, threads -> vm.enqueueManualDownload(url, name, threads) }
        )
    }
}

@Composable
fun DownloadsScreenContent(
    modifier: Modifier = Modifier,
    tasks: List<DownloadTaskEntity>,
    chunksMap: Map<String, List<DownloadChunkEntity>>,
    onPauseTask: (String) -> Unit,
    onResumeTask: (String) -> Unit,
    onCancelTask: (String) -> Unit,
    onRetryTask: (String) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onEnqueueManualDownload: (String, String, Int) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var showManualUrlDialog by remember { mutableStateOf(false) }

    val filteredTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            "Video (AV1/MP4)" -> tasks.filter { it.mediaType == MediaType.VIDEO }
            "Audio (M4A/MP3)" -> tasks.filter { it.mediaType == MediaType.AUDIO }
            "Documents & Files" -> tasks.filter { it.mediaType == MediaType.DIRECT_FILE }
            else -> tasks
        }
    }

    val totalSpeed = tasks.sumOf { it.speedBytesPerSec }
    val activeRunningCount = tasks.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
    val isAnyDownloading = tasks.any { it.status == DownloadStatus.DOWNLOADING }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. TOP HERO THROUGHPUT MONITOR
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                SpeedBadgeColor.copy(alpha = 0.25f),
                                                MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = SpeedBadgeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Total Active Throughput",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DownloadEngineHelper.formatSpeed(totalSpeed),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SpeedBadgeColor
                                )
                            }
                        }

                        // Badge showing running tasks
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (activeRunningCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (activeRunningCount > 0) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "$activeRunningCount Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeRunningCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))

                    // Action Controls: Pause All / Resume All + New URL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (isAnyDownloading) onPauseAll() else onResumeAll()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            enabled = tasks.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = if (isAnyDownloading) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isAnyDownloading) "Pause All" else "Resume All")
                        }

                        Button(
                            onClick = { showManualUrlDialog = true },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New URL")
                        }
                    }
                }
            }
        }

        // 2. CATEGORY FILTER CHIPS
        item {
            val categories = listOf("All", "Video (AV1/MP4)", "Audio (M4A/MP3)", "Documents & Files")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedFilter == cat,
                        onClick = { selectedFilter = cat },
                        label = { Text(cat, fontWeight = if (selectedFilter == cat) FontWeight.Bold else FontWeight.Medium) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // 3. EMPTY STATE
        if (filteredTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "No Active Downloads",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Browse websites in the Browser tab to sniff and download media, or tap '+ New URL' above to download direct files or YouTube streams.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. ACTIVE DOWNLOAD CARDS
        items(filteredTasks, key = { it.id }) { task ->
            val chunks = chunksMap[task.id] ?: emptyList()
            ActiveDownloadTaskCard(
                task = task,
                chunks = chunks,
                onPause = { onPauseTask(task.id) },
                onResume = { onResumeTask(task.id) },
                onCancel = { onCancelTask(task.id) },
                onRetry = { onRetryTask(task.id) }
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    // Manual URL Enqueue Dialog
    if (showManualUrlDialog) {
        val clipboardManager = LocalClipboardManager.current
        var manualUrl by remember { mutableStateOf("") }
        var manualName by remember { mutableStateOf("") }
        var threadSlider by remember { mutableIntStateOf(8) }

        AlertDialog(
            onDismissRequest = { showManualUrlDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Add Download URL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Paste direct file links (.zip, .apk, .mp4) or streaming URLs (YouTube, Vimeo, SoundCloud):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = manualUrl,
                        onValueChange = { manualUrl = it },
                        label = { Text("Download / Stream URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) manualUrl = clip
                            }) {
                                Icon(Icons.Default.CloudDownload, contentDescription = "Paste Clipboard")
                            }
                        }
                    )

                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("Custom File Name (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Parallel Threads Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Parallel Threads", style = MaterialTheme.typography.bodySmall)
                            Text("$threadSlider Threads", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = threadSlider.toFloat(),
                            onValueChange = { threadSlider = it.toInt() },
                            valueRange = 1f..16f,
                            steps = 14
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualUrl.isNotBlank()) {
                            onEnqueueManualDownload(manualUrl.trim(), manualName.trim(), threadSlider)
                            showManualUrlDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start Download")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showManualUrlDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun ActiveDownloadTaskCard(
    task: DownloadTaskEntity,
    chunks: List<DownloadChunkEntity>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val isPaused = task.status == DownloadStatus.PAUSED
    val isFailed = task.status == DownloadStatus.FAILED
    val isMuxing = task.status == DownloadStatus.MERGING_MUXING

    val percent = if (task.totalBytes > 0) {
        ((task.downloadedBytes * 100) / task.totalBytes).toInt().coerceIn(0, 100)
    } else 0

    val etaSeconds = if (task.speedBytesPerSec > 0 && task.totalBytes > task.downloadedBytes) {
        (task.totalBytes - task.downloadedBytes) / task.speedBytesPerSec
    } else 0L

    val etaText = if (etaSeconds > 0) {
        val m = etaSeconds / 60
        val s = etaSeconds % 60
        String.format(Locale.US, "%02d:%02d left", m, s)
    } else if (isMuxing) {
        "Muxing streams..."
    } else {
        "--"
    }

    val codecColor = when {
        task.codecLabel.contains("AV1", ignoreCase = true) -> CodecAv1Badge
        task.codecLabel.contains("M4A", ignoreCase = true) -> CodecM4aBadge
        else -> MaterialTheme.colorScheme.primary
    }

    val typeIcon = when (task.mediaType) {
        MediaType.VIDEO -> Icons.Default.Movie
        MediaType.AUDIO -> Icons.Default.Audiotrack
        MediaType.DIRECT_FILE -> Icons.Default.DownloadDone
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Media Type Icon + File Title + Action Buttons
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(codecColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = codecColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.fileName.ifEmpty { task.title },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${task.codecLabel} • ${task.qualityLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = codecColor
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = when (task.status) {
                                    DownloadStatus.MERGING_MUXING -> "Muxing Video & Audio"
                                    DownloadStatus.PAUSED -> "Paused"
                                    DownloadStatus.FAILED -> "Failed"
                                    DownloadStatus.QUEUED -> "Queued"
                                    DownloadStatus.DOWNLOADING -> "Downloading"
                                    DownloadStatus.COMPLETED -> "Finished"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMuxing) MaterialTheme.colorScheme.tertiary else if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isFailed) {
                        FilledTonalIconButton(onClick = onRetry, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(18.dp))
                        }
                    } else {
                        FilledTonalIconButton(
                            onClick = if (isPaused) onResume else onPause,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Main Progress Indicator
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isPaused) MaterialTheme.colorScheme.outline else if (isMuxing) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            // IDM-STYLE SEGMENTED PARALLEL THREAD BAR
            IdmSegmentedThreadBar(
                threadCount = task.threadCount,
                chunks = chunks,
                isPaused = isPaused
            )

            // Metrics row: Downloaded / Total, Percentage, Speed, ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sizeText = if (task.totalBytes >= 1024L * 1024L * 1024L) {
                    val downloadedGb = task.downloadedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                    val totalGb = task.totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                    String.format(Locale.US, "%.2f / %.2f GB (%d%%)", downloadedGb, totalGb, percent)
                } else if (task.totalBytes > 0) {
                    val downloadedMb = task.downloadedBytes.toDouble() / (1024.0 * 1024.0)
                    val totalMb = task.totalBytes.toDouble() / (1024.0 * 1024.0)
                    String.format(Locale.US, "%.1f / %.1f MB (%d%%)", downloadedMb, totalMb, percent)
                } else {
                    val downloadedMb = task.downloadedBytes.toDouble() / (1024.0 * 1024.0)
                    String.format(Locale.US, "%.1f MB downloaded", downloadedMb)
                }

                Text(
                    text = sizeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = if (isPaused) "Paused" else "${DownloadEngineHelper.formatSpeed(task.speedBytesPerSec)} • $etaText",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPaused) MaterialTheme.colorScheme.outline else SpeedBadgeColor
                )
            }
        }
    }
}

/**
 * IDM-Style Segmented Parallel Thread Bar:
 * Displays N mini progress segments filling up simultaneously in real-time.
 */
@Composable
fun IdmSegmentedThreadBar(
    threadCount: Int,
    chunks: List<DownloadChunkEntity>,
    isPaused: Boolean
) {
    val count = threadCount.coerceIn(1, 16)

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$count Parallel Thread Segments",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (i in 0 until count) {
                val chunk = chunks.find { it.threadIndex == i }
                val chunkProgress = if (chunk != null) {
                    val length = (chunk.endByte - chunk.startByte).coerceAtLeast(1L)
                    val done = (chunk.currentByte - chunk.startByte).coerceAtLeast(0L)
                    (done.toFloat() / length).coerceIn(0f, 1f)
                } else {
                    // When chunks are managed by stream engine or not yet split, distribute overall progress
                    0f
                }

                LinearProgressIndicator(
                    progress = { chunkProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isPaused) MaterialTheme.colorScheme.outline
                    else if (chunk?.isCompleted == true) SpeedBadgeColor
                    else MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActiveDownloadCardPreview() {
    UDMTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ActiveDownloadTaskCard(
                task = SamplePreviewTask,
                chunks = SamplePreviewChunks,
                onPause = {},
                onResume = {},
                onCancel = {},
                onRetry = {}
            )
        }
    }
}
