package com.universal.dm.ui.downloads

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.net.URI
import java.util.Locale

// Sample preview models
private val SamplePreviewTask = DownloadTaskEntity(
    id = "sample_task",
    title = "Interstellar 4K IMAX Ultra HD Experience (AV1/Opus)",
    sourcePageUrl = "https://example.com/watch?v=interstellar",
    downloadUrl = "https://example.com/video.mp4",
    mediaType = MediaType.VIDEO,
    qualityLabel = "2160p 4K",
    codecLabel = "AV1",
    fileName = "Interstellar_4K_IMAX.mp4",
    filePath = "/storage/emulated/0/Download/UDM/Interstellar_4K_IMAX.mp4",
    totalBytes = 850_000_000L,
    downloadedBytes = 544_000_000L,
    speedBytesPerSec = 16_800_000L,
    status = DownloadStatus.DOWNLOADING,
    threadCount = 8
)

private val SamplePreviewChunks = listOf(
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 0, startByte = 0, endByte = 100, currentByte = 100, isCompleted = true),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 1, startByte = 100, endByte = 200, currentByte = 195, isCompleted = false),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 2, startByte = 200, endByte = 300, currentByte = 270, isCompleted = false),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 3, startByte = 300, endByte = 400, currentByte = 350, isCompleted = false),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 4, startByte = 400, endByte = 500, currentByte = 440, isCompleted = false),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 5, startByte = 500, endByte = 600, currentByte = 560, isCompleted = false),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 6, startByte = 600, endByte = 700, currentByte = 620, isCompleted = false),
    DownloadChunkEntity(taskId = "sample_task", threadIndex = 7, startByte = 700, endByte = 800, currentByte = 710, isCompleted = false)
)

enum class DownloadSortOrder(val label: String) {
    RECENT("Most Recent"),
    SPEED("Fastest Speed"),
    SIZE("Largest Size"),
    PROGRESS("Progress %")
}

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
            peakSpeed = 24_500_000L,
            onPauseTask = {},
            onResumeTask = {},
            onCancelTask = {},
            onRetryTask = {},
            onPauseAll = {},
            onResumeAll = {},
            onCancelAll = {},
            onClearFailed = {},
            onEnqueueManualDownload = { _, _, _ -> }
        )
    } else {
        val vm: DownloadsViewModel = viewModel ?: viewModel()
        val tasks by vm.activeTasks.collectAsState()
        val chunksMap by vm.chunksMap.collectAsState()
        val peakSpeed by vm.peakSpeed.collectAsState()

        DownloadsScreenContent(
            modifier = modifier,
            tasks = tasks,
            chunksMap = chunksMap,
            peakSpeed = peakSpeed,
            onPauseTask = { vm.pauseTask(it) },
            onResumeTask = { vm.resumeTask(it) },
            onCancelTask = { vm.cancelTask(it) },
            onRetryTask = { vm.retryTask(it) },
            onPauseAll = { vm.pauseAll() },
            onResumeAll = { vm.resumeAll() },
            onCancelAll = { vm.cancelAll() },
            onClearFailed = { vm.clearFailedTasks() },
            onEnqueueManualDownload = { url, name, threads -> vm.enqueueManualDownload(url, name, threads) }
        )
    }
}

