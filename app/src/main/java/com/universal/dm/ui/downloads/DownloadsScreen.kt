package com.universal.dm.ui.downloads

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universal.dm.data.local.DownloadChunkEntity
import com.universal.dm.data.local.DownloadTaskEntity
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.data.model.MediaType
import com.universal.dm.domain.downloader.DownloadEngineHelper
import com.universal.dm.domain.sniffer.MediaSizeCalculator
import com.universal.dm.ui.components.UdmHeaderActionButton
import com.universal.dm.ui.components.UdmIconSquircle
import com.universal.dm.ui.components.UdmScreenHeader
import com.universal.dm.ui.components.UdmTonalCard
import com.universal.dm.ui.finished.Media3PlayerDialog
import com.universal.dm.ui.theme.UDMTheme
import com.universal.dm.ui.theme.UdmTextStyles
import com.universal.dm.ui.theme.UdmTheme
import java.io.File
import java.util.Locale

private val SamplePreviewTask = DownloadTaskEntity(
    id = "sample_task",
    title = "Interstellar 4K IMAX Experience",
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

enum class DownloadSortOrder(val label: String) {
    RECENT("Recent"),
    SPEED("Speed"),
    SIZE("Size"),
    PROGRESS("Progress")
}

@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel? = null,
    onNavigateToBrowser: () -> Unit = {}
) {
    val isInspection = LocalInspectionMode.current
    if (isInspection) {
        DownloadsScreenContent(
            modifier = modifier,
            tasks = listOf(SamplePreviewTask),
            chunksMap = emptyMap(),
            peakSpeed = 24_500_000L,
            previewTask = null,
            onPauseTask = {},
            onResumeTask = {},
            onCancelTask = {},
            onRetryTask = {},
            onDeleteTask = { _, _ -> },
            onPlayPreview = {},
            onDismissPreview = {},
            onPauseAll = {},
            onResumeAll = {},
            onCancelAll = {},
            onClearFailed = {},
            onClearCompleted = {},
            onEnqueueManualDownload = { _, _, _ -> },
            onNavigateToBrowser = onNavigateToBrowser
        )
    } else {
        val vm: DownloadsViewModel = viewModel ?: viewModel()
        val tasks by vm.allTasks.collectAsState()
        val chunksMap by vm.chunksMap.collectAsState()
        val peakSpeed by vm.peakSpeed.collectAsState()
        val previewTask by vm.previewTask.collectAsState()

        DownloadsScreenContent(
            modifier = modifier,
            tasks = tasks,
            chunksMap = chunksMap,
            peakSpeed = peakSpeed,
            previewTask = previewTask,
            onPauseTask = { vm.pauseTask(it) },
            onResumeTask = { vm.resumeTask(it) },
            onCancelTask = { vm.cancelTask(it) },
            onRetryTask = { vm.retryTask(it) },
            onDeleteTask = { task, deleteFiles -> vm.deleteTask(task, deleteFiles) },
            onPlayPreview = { vm.selectTaskForPreview(it) },
            onDismissPreview = { vm.selectTaskForPreview(null) },
            onPauseAll = { vm.pauseAll() },
            onResumeAll = { vm.resumeAll() },
            onCancelAll = { vm.cancelAll() },
            onClearFailed = { vm.clearFailedTasks() },
            onClearCompleted = { vm.clearCompletedTasks() },
            onEnqueueManualDownload = { url, name, threads -> vm.enqueueManualDownload(url, name, threads) },
            onNavigateToBrowser = onNavigateToBrowser
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreenContent(
    modifier: Modifier = Modifier,
    tasks: List<DownloadTaskEntity>,
    @Suppress("UNUSED_PARAMETER") chunksMap: Map<String, List<DownloadChunkEntity>>,
    @Suppress("UNUSED_PARAMETER") peakSpeed: Long,
    previewTask: DownloadTaskEntity? = null,
    onPauseTask: (String) -> Unit,
    onResumeTask: (String) -> Unit,
    onCancelTask: (String) -> Unit,
    onRetryTask: (String) -> Unit,
    onDeleteTask: (DownloadTaskEntity, Boolean) -> Unit = { _, _ -> },
    onPlayPreview: (DownloadTaskEntity) -> Unit = {},
    onDismissPreview: () -> Unit = {},
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onCancelAll: () -> Unit,
    onClearFailed: () -> Unit,
    onClearCompleted: () -> Unit = {},
    onEnqueueManualDownload: (String, String, Int) -> Unit,
    onNavigateToBrowser: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf(DownloadSortOrder.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showManualUrlDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<DownloadTaskEntity?>(null) }
    var deleteFileFromDisk by remember { mutableStateOf(true) }
    var taskForDetails by remember { mutableStateOf<DownloadTaskEntity?>(null) }

    val categories = remember(tasks) {
        listOf(
            "All" to tasks.size,
            "Active" to tasks.count {
                it.status == DownloadStatus.DOWNLOADING ||
                    it.status == DownloadStatus.QUEUED ||
                    it.status == DownloadStatus.PAUSED ||
                    it.status == DownloadStatus.MERGING_MUXING
            },
            "Done" to tasks.count { it.status == DownloadStatus.COMPLETED },
            "Video" to tasks.count { it.mediaType == MediaType.VIDEO },
            "Audio" to tasks.count { it.mediaType == MediaType.AUDIO },
            "Files" to tasks.count { it.mediaType == MediaType.DIRECT_FILE }
        )
    }

    val filteredTasks = remember(tasks, searchQuery, selectedCategory, sortOrder) {
        var list = when (selectedCategory) {
            "Active" -> tasks.filter {
                it.status == DownloadStatus.DOWNLOADING ||
                    it.status == DownloadStatus.QUEUED ||
                    it.status == DownloadStatus.PAUSED ||
                    it.status == DownloadStatus.MERGING_MUXING
            }
            "Done" -> tasks.filter { it.status == DownloadStatus.COMPLETED }
            "Video" -> tasks.filter { it.mediaType == MediaType.VIDEO }
            "Audio" -> tasks.filter { it.mediaType == MediaType.AUDIO }
            "Files" -> tasks.filter { it.mediaType == MediaType.DIRECT_FILE }
            else -> tasks
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            list = list.filter {
                it.title.lowercase(Locale.ROOT).contains(q) ||
                    it.fileName.lowercase(Locale.ROOT).contains(q) ||
                    it.sourcePageUrl.lowercase(Locale.ROOT).contains(q)
            }
        }

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
    val pausedCount = tasks.count { it.status == DownloadStatus.PAUSED }
    val completedCount = tasks.count { it.status == DownloadStatus.COMPLETED }
    val failedCount = tasks.count { it.status == DownloadStatus.FAILED }
    val isAnyDownloading = tasks.any { it.status == DownloadStatus.DOWNLOADING }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)
    ) {
        // 1. CLEAN HEADER
        item {
            UdmScreenHeader(
                title = "Downloads",
                subtitle = when {
                    activeRunningCount > 0 ->
                        "$activeRunningCount active • ${DownloadEngineHelper.formatSpeed(totalCurrentSpeed)}"
                    pausedCount > 0 ->
                        "$pausedCount paused"
                    tasks.isNotEmpty() ->
                        "${tasks.size} ${if (tasks.size == 1) "file" else "files"}"
                    else -> null
                },
                subtitleColor = if (activeRunningCount > 0) colorScheme.primary else colorScheme.onSurfaceVariant
            ) {
                UdmHeaderActionButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Add URL",
                    onClick = { showManualUrlDialog = true }
                )

                if (tasks.isNotEmpty()) {
                    UdmHeaderActionButton(
                        icon = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        selected = isSearchExpanded,
                        onClick = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) searchQuery = ""
                        }
                    )

                    Box {
                        UdmHeaderActionButton(
                            icon = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            onClick = { showMoreMenu = true }
                        )

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isAnyDownloading) {
                                DropdownMenuItem(
                                    text = { Text("Pause all") },
                                    onClick = {
                                        onPauseAll()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Pause, contentDescription = null) }
                                )
                            } else if (pausedCount > 0) {
                                DropdownMenuItem(
                                    text = { Text("Resume all") },
                                    onClick = {
                                        onResumeAll()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
                                )
                            }

                            if (completedCount > 0) {
                                DropdownMenuItem(
                                    text = { Text("Clear completed ($completedCount)") },
                                    onClick = {
                                        onClearCompleted()
                                        showMoreMenu = false
                                        Toast.makeText(context, "Cleared from list (files kept)", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = { Icon(Icons.Default.DownloadDone, contentDescription = null) }
                                )
                            }

                            if (failedCount > 0) {
                                DropdownMenuItem(
                                    text = { Text("Clear failed ($failedCount)") },
                                    onClick = {
                                        onClearFailed()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = colorScheme.error) }
                                )
                            }

                            if (activeRunningCount > 0 || pausedCount > 0) {
                                DropdownMenuItem(
                                    text = { Text("Cancel all active", color = colorScheme.error) },
                                    onClick = {
                                        onCancelAll()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null, tint = colorScheme.error) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. SEARCH BAR (EXPANDABLE)
        item {
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    color = colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colorScheme.onSurface,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search downloads...",
                                        style = TextStyle(color = colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }
            }
        }

        // 3. COMPACT ACTIVE TRANSFER BAR (ONLY WHEN ACTIVE OR PAUSED)
        if (activeRunningCount > 0 || pausedCount > 0) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAnyDownloading) {
                        colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        colorScheme.surfaceContainerLow
                    }
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
                            UdmIconSquircle(
                                icon = Icons.Default.Speed,
                                containerColor = if (isAnyDownloading) colorScheme.primary else colorScheme.surfaceContainerHigh,
                                contentColor = if (isAnyDownloading) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                                size = 38.dp,
                                iconSize = 19.dp,
                                cornerRadius = 11.dp
                            )
                            Column {
                                Text(
                                    text = if (isAnyDownloading) {
                                        DownloadEngineHelper.formatSpeed(totalCurrentSpeed)
                                    } else {
                                        "Queue paused"
                                    },
                                    style = UdmTextStyles.TabularMetricLarge.copy(fontSize = 18.sp, lineHeight = 22.sp),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = if (isAnyDownloading) {
                                        "$activeRunningCount active ${if (activeRunningCount == 1) "download" else "downloads"}"
                                    } else {
                                        "$pausedCount paused"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isAnyDownloading) onPauseAll() else onResumeAll()
                            },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isAnyDownloading) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAnyDownloading) "Pause all" else "Resume all",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        // 4. FILTER CHIPS & SORT (ONLY WHEN THERE ARE TASKS)
        if (tasks.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories, key = { it.first }) { (cat, count) ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategory = cat
                                },
                                label = {
                                    Text(
                                        text = if (count > 0 && cat != "All") "$cat ($count)" else cat,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                shape = CircleShape,
                                border = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = colorScheme.surfaceContainerLow,
                                    selectedContainerColor = colorScheme.primaryContainer,
                                    labelColor = colorScheme.onSurfaceVariant,
                                    selectedLabelColor = colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredTasks.size} ${if (filteredTasks.size == 1) "item" else "items"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showSortMenu = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Sort",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = sortOrder.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                DownloadSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = order.label,
                                                fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                                color = if (sortOrder == order) colorScheme.primary else colorScheme.onSurface
                                            )
                                        },
                                        trailingIcon = if (sortOrder == order) {
                                            { Icon(Icons.Default.Check, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(16.dp)) }
                                        } else null,
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
        }

        // 5. EMPTY STATE
        if (filteredTasks.isEmpty()) {
            item {
                EmptyQueueStateCard(
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    onOpenNewDownload = { showManualUrlDialog = true },
                    onNavigateToBrowser = onNavigateToBrowser
                )
            }
        }

        // 6. DOWNLOAD ITEMS
        items(
            items = filteredTasks,
            key = { it.id },
            contentType = { it.mediaType.name }
        ) { task ->
            DownloadTaskCard(
                modifier = Modifier.animateItem(),
                task = task,
                onPause = { onPauseTask(task.id) },
                onResume = { onResumeTask(task.id) },
                onCancel = { onCancelTask(task.id) },
                onRetry = { onRetryTask(task.id) },
                onDelete = { taskToDelete = task },
                onShowDetails = { taskForDetails = task },
                onPlayPreview = { onPlayPreview(task) },
                onOpenFile = { openExternalFile(context, task) },
                onShare = { shareMedia(context, task) }
            )
        }
    }

    if (showManualUrlDialog) {
        ModernAddDownloadDialog(
            onDismiss = { showManualUrlDialog = false },
            onEnqueue = { url, name, threads ->
                onEnqueueManualDownload(url, name, threads)
                showManualUrlDialog = false
            }
        )
    }

    taskForDetails?.let { detailsTask ->
        DownloadDetailsDialog(
            task = detailsTask,
            onDismiss = { taskForDetails = null }
        )
    }

    taskToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = "Delete download?",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Remove \"${target.fileName.ifEmpty { target.title }}\" from Downloads?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { deleteFileFromDisk = !deleteFileFromDisk }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = deleteFileFromDisk,
                            onCheckedChange = { deleteFileFromDisk = it },
                            colors = CheckboxDefaults.colors(checkedColor = colorScheme.error)
                        )
                        Text(
                            text = "Also delete file from device",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTask(target, deleteFileFromDisk)
                        taskToDelete = null
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                ) {
                    Text("Delete", color = colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (previewTask != null) {
        Media3PlayerDialog(
            task = previewTask,
            onDismiss = onDismissPreview
        )
    }
}

@Composable
private fun DownloadTaskCard(
    modifier: Modifier = Modifier,
    task: DownloadTaskEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onShowDetails: () -> Unit,
    onPlayPreview: () -> Unit,
    onOpenFile: () -> Unit,
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = UdmTheme.extendedColors

    val isCompleted = task.status == DownloadStatus.COMPLETED
    val isPaused = task.status == DownloadStatus.PAUSED
    val isFailed = task.status == DownloadStatus.FAILED
    val isMuxing = task.status == DownloadStatus.MERGING_MUXING
    val isDownloading = task.status == DownloadStatus.DOWNLOADING
    val isPlayable = task.mediaType == MediaType.VIDEO || task.mediaType == MediaType.AUDIO

    val percent = if (task.totalBytes > 0) {
        ((task.downloadedBytes * 100) / task.totalBytes).toInt().coerceIn(0, 100)
    } else 0

    val etaSeconds = MediaSizeCalculator.calculateEtaSeconds(
        downloadedBytes = task.downloadedBytes,
        totalBytes = task.totalBytes,
        speedBytesPerSec = task.speedBytesPerSec
    )
    val etaText = if (etaSeconds > 0L) MediaSizeCalculator.formatEta(etaSeconds) else null

    val ext = task.fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val effectiveMediaType = remember(task.mediaType, ext) {
        if (ext in listOf("apk", "xapk", "apks", "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "exe", "dmg", "iso", "bin")) {
            MediaType.DIRECT_FILE
        } else {
            task.mediaType
        }
    }

    val typeIcon = when (effectiveMediaType) {
        MediaType.VIDEO -> Icons.Default.Movie
        MediaType.AUDIO -> Icons.Default.Audiotrack
        MediaType.DIRECT_FILE -> when (ext) {
            "apk", "xapk", "apks" -> Icons.Default.Android
            "zip", "rar", "7z", "tar", "gz" -> Icons.Default.FolderZip
            "pdf" -> Icons.Default.PictureAsPdf
            else -> Icons.AutoMirrored.Filled.InsertDriveFile
        }
    }

    val typeContainer = when (effectiveMediaType) {
        MediaType.VIDEO -> colorScheme.primaryContainer
        MediaType.AUDIO -> colorScheme.tertiaryContainer
        MediaType.DIRECT_FILE -> extendedColors.successContainer
    }
    val typeOnContainer = when (effectiveMediaType) {
        MediaType.VIDEO -> colorScheme.onPrimaryContainer
        MediaType.AUDIO -> colorScheme.onTertiaryContainer
        MediaType.DIRECT_FILE -> extendedColors.onSuccessContainer
    }

    val metaTag = remember(task, effectiveMediaType, ext) {
        when (effectiveMediaType) {
            MediaType.DIRECT_FILE -> ext.uppercase(Locale.ROOT).ifEmpty { "FILE" }
            MediaType.AUDIO -> task.codecLabel.ifBlank { task.qualityLabel }.uppercase(Locale.ROOT)
            else -> task.qualityLabel
                .replace(" Ultra HD", "")
                .replace(" Full HD", "")
                .replace(" FHD", "")
                .replace(" HD", "")
                .trim()
                .ifBlank { ext.uppercase(Locale.ROOT) }
        }
    }

    UdmTonalCard(
        modifier = modifier.then(
            if (isCompleted) {
                Modifier.clickable {
                    if (isPlayable) onPlayPreview() else onOpenFile()
                }
            } else Modifier
        ),
        shape = RoundedCornerShape(20.dp),
        containerColor = colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UdmIconSquircle(
                    icon = typeIcon,
                    containerColor = typeContainer,
                    contentColor = typeOnContainer,
                    size = 44.dp,
                    iconSize = 21.dp,
                    cornerRadius = 13.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = task.fileName.ifEmpty { task.title },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subtitleText = when {
                        isCompleted -> {
                            val size = formatStorageSize(task.totalBytes.takeIf { it > 0 } ?: task.downloadedBytes)
                            listOfNotNull(size, metaTag.takeIf { it.isNotBlank() }).joinToString(" • ")
                        }
                        isFailed -> task.errorMessage?.take(42) ?: "Download failed"
                        isMuxing -> "Finishing file..."
                        isPaused -> "Paused • ${formatProgressSizes(task.downloadedBytes, task.totalBytes)}"
                        isDownloading -> buildString {
                            append(formatProgressSizes(task.downloadedBytes, task.totalBytes))
                            append(" • ")
                            append(DownloadEngineHelper.formatSpeed(task.speedBytesPerSec))
                            if (etaText != null) append(" • $etaText")
                        }
                        else -> "Waiting in queue..."
                    }

                    Text(
                        text = subtitleText,
                        style = UdmTextStyles.TabularMetricSmall,
                        color = when {
                            isFailed -> colorScheme.error
                            isDownloading -> colorScheme.primary
                            else -> colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Primary action + overflow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when {
                        isCompleted -> {
                            Surface(
                                onClick = { if (isPlayable) onPlayPreview() else onOpenFile() },
                                shape = CircleShape,
                                color = colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlayable) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = if (isPlayable) "Play" else "Open",
                                        tint = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        isFailed -> {
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onRetry()
                                },
                                shape = CircleShape,
                                color = colorScheme.errorContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry",
                                        tint = colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        !isMuxing -> {
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (isPaused) onResume() else onPause()
                                },
                                shape = CircleShape,
                                color = if (isPaused) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (isPaused) "Resume" else "Pause",
                                        tint = if (isPaused) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isCompleted) {
                                DropdownMenuItem(
                                    text = { Text("Open with...") },
                                    onClick = {
                                        showMenu = false
                                        onOpenFile()
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    onClick = {
                                        showMenu = false
                                        onShare()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Copy link") },
                                onClick = {
                                    showMenu = false
                                    copyToClipboard(context, "Link", task.sourcePageUrl)
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Details") },
                                onClick = {
                                    showMenu = false
                                    onShowDetails()
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (isCompleted || isFailed) "Delete" else "Cancel", color = colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    if (isCompleted || isFailed) onDelete() else onCancel()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isCompleted || isFailed) Icons.Default.Delete else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (!isCompleted) {
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = when {
                        isPaused -> extendedColors.warning
                        isFailed -> colorScheme.error
                        isMuxing -> colorScheme.tertiary
                        else -> colorScheme.primary
                    },
                    trackColor = colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}

@Composable
private fun DownloadDetailsDialog(
    task: DownloadTaskEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "File details",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Name", task.fileName.ifEmpty { task.title })
                DetailRow("Size", formatStorageSize(task.totalBytes.takeIf { it > 0 } ?: task.downloadedBytes))
                DetailRow("Quality", "${task.qualityLabel} • ${task.codecLabel}")
                DetailRow("Saved to", task.filePath) {
                    copyToClipboard(context, "Path", task.filePath)
                }
                DetailRow("Source URL", task.sourcePageUrl) {
                    copyToClipboard(context, "URL", task.sourcePageUrl)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyQueueStateCard(
    searchQuery: String,
    selectedCategory: String,
    onOpenNewDownload: () -> Unit,
    onNavigateToBrowser: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        UdmIconSquircle(
            icon = Icons.Default.Download,
            containerColor = colorScheme.primaryContainer.copy(alpha = 0.7f),
            contentColor = colorScheme.onPrimaryContainer,
            size = 64.dp,
            iconSize = 30.dp,
            cornerRadius = 20.dp
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (searchQuery.isNotEmpty()) "No matches" else "No downloads yet",
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onSurface
            )

            Text(
                text = if (searchQuery.isNotEmpty()) {
                    "Nothing matches \"$searchQuery\" in $selectedCategory."
                } else {
                    "Browse any page to detect videos and files, or add a direct link."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onOpenNewDownload,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add link")
            }

            Button(
                onClick = onNavigateToBrowser,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Browse")
            }
        }
    }
}

@Composable
fun ModernAddDownloadDialog(
    onDismiss: () -> Unit,
    onEnqueue: (url: String, fileName: String, threadCount: Int) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var urlText by remember { mutableStateOf("") }
    var fileNameText by remember { mutableStateOf("") }
    var threadSlider by remember { mutableIntStateOf(8) }

    val clipboardText = remember {
        val clip = readClipboardText(context)
        if (!clip.isNullOrBlank() && (clip.startsWith("http://", ignoreCase = true) || clip.startsWith("https://", ignoreCase = true))) {
            clip
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "Add download",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (clipboardText != null && urlText.isEmpty()) {
                    Surface(
                        onClick = { urlText = clipboardText },
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Paste copied link",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        if (urlText.isNotEmpty()) {
                            IconButton(onClick = { urlText = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                )

                OutlinedTextField(
                    value = fileNameText,
                    onValueChange = { fileNameText = it },
                    label = { Text("File name (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Connections",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$threadSlider",
                            style = UdmTextStyles.TabularMetricSmall,
                            color = colorScheme.primary
                        )
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
                    if (urlText.isNotBlank()) {
                        onEnqueue(urlText.trim(), fileNameText.trim(), threadSlider)
                    }
                },
                shape = CircleShape,
                enabled = urlText.isNotBlank()
            ) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun readClipboardText(context: Context): String? {
    return runCatching {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
    }.getOrNull()
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
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
        String.format(Locale.US, "%.1f MB", downloadedMb)
    }
}

private fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}

private fun openExternalFile(context: Context, task: DownloadTaskEntity) {
    try {
        val file = File(task.filePath)
        if (file.exists()) {
            val uri = runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            }.getOrNull() ?: Uri.fromFile(file)

            val fileExt = (file.extension.ifEmpty { task.fileName.substringAfterLast('.', "") }).lowercase(Locale.ROOT)
            val resolvedMime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExt)

            val mimeType = when (task.mediaType) {
                MediaType.VIDEO -> resolvedMime ?: "video/*"
                MediaType.AUDIO -> resolvedMime ?: "audio/*"
                MediaType.DIRECT_FILE -> when (fileExt) {
                    "apk" -> "application/vnd.android.package-archive"
                    "pdf" -> "application/pdf"
                    "zip" -> "application/zip"
                    "tar" -> "application/x-tar"
                    "gz" -> "application/gzip"
                    "7z" -> "application/x-7z-compressed"
                    "rar" -> "application/vnd.rar"
                    else -> resolvedMime ?: "*/*"
                }
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "File not found on device", Toast.LENGTH_SHORT).show()
        }
    } catch (_: Exception) {
        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
    }
}

private fun shareMedia(context: Context, task: DownloadTaskEntity) {
    val fileExt = task.fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val resolvedMime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExt)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = resolvedMime ?: if (task.mediaType == MediaType.VIDEO) "video/*" else if (task.mediaType == MediaType.AUDIO) "audio/*" else "*/*"
        if (task.filePath.startsWith("content://")) {
            putExtra(Intent.EXTRA_STREAM, task.filePath.toUri())
        } else {
            val file = File(task.filePath)
            if (file.exists()) {
                val uri = runCatching {
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                }.getOrNull() ?: Uri.fromFile(file)
                putExtra(Intent.EXTRA_STREAM, uri)
            }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

@Preview(name = "Downloads - Light", showBackground = true)
@Composable
fun LuxuryDownloadsScreenPreviewLight() {
    UDMTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            DownloadsScreenContent(
                tasks = listOf(
                    SamplePreviewTask,
                    SamplePreviewTask.copy(
                        id = "sample_2",
                        title = "Night City FLAC Audio",
                        mediaType = MediaType.AUDIO,
                        qualityLabel = "320kbps",
                        codecLabel = "FLAC",
                        fileName = "NightCity_Soundtrack.flac",
                        downloadedBytes = 120_000_000L,
                        totalBytes = 240_000_000L,
                        speedBytesPerSec = 7_200_000L,
                        status = DownloadStatus.DOWNLOADING
                    ),
                    SamplePreviewTask.copy(
                        id = "sample_3",
                        title = "Linux Kernel Source",
                        mediaType = MediaType.DIRECT_FILE,
                        qualityLabel = "Archive",
                        codecLabel = "TAR.XZ",
                        fileName = "linux-6.12.tar.xz",
                        downloadedBytes = 148_000_000L,
                        totalBytes = 148_000_000L,
                        speedBytesPerSec = 0L,
                        status = DownloadStatus.COMPLETED
                    )
                ),
                chunksMap = emptyMap(),
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
}

@Preview(name = "Downloads - Dark", showBackground = true)
@Composable
fun LuxuryDownloadsScreenPreviewDark() {
    UDMTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            DownloadsScreenContent(
                tasks = listOf(
                    SamplePreviewTask,
                    SamplePreviewTask.copy(
                        id = "sample_2",
                        title = "Night City FLAC Audio",
                        mediaType = MediaType.AUDIO,
                        qualityLabel = "320kbps",
                        codecLabel = "FLAC",
                        fileName = "NightCity_Soundtrack.flac",
                        downloadedBytes = 120_000_000L,
                        totalBytes = 240_000_000L,
                        speedBytesPerSec = 7_200_000L,
                        status = DownloadStatus.DOWNLOADING
                    ),
                    SamplePreviewTask.copy(
                        id = "sample_3",
                        title = "Linux Kernel Source",
                        mediaType = MediaType.DIRECT_FILE,
                        qualityLabel = "Archive",
                        codecLabel = "TAR.XZ",
                        fileName = "linux-6.12.tar.xz",
                        downloadedBytes = 148_000_000L,
                        totalBytes = 148_000_000L,
                        speedBytesPerSec = 0L,
                        status = DownloadStatus.COMPLETED
                    )
                ),
                chunksMap = emptyMap(),
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
}