@Composable
fun DownloadsScreenContent(
    modifier: Modifier = Modifier,
    tasks: List<DownloadTaskEntity>,
    chunksMap: Map<String, List<DownloadChunkEntity>>,
    peakSpeed: Long,
    onPauseTask: (String) -> Unit,
    onResumeTask: (String) -> Unit,
    onCancelTask: (String) -> Unit,
    onRetryTask: (String) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onCancelAll: () -> Unit,
    onClearFailed: () -> Unit,
    onEnqueueManualDownload: (String, String, Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf(DownloadSortOrder.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showManualUrlDialog by remember { mutableStateOf(false) }
    var selectedSpeedMode by remember { mutableStateOf("Turbo 16x") }

    val categories = remember(tasks) {
        listOf(
            "All" to tasks.size,
            "Active" to tasks.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED },
            "Video" to tasks.count { it.mediaType == MediaType.VIDEO },
            "Audio" to tasks.count { it.mediaType == MediaType.AUDIO },
            "Files" to tasks.count { it.mediaType == MediaType.DIRECT_FILE },
            "Paused" to tasks.count { it.status == DownloadStatus.PAUSED },
            "Failed" to tasks.count { it.status == DownloadStatus.FAILED }
        )
    }

    // Filter & Sort Logic
    val filteredTasks = remember(tasks, searchQuery, selectedCategory, sortOrder) {
        var list = tasks

        // Category filter
        list = when (selectedCategory) {
            "Active" -> list.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
            "Video" -> list.filter { it.mediaType == MediaType.VIDEO }
            "Audio" -> list.filter { it.mediaType == MediaType.AUDIO }
            "Files" -> list.filter { it.mediaType == MediaType.DIRECT_FILE }
            "Paused" -> list.filter { it.status == DownloadStatus.PAUSED }
            "Failed" -> list.filter { it.status == DownloadStatus.FAILED }
            else -> list
        }

        // Search query filter
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            list = list.filter {
                it.title.lowercase(Locale.ROOT).contains(q) ||
                        it.fileName.lowercase(Locale.ROOT).contains(q) ||
                        it.sourcePageUrl.lowercase(Locale.ROOT).contains(q)
            }
        }

        // Sort order
        when (sortOrder) {
            DownloadSortOrder.RECENT -> list.sortedByDescending { it.createdAt }
            DownloadSortOrder.SPEED -> list.sortedByDescending { it.speedBytesPerSec }
            DownloadSortOrder.SIZE -> list.sortedByDescending { it.totalBytes }
            DownloadSortOrder.PROGRESS -> list.sortedByDescending {
                if (it.totalBytes > 0) (it.downloadedBytes * 100) / it.totalBytes else 0
            }
        }
    }

    val totalCurrentSpeed = tasks.sumOf { it.speedBytesPerSec }
    val activeRunningCount = tasks.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
    val isAnyDownloading = tasks.any { it.status == DownloadStatus.DOWNLOADING }
    val totalActiveThreads = tasks.filter { it.status == DownloadStatus.DOWNLOADING }.sumOf { it.threadCount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
    ) {
        // 1. TOP HEADER WITH CONTROLS & SEARCH TOGGLE
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = if (activeRunningCount > 0)
                            "$activeRunningCount active streams • ${DownloadEngineHelper.formatSpeed(totalCurrentSpeed)}"
                        else
                            "Multi-thread parallel engine • Idle",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (activeRunningCount > 0) Color(0xFF00E5FF) else Color(0xFF88A0B2)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search toggle button
                    Surface(
                        onClick = { isSearchExpanded = !isSearchExpanded },
                        shape = CircleShape,
                        color = if (isSearchExpanded) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF0C1924),
                        border = BorderStroke(1.dp, if (isSearchExpanded) Color(0xFF00E5FF) else Color(0xFF1B3547)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchExpanded) Color(0xFF00E5FF) else Color(0xFF88A0B2),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // More Menu
                    Box {
                        Surface(
                            onClick = { showMoreMenu = true },
                            shape = CircleShape,
                            color = Color(0xFF0C1924),
                            border = BorderStroke(1.dp, Color(0xFF1B3547)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Options",
                                    tint = Color(0xFF88A0B2),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Failed / Errored Tasks") },
                                onClick = {
                                    onClearFailed()
                                    showMoreMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cancel All Active Tasks") },
                                onClick = {
                                    onCancelAll()
                                    showMoreMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ClearAll, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. EXPANDABLE SEARCH BAR
        if (isSearchExpanded) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0A1722),
                    border = BorderStroke(1.dp, Color(0xFF183244))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(Color(0xFF00E5FF)),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Filter by title, URL or extension...",
                                        style = TextStyle(color = Color(0xFF5D7486), fontSize = 14.sp)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF88A0B2),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }
            }
        }

        // 3. QUANTUM THROUGHPUT TELEMETRY HERO CARD
        item {
            QuantumThroughputHeroCard(
                totalSpeed = totalCurrentSpeed,
                peakSpeed = peakSpeed,
                activeCount = activeRunningCount,
                activeThreads = totalActiveThreads,
                isDownloading = isAnyDownloading,
                selectedSpeedMode = selectedSpeedMode,
                onSpeedModeChange = { selectedSpeedMode = it },
                onTogglePauseAll = {
                    if (isAnyDownloading) onPauseAll() else onResumeAll()
                },
                onAddNewDownload = { showManualUrlDialog = true }
            )
        }

        // 4. CATEGORY FILTER CHIPS & SORT SELECTOR (TWO-TIER SEPARATION - NO OVERLAPPING)
        item {
            val haptic = LocalHapticFeedback.current

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tier 1: Full-Width Scrollable Category Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(categories) { (cat, count) ->
                        val isSelected = selectedCategory == cat
                        val chipSource = remember { MutableInteractionSource() }
                        val isPressed by chipSource.collectIsPressedAsState()
                        val chipScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.95f else 1f,
                            animationSpec = spring(stiffness = 500f),
                            label = "chipScale"
                        )

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCategory = cat
                            },
                            modifier = Modifier.graphicsLayer {
                                scaleX = chipScale
                                scaleY = chipScale
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF08141F),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF00E5FF) else Color(0xFF132838)
                            ),
                            interactionSource = chipSource
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF8BA2B4)
                                )

                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Color(0xFF00E5FF) else Color(0xFF162D3D)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF002733) else Color(0xFF8BA2B4)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tier 2: Category Context Tag on Left & Sort Dropdown on Right (Zero Collision)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF))
                        )
                        Text(
                            text = when (selectedCategory) {
                                "Active" -> "Showing Active Downloads"
                                "Completed" -> "Showing Completed Tasks"
                                "Paused" -> "Showing Paused Tasks"
                                else -> "All Active Queue Tasks"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7591A3),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Sort Dropdown Button
                    Box {
                        val sortSource = remember { MutableInteractionSource() }
                        val isSortPressed by sortSource.collectIsPressedAsState()
                        val sortScale by animateFloatAsState(
                            targetValue = if (isSortPressed) 0.95f else 1f,
                            animationSpec = spring(stiffness = 500f),
                            label = "sortScale"
                        )

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showSortMenu = true
                            },
                            modifier = Modifier.graphicsLayer {
                                scaleX = sortScale
                                scaleY = sortScale
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF08141F),
                            border = BorderStroke(1.dp, Color(0xFF132838)),
                            interactionSource = sortSource
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Sort",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = sortOrder.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFC7D7E2)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF8BA2B4),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(Color(0xFF0B1722))
                        ) {
                            DownloadSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = order.label,
                                                color = if (sortOrder == order) Color(0xFF00E5FF) else Color.White,
                                                fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (sortOrder == order) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF00E5FF),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        sortOrder = order
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. EMPTY QUEUE STATE
        if (filteredTasks.isEmpty()) {
            item {
                EmptyQueueStateCard(
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    onOpenNewDownload = { showManualUrlDialog = true }
                )
            }
        }

        // 6. ACTIVE TASK CARDS
        items(filteredTasks, key = { it.id }) { task ->
            val chunks = chunksMap[task.id] ?: emptyList()
            LuxuryDownloadTaskCard(
                task = task,
                chunks = chunks,
                onPause = { onPauseTask(task.id) },
                onResume = { onResumeTask(task.id) },
                onCancel = { onCancelTask(task.id) },
                onRetry = { onRetryTask(task.id) }
            )
        }
    }

    // Modern Add Download URL Modal
    if (showManualUrlDialog) {
        ModernAddDownloadDialog(
            onDismiss = { showManualUrlDialog = false },
            onEnqueue = { url, name, threads ->
                onEnqueueManualDownload(url, name, threads)
                showManualUrlDialog = false
            }
        )
    }
}

/**
 * High-End "Quantum Throughput" Hero Dashboard
 * Displays real-time speed, live audio-style animated bandwidth waveform, peak speed, and turbo controls.
 */
@Composable
fun QuantumThroughputHeroCard(
    totalSpeed: Long,
    peakSpeed: Long,
    activeCount: Int,
    activeThreads: Int,
    isDownloading: Boolean,
    selectedSpeedMode: String,
    onSpeedModeChange: (String) -> Unit,
    onTogglePauseAll: () -> Unit,
    onAddNewDownload: () -> Unit
) {
    // Infinite animation for glowing speed pulse
    val infiniteTransition = rememberInfiniteTransition(label = "PulseGlow")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF08141F),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    Color(0x4000E5FF),
                    Color(0x2000B0FF),
                    Color(0x1000E5FF)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Row 1: Speedometer & Telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Glowing Icon Pill
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF00E5FF).copy(alpha = if (isDownloading) pulseGlowAlpha * 0.45f else 0.15f),
                                        Color(0xFF032233)
                                    )
                                )
                            )
                            .border(
                                1.5.dp,
                                if (isDownloading) Color(0xFF00E5FF).copy(alpha = pulseGlowAlpha) else Color(0xFF133245),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (isDownloading) Color(0xFF00E5FF) else Color(0xFF7593A5),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "LIVE THROUGHPUT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = Color(0xFF7D9AB0)
                            )
                            if (isDownloading) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF))
                                )
                            }
                        }

                        Text(
                            text = DownloadEngineHelper.formatSpeed(totalSpeed),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = if (isDownloading) Color(0xFF00E5FF) else Color.White
                        )
                    }
                }

                // Stats Badges (Peak & Active Threads)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F2636),
                        border = BorderStroke(1.dp, Color(0xFF1C425A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Peak: ${DownloadEngineHelper.formatSpeed(peakSpeed)}",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F)
                                )
                            )
                        }
                    }

                    Text(
                        text = "$activeThreads Sockets • $activeCount Active",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF7F9DB0)
                        )
                    )
                }
            }

            // Row 2: Real-time Audio/Waveform Style Bandwidth Meter
            BandwidthActivityWaveform(
                isDownloading = isDownloading
            )

            // Speed Profile Presets (Turbo 16x / Balanced 8x / Eco 4x)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Turbo 16x", "Balanced 8x", "Eco 4x").forEach { mode ->
                    val isModeSelected = selectedSpeedMode == mode
                    Surface(
                        onClick = { onSpeedModeChange(mode) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isModeSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF0D1F2D),
                        border = BorderStroke(1.dp, if (isModeSelected) Color(0xFF00E5FF) else Color(0xFF17354A)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isModeSelected) Color(0xFF00E5FF) else Color(0xFF7F9CB0)
                                )
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF132A3B))

            // Row 3: Action Buttons (Pause All / Resume All + Add Download)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Resume All
                val pauseButtonSource = remember { MutableInteractionSource() }
                val isPausePressed by pauseButtonSource.collectIsPressedAsState()
                val pauseScale by animateFloatAsState(
                    targetValue = if (isPausePressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                    label = "pauseScale"
                )

                Surface(
                    onClick = onTogglePauseAll,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = pauseScale
                            scaleY = pauseScale
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDownloading) Color(0xFF1B2F3D) else Color(0xFF102838),
                    border = BorderStroke(1.dp, Color(0xFF274A61)),
                    interactionSource = pauseButtonSource
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDownloading) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isDownloading) Color(0xFFFFCC80) else Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDownloading) "Pause All" else "Resume All",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                // Add Download Button
                val addSource = remember { MutableInteractionSource() }
                val isAddPressed by addSource.collectIsPressedAsState()
                val addScale by animateFloatAsState(
                    targetValue = if (isAddPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                    label = "addScale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = addScale
                            scaleY = addScale
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF0084FF))
                            )
                        )
                        .clickable(interactionSource = addSource, indication = null) {
                            onAddNewDownload()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF002230),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Download",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF002230)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated High-Tech Live Bandwidth Waveform
 * Mimics high-end hardware throughput telemetry.
 */
@Composable
fun BandwidthActivityWaveform(
    isDownloading: Boolean
) {
    val barCount = 24
    val transition = rememberInfiniteTransition(label = "WaveformAnim")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            val animatedHeight by transition.animateFloat(
                initialValue = 0.15f,
                targetValue = if (isDownloading) ((i % 5 + 3) * 0.18f).coerceIn(0.2f, 1f) else 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600 + (i * 35) % 400,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "WaveBar$i"
            )

            val currentFraction = if (isDownloading) animatedHeight else 0.15f
            val barColor = when {
                !isDownloading -> Color(0xFF132A39)
                i % 4 == 0 -> Color(0xFF00E5FF)
                i % 4 == 1 -> Color(0xFF00B0FF)
                i % 4 == 2 -> Color(0xFF38EF7D)
                else -> Color(0xFF11998E)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 1.5.dp)
                    .height(28.dp)
                    .graphicsLayer {
                        scaleY = currentFraction.coerceIn(0.14f, 1f)
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * Luxury Download Task Card (Apple Elegance + IDM Turbo Parallel Sockets)
 */
@Composable
fun LuxuryDownloadTaskCard(
    task: DownloadTaskEntity,
    chunks: List<DownloadChunkEntity>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val isPaused = task.status == DownloadStatus.PAUSED
    val isFailed = task.status == DownloadStatus.FAILED
    val isMuxing = task.status == DownloadStatus.MERGING_MUXING
    val isDownloading = task.status == DownloadStatus.DOWNLOADING

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

    val typeIcon = when (task.mediaType) {
        MediaType.VIDEO -> Icons.Default.Movie
        MediaType.AUDIO -> Icons.Default.Audiotrack
        MediaType.DIRECT_FILE -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    val themeColor = when (task.mediaType) {
        MediaType.VIDEO -> Color(0xFF00E5FF)
        MediaType.AUDIO -> Color(0xFFD05CE3)
        MediaType.DIRECT_FILE -> Color(0xFF00E676)
    }

    // Extract domain host for display
    val domainHost = remember(task.sourcePageUrl) {
        try {
            val uri = URI(task.sourcePageUrl)
            uri.host?.removePrefix("www.") ?: "Direct Link"
        } catch (_: Exception) {
            "Direct Stream"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF091622),
        border = BorderStroke(
            1.dp,
            if (isDownloading) Color(0xFF1B3C52) else Color(0xFF122432)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Media Icon, Title & Domain, Action Buttons
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
                    // Glowing Media Badge
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(themeColor.copy(alpha = 0.15f))
                            .border(1.dp, themeColor.copy(alpha = 0.4f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = task.fileName.ifEmpty { task.title },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Domain capsule
                            Text(
                                text = domainHost,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF7F9CB0)
                                )
                            )

                            Text(text = "•", color = Color(0xFF435A6B), fontSize = 11.sp)

                            // Quality & Codec tag
                            Text(
                                text = "${task.codecLabel} • ${task.qualityLabel}",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor
                                )
                            )
                        }
                    }
                }

                // Action Controls: Play/Pause/Retry + Cancel
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFailed) {
                        Surface(
                            onClick = onRetry,
                            shape = CircleShape,
                            color = Color(0xFFFF5252).copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color(0xFFFF5252)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            onClick = if (isPaused) onResume else onPause,
                            shape = CircleShape,
                            color = if (isPaused) Color(0xFF00E5FF).copy(alpha = 0.18f) else Color(0xFF1B3242),
                            border = BorderStroke(1.dp, if (isPaused) Color(0xFF00E5FF) else Color(0xFF284B61)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPaused) "Resume" else "Pause",
                                    tint = if (isPaused) Color(0xFF00E5FF) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onCancel,
                        shape = CircleShape,
                        color = Color(0xFF1B242D),
                        border = BorderStroke(1.dp, Color(0xFF2E3D4A)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = Color(0xFFFF7043),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Main Fluid Progress Indicator
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sizeFormatted = formatProgressSizes(task.downloadedBytes, task.totalBytes)
                    Text(
                        text = sizeFormatted,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8BA6B9)
                        )
                    )

                    Text(
                        text = if (isPaused) "Paused" else if (isFailed) "Failed" else if (isMuxing) "Muxing..." else "$percent%",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isFailed) Color(0xFFFF5252) else if (isPaused) Color(0xFFFFB74D) else Color(0xFF00E5FF)
                        )
                    )
                }

                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isPaused) Color(0xFFFFB74D)
                    else if (isFailed) Color(0xFFFF5252)
                    else if (isMuxing) Color(0xFFBA68C8)
                    else Color(0xFF00E5FF),
                    trackColor = Color(0xFF112330)
                )
            }

            // IDM-Style Segmented Parallel Thread Sockets
            IdmSegmentedThreadBarModern(
                threadCount = task.threadCount,
                chunks = chunks,
                isPaused = isPaused,
                isDownloading = isDownloading
            )

            // Speed & ETA Telemetry Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDownloading) Color(0xFF00E5FF)
                                else if (isPaused) Color(0xFFFFB74D)
                                else if (isFailed) Color(0xFFFF5252)
                                else Color(0xFF7593A5)
                            )
                    )
                    Text(
                        text = when (task.status) {
                            DownloadStatus.DOWNLOADING -> "${DownloadEngineHelper.formatSpeed(task.speedBytesPerSec)} • $etaText"
                            DownloadStatus.PAUSED -> "Paused by user"
                            DownloadStatus.QUEUED -> "Queued in socket pool"
                            DownloadStatus.MERGING_MUXING -> "Muxing video & audio streams"
                            DownloadStatus.FAILED -> task.errorMessage?.take(30) ?: "Download failed"
                            DownloadStatus.COMPLETED -> "Finished"
                        },
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDownloading) Color(0xFF00E5FF) else Color(0xFF90A4AE)
                        )
                    )
                }

                // Expand / Collapse Chevron
                Row(
                    modifier = Modifier.clickable { isExpanded = !isExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Less" else "Details",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF718C9E)
                        )
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF718C9E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expandable Diagnostic Drawer
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF061019))
                        .border(1.dp, Color(0xFF112330), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Destination Path
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DESTINATION STORAGE",
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D788C))
                            )
                            Text(
                                text = task.filePath,
                                style = TextStyle(fontSize = 12.sp, color = Color(0xFFCFDCE5)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(task.filePath))
                                Toast.makeText(context, "Storage path copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Path", tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                        }
                    }

                    // Source Page URL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SOURCE STREAM URL",
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D788C))
                            )
                            Text(
                                text = task.sourcePageUrl,
                                style = TextStyle(fontSize = 12.sp, color = Color(0xFFCFDCE5)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(task.sourcePageUrl))
                                Toast.makeText(context, "Source URL copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL", tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                        }
                    }

                    // Chunk Breakdown
                    if (chunks.isNotEmpty()) {
                        HorizontalDivider(color = Color(0xFF112330))
                        Text(
                            text = "SOCKET CHUNK TABLE (${chunks.size} Active Chunks)",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D788C))
                        )

                        chunks.take(4).forEach { chunk ->
                            val cLength = (chunk.endByte - chunk.startByte).coerceAtLeast(1L)
                            val cDone = (chunk.currentByte - chunk.startByte).coerceAtLeast(0L)
                            val cPct = ((cDone * 100) / cLength).toInt().coerceIn(0, 100)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Thread #${chunk.threadIndex}",
                                    style = TextStyle(fontSize = 11.sp, color = Color(0xFF8BA6B9), fontFamily = FontFamily.Monospace)
                                )
                                Text(
                                    text = if (chunk.isCompleted) "Completed" else "$cPct%",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (chunk.isCompleted) Color(0xFF00E676) else Color(0xFF00E5FF),
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * IDM-Style Segmented Parallel Thread Sockets Bar
 */
@Composable
fun IdmSegmentedThreadBarModern(
    threadCount: Int,
    chunks: List<DownloadChunkEntity>,
    isPaused: Boolean,
    isDownloading: Boolean
) {
    val count = threadCount.coerceIn(1, 16)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count Parallel Thread Sockets",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF718C9E))
            )

            Text(
                text = if (isDownloading) "Turbo Multi-Part Active" else if (isPaused) "Socket Pool Paused" else "Ready",
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDownloading) Color(0xFF00E5FF) else Color(0xFF718C9E))
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
                    0f
                }

                LinearProgressIndicator(
                    progress = { chunkProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isPaused) Color(0xFF455A64)
                    else if (chunk?.isCompleted == true) Color(0xFF00E676)
                    else Color(0xFF00E5FF),
                    trackColor = Color(0xFF10222F)
                )
            }
        }
    }
}

/**
 * Modern High-Tech Empty State Card
 */
@Composable
fun EmptyQueueStateCard(
    searchQuery: String,
    selectedCategory: String,
    onOpenNewDownload: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF08141F),
        border = BorderStroke(1.dp, Color(0xFF132838))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF00E5FF).copy(alpha = 0.2f), Color(0xFF0B2131))
                        )
                    )
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(34.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No Matching Downloads" else "Download Queue Empty",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )

                Text(
                    text = if (searchQuery.isNotEmpty())
                        "No tasks found matching \"$searchQuery\" in category \"$selectedCategory\"."
                    else
                        "Browse any web page in the Browser tab to sniff video and audio streams, or tap below to paste a direct download link.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF88A0B2),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    lineHeight = 20.sp
                )
            }

            Surface(
                onClick = onOpenNewDownload,
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFF00E5FF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Add Download URL",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Modern High-Tech Modal Dialog to Add Direct/Streaming Downloads
 */
@Composable
fun ModernAddDownloadDialog(
    onDismiss: () -> Unit,
    onEnqueue: (url: String, fileName: String, threadCount: Int) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var urlText by remember { mutableStateOf("") }
    var fileNameText by remember { mutableStateOf("") }
    var threadSlider by remember { mutableIntStateOf(8) }

    // Check if clipboard contains a link
    val clipboardText = remember {
        val clip = clipboardManager.getText()?.text
        if (!clip.isNullOrBlank() && (clip.startsWith("http://") || clip.startsWith("https://"))) clip else null
    }

    val isStreaming = remember(urlText) {
        val u = urlText.lowercase(Locale.ROOT)
        u.contains("youtube.com") || u.contains("youtu.be") || u.contains("vimeo.com") ||
                u.contains("soundcloud.com") || u.contains("dailymotion.com") || u.contains("tiktok.com")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "New Download Task",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Clipboard quick chip
                if (clipboardText != null && urlText.isEmpty()) {
                    Surface(
                        onClick = { urlText = clipboardText },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                            Text(
                                text = "Paste: ${clipboardText.take(35)}...",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // URL Input Field
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Stream or Direct File URL") },
                    placeholder = { Text("https://example.com/file.zip or YouTube URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF1D374A),
                        focusedLabelColor = Color(0xFF00E5FF),
                        unfocusedLabelColor = Color(0xFF8BA6B9),
                        cursorColor = Color(0xFF00E5FF)
                    ),
                    trailingIcon = {
                        if (urlText.isNotEmpty()) {
                            IconButton(onClick = { urlText = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF8BA6B9))
                            }
                        }
                    }
                )

                // Engine mode badge
                if (urlText.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isStreaming) Color(0xFF00E5FF) else Color(0xFF00E676))
                        )
                        Text(
                            text = if (isStreaming) "Detected: Video/Audio Media Stream Extractor" else "Detected: Direct HTTP/HTTPS Multi-Thread File",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isStreaming) Color(0xFF00E5FF) else Color(0xFF00E676)
                            )
                        )
                    }
                }

                // Custom file name input
                OutlinedTextField(
                    value = fileNameText,
                    onValueChange = { fileNameText = it },
                    label = { Text("File Name (Optional)") },
                    placeholder = { Text(if (isStreaming) "my_video.mp4" else "archive.zip") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF1D374A),
                        focusedLabelColor = Color(0xFF00E5FF),
                        unfocusedLabelColor = Color(0xFF8BA6B9),
                        cursorColor = Color(0xFF00E5FF)
                    )
                )

                // Parallel Thread Count Stepper / Slider
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Parallel Concurrency",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF8BA6B9)
                        )
                        Text(
                            text = "$threadSlider Sockets ${if (threadSlider >= 16) "(Turbo)" else ""}",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                        )
                    }

                    Slider(
                        value = threadSlider.toFloat(),
                        onValueChange = { threadSlider = it.toInt() },
                        valueRange = 1f..16f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0xFF132A39)
                        )
                    )

                    // Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(4 to "4x Eco", 8 to "8x Standard", 16 to "16x Turbo").forEach { (th, label) ->
                            Surface(
                                onClick = { threadSlider = th },
                                shape = RoundedCornerShape(8.dp),
                                color = if (threadSlider == th) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF0D1E2B),
                                border = BorderStroke(1.dp, if (threadSlider == th) Color(0xFF00E5FF) else Color(0xFF1A384D)),
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
                                            fontWeight = if (threadSlider == th) FontWeight.Bold else FontWeight.Medium,
                                            color = if (threadSlider == th) Color(0xFF00E5FF) else Color(0xFF8BA6B9)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (urlText.isNotBlank()) {
                        onEnqueue(urlText.trim(), fileNameText.trim(), threadSlider)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color(0xFF002230)
                ),
                enabled = urlText.isNotBlank()
            ) {
                Text("Start Download", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1E3C50))
            ) {
                Text("Cancel", color = Color(0xFF8BA6B9))
            }
        },
        shape = RoundedCornerShape(26.dp),
        containerColor = Color(0xFF07131D)
    )
}

private fun formatProgressSizes(downloadedBytes: Long, totalBytes: Long): String {
    return if (totalBytes >= 1024L * 1024L * 1024L) {
        val downloadedGb = downloadedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val totalGb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        String.format(Locale.US, "%.2f / %.2f GB", downloadedGb, totalGb)
    } else if (totalBytes > 0) {
        val downloadedMb = downloadedBytes.toDouble() / (1024.0 * 1024.0)
        val totalMb = totalBytes.toDouble() / (1024.0 * 1024.0)
        String.format(Locale.US, "%.1f / %.1f MB", downloadedMb, totalMb)
    } else {
        val downloadedMb = downloadedBytes.toDouble() / (1024.0 * 1024.0)
        String.format(Locale.US, "%.1f MB downloaded", downloadedMb)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060D13)
@Composable
fun LuxuryDownloadsScreenPreview() {
    UDMTheme {
        DownloadsScreenContent(
            tasks = listOf(
                SamplePreviewTask,
                SamplePreviewTask.copy(
                    id = "sample_2",
                    title = "Cyberpunk 2077 Night City High Bitrate FLAC Audio",
                    mediaType = MediaType.AUDIO,
                    qualityLabel = "320kbps",
                    codecLabel = "FLAC",
                    fileName = "NightCity_Soundtrack.flac",
                    downloadedBytes = 120_000_000L,
                    totalBytes = 240_000_000L,
                    speedBytesPerSec = 7_200_000L,
                    status = DownloadStatus.DOWNLOADING
                )
            ),
            chunksMap = mapOf("sample_task" to SamplePreviewChunks),
            peakSpeed = 24_500_000L,
            onPauseTask = {},
            onResumeTask = {},
            onCancelTask = {},
            onRetryTask = {},
            onPauseAll = {},
            onResumeAll = {},
            onCancelAll = {},
            onClearFailed = {},
            onEnqueueManualDownload = { _, _, _ -> }
        )
    }
}
