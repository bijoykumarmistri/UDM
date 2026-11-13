package com.universal.dm.ui.finished

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universal.dm.data.local.DownloadTaskEntity
import com.universal.dm.data.model.MediaType
import com.universal.dm.ui.theme.CodecAv1Badge
import com.universal.dm.ui.theme.CodecM4aBadge
import com.universal.dm.ui.theme.UDMTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Sample Completed Tasks for Previews
private val SampleCompletedTask1 = DownloadTaskEntity(
    id = "finished_1",
    title = "Blade Runner 2049 4K HDR Final Scene (AV1)",
    sourcePageUrl = "https://example.com/watch?v=bladerunner",
    downloadUrl = "https://example.com/blade.mp4",
    mediaType = MediaType.VIDEO,
    qualityLabel = "2160p 4K",
    codecLabel = "AV1",
    fileName = "Blade_Runner_2049_4K.mp4",
    filePath = "/storage/emulated/0/Download/UDM/Blade_Runner_2049_4K.mp4",
    totalBytes = 1_420_000_000L,
    downloadedBytes = 1_420_000_000L,
    status = com.universal.dm.data.model.DownloadStatus.COMPLETED,
    createdAt = System.currentTimeMillis() - 3_600_000L
)

private val SampleCompletedTask2 = DownloadTaskEntity(
    id = "finished_2",
    title = "Hans Zimmer Live in Prague - Time Master Tape (FLAC)",
    sourcePageUrl = "https://example.com/audio/time",
    downloadUrl = "https://example.com/time.flac",
    mediaType = MediaType.AUDIO,
    qualityLabel = "Lossless",
    codecLabel = "FLAC",
    fileName = "Hans_Zimmer_Time_Live.flac",
    filePath = "/storage/emulated/0/Download/UDM/Hans_Zimmer_Time_Live.flac",
    totalBytes = 85_000_000L,
    downloadedBytes = 85_000_000L,
    status = com.universal.dm.data.model.DownloadStatus.COMPLETED,
    createdAt = System.currentTimeMillis() - 86_400_000L
)

private val SampleCompletedTask3 = DownloadTaskEntity(
    id = "finished_3",
    title = "Android 16 Developer System Images Archive",
    sourcePageUrl = "https://example.com/files/android16.zip",
    downloadUrl = "https://example.com/android16.zip",
    mediaType = MediaType.DIRECT_FILE,
    qualityLabel = "Direct",
    codecLabel = "ZIP",
    fileName = "Android_16_System_Images.zip",
    filePath = "/storage/emulated/0/Download/UDM/Android_16_System_Images.zip",
    totalBytes = 620_000_000L,
    downloadedBytes = 620_000_000L,
    status = com.universal.dm.data.model.DownloadStatus.COMPLETED,
    createdAt = System.currentTimeMillis() - 172_800_000L
)

enum class FinishedSortOrder(val label: String) {
    RECENT("Most Recent"),
    OLDEST("Oldest First"),
    LARGEST("Largest Size"),
    SMALLEST("Smallest Size"),
    NAME("Name (A-Z)")
}

enum class LibraryViewMode {
    LIST,
    GRID
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedScreen(
    modifier: Modifier = Modifier
) {
    FinishedScreen(modifier = modifier, viewModel = null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedScreen(
    modifier: Modifier = Modifier,
    viewModel: FinishedViewModel?
) {
    val isInspection = LocalInspectionMode.current
    if (isInspection) {
        FinishedScreenContent(
            modifier = modifier,
            completedTasks = listOf(SampleCompletedTask1, SampleCompletedTask2, SampleCompletedTask3),
            previewTask = null,
            onPlayPreview = {},
            onDeleteTask = { _, _ -> },
            onDeleteMultipleTasks = { _, _ -> },
            onClearHistory = { _ -> },
            onDismissPreview = {}
        )
    } else {
        val vm: FinishedViewModel = viewModel ?: viewModel()
        val completedTasks by vm.completedTasks.collectAsState()
        val previewTask by vm.previewTask.collectAsState()

        FinishedScreenContent(
            modifier = modifier,
            completedTasks = completedTasks,
            previewTask = previewTask,
            onPlayPreview = { vm.selectTaskForPreview(it) },
            onDeleteTask = { task, deleteFromStorage -> vm.deleteTask(task, deleteFromStorage) },
            onDeleteMultipleTasks = { tasks, deleteFromStorage -> vm.deleteMultipleTasks(tasks, deleteFromStorage) },
            onClearHistory = { deleteFromStorage -> vm.clearAllHistory(deleteFromStorage) },
            onDismissPreview = { vm.selectTaskForPreview(null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FinishedScreenContent(
    modifier: Modifier = Modifier,
    completedTasks: List<DownloadTaskEntity>,
    previewTask: DownloadTaskEntity?,
    onPlayPreview: (DownloadTaskEntity) -> Unit,
    onDeleteTask: (DownloadTaskEntity, Boolean) -> Unit,
    onDeleteMultipleTasks: (List<DownloadTaskEntity>, Boolean) -> Unit,
    onClearHistory: (Boolean) -> Unit,
    onDismissPreview: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf(FinishedSortOrder.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(LibraryViewMode.LIST) }

    // Multi-select state
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedTaskIds = remember { mutableStateListOf<String>() }

    // Deletion Dialog State
    var taskToDelete by remember { mutableStateOf<DownloadTaskEntity?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val categories = remember(completedTasks) {
        listOf(
            "All" to completedTasks.size,
            "Videos" to completedTasks.count { it.mediaType == MediaType.VIDEO },
            "Audios" to completedTasks.count { it.mediaType == MediaType.AUDIO },
            "Files" to completedTasks.count { it.mediaType == MediaType.DIRECT_FILE }
        )
    }

    // Filter & Sort Logic
    val filteredList = remember(completedTasks, searchQuery, selectedCategory, sortOrder) {
        var list = completedTasks

        // Category Filter
        list = when (selectedCategory) {
            "Videos" -> list.filter { it.mediaType == MediaType.VIDEO }
            "Audios" -> list.filter { it.mediaType == MediaType.AUDIO }
            "Files" -> list.filter { it.mediaType == MediaType.DIRECT_FILE }
            else -> list
        }

        // Search Filter
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            list = list.filter {
                it.title.lowercase(Locale.ROOT).contains(q) ||
                        it.fileName.lowercase(Locale.ROOT).contains(q) ||
                        it.codecLabel.lowercase(Locale.ROOT).contains(q)
            }
        }

        // Sort Order
        when (sortOrder) {
            FinishedSortOrder.RECENT -> list.sortedByDescending { it.createdAt }
            FinishedSortOrder.OLDEST -> list.sortedBy { it.createdAt }
            FinishedSortOrder.LARGEST -> list.sortedByDescending { it.totalBytes }
            FinishedSortOrder.SMALLEST -> list.sortedBy { it.totalBytes }
            FinishedSortOrder.NAME -> list.sortedBy { it.fileName.lowercase(Locale.ROOT) }
        }
    }

    val totalStorageBytes = remember(completedTasks) { completedTasks.sumOf { it.totalBytes } }
    val totalVideosBytes = remember(completedTasks) { completedTasks.filter { it.mediaType == MediaType.VIDEO }.sumOf { it.totalBytes } }
    val totalAudiosBytes = remember(completedTasks) { completedTasks.filter { it.mediaType == MediaType.AUDIO }.sumOf { it.totalBytes } }
    val totalFilesBytes = remember(completedTasks) { completedTasks.filter { it.mediaType == MediaType.DIRECT_FILE }.sumOf { it.totalBytes } }

    Box(modifier = modifier.fillMaxSize()) {
        if (viewMode == LibraryViewMode.LIST) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                // 1. TOP HEADER & TELEMETRY
                item {
                    LibraryTopHeader(
                        fileCount = completedTasks.size,
                        totalBytes = totalStorageBytes,
                        isSelectionMode = isSelectionMode,
                        selectedCount = selectedTaskIds.size,
                        isSearchExpanded = isSearchExpanded,
                        viewMode = viewMode,
                        onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                        onToggleViewMode = {
                            viewMode = if (viewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST
                        },
                        onToggleSelectionMode = {
                            isSelectionMode = !isSelectionMode
                            if (!isSelectionMode) selectedTaskIds.clear()
                        },
                        onOpenMenu = { showMoreMenu = true }
                    )
                }

                // 2. EXPANDABLE SEARCH BAR
                if (isSearchExpanded) {
                    item {
                        LibrarySearchBar(
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it }
                        )
                    }
                }

                // 3. STORAGE & INGEST ANALYTICS HERO CARD
                if (!isSelectionMode && searchQuery.isEmpty()) {
                    item {
                        LibraryStorageHeroCard(
                            totalBytes = totalStorageBytes,
                            videoBytes = totalVideosBytes,
                            audioBytes = totalAudiosBytes,
                            fileBytes = totalFilesBytes,
                            totalFiles = completedTasks.size,
                            onOpenFolder = { openUdmFolder(context) }
                        )
                    }
                }

                // 4. CATEGORY FILTER CHIPS & SORT DROPDOWN
                item {
                    LibraryFilterAndSortRow(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it },
                        sortOrder = sortOrder,
                        showSortMenu = showSortMenu,
                        onOpenSortMenu = { showSortMenu = true },
                        onDismissSortMenu = { showSortMenu = false },
                        onSelectSortOrder = {
                            sortOrder = it
                            showSortMenu = false
                        }
                    )
                }

                // 5. EMPTY STATE
                if (filteredList.isEmpty()) {
                    item {
                        EmptyLibraryStateCard(
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            onOpenFolder = { openUdmFolder(context) }
                        )
                    }
                }

                // 6. COMPLETED MEDIA CARDS (LIST)
                items(filteredList, key = { it.id }) { task ->
                    val isSelected = selectedTaskIds.contains(task.id)
                    LuxuryCompletedMediaCard(
                        task = task,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onSelectToggle = {
                            if (isSelected) selectedTaskIds.remove(task.id) else selectedTaskIds.add(task.id)
                        },
                        onPlayPreview = { onPlayPreview(task) },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedTaskIds.add(task.id)
                            } else {
                                if (isSelected) selectedTaskIds.remove(task.id) else selectedTaskIds.add(task.id)
                            }
                        },
                        onShare = { shareMedia(context, task) },
                        onOpenFile = { openExternalFile(context, task) },
                        onDeleteClick = { taskToDelete = task }
                    )
                }
            }
        } else {
            // 2-COLUMN LUXURY GRID VIEW
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                item(span = { GridItemSpan(2) }) {
                    LibraryTopHeader(
                        fileCount = completedTasks.size,
                        totalBytes = totalStorageBytes,
                        isSelectionMode = isSelectionMode,
                        selectedCount = selectedTaskIds.size,
                        isSearchExpanded = isSearchExpanded,
                        viewMode = viewMode,
                        onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                        onToggleViewMode = {
                            viewMode = if (viewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST
                        },
                        onToggleSelectionMode = {
                            isSelectionMode = !isSelectionMode
                            if (!isSelectionMode) selectedTaskIds.clear()
                        },
                        onOpenMenu = { showMoreMenu = true }
                    )
                }

                if (isSearchExpanded) {
                    item(span = { GridItemSpan(2) }) {
                        LibrarySearchBar(
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it }
                        )
                    }
                }

                if (!isSelectionMode && searchQuery.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        LibraryStorageHeroCard(
                            totalBytes = totalStorageBytes,
                            videoBytes = totalVideosBytes,
                            audioBytes = totalAudiosBytes,
                            fileBytes = totalFilesBytes,
                            totalFiles = completedTasks.size,
                            onOpenFolder = { openUdmFolder(context) }
                        )
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    LibraryFilterAndSortRow(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it },
                        sortOrder = sortOrder,
                        showSortMenu = showSortMenu,
                        onOpenSortMenu = { showSortMenu = true },
                        onDismissSortMenu = { showSortMenu = false },
                        onSelectSortOrder = {
                            sortOrder = it
                            showSortMenu = false
                        }
                    )
                }

                if (filteredList.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        EmptyLibraryStateCard(
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            onOpenFolder = { openUdmFolder(context) }
                        )
                    }
                }

                items(filteredList, key = { it.id }) { task ->
                    val isSelected = selectedTaskIds.contains(task.id)
                    LuxuryCompletedGridCard(
                        task = task,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onSelectToggle = {
                            if (isSelected) selectedTaskIds.remove(task.id) else selectedTaskIds.add(task.id)
                        },
                        onPlayPreview = { onPlayPreview(task) },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedTaskIds.add(task.id)
                            }
                        },
                        onShare = { shareMedia(context, task) },
                        onOpenFile = { openExternalFile(context, task) },
                        onDeleteClick = { taskToDelete = task }
                    )
                }
            }
        }

        // BATCH SELECTION FLOATING ACTION BAR
        if (isSelectionMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF07141E),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedTaskIds.size} Selected",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Select All / Deselect All
                        Surface(
                            onClick = {
                                if (selectedTaskIds.size == filteredList.size) {
                                    selectedTaskIds.clear()
                                } else {
                                    selectedTaskIds.clear()
                                    selectedTaskIds.addAll(filteredList.map { it.id })
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF132838)
                        ) {
                            Text(
                                text = if (selectedTaskIds.size == filteredList.size) "Deselect All" else "Select All",
                                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC4D8E6)),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }

                        // Batch Delete Button
                        Surface(
                            onClick = {
                                if (selectedTaskIds.isNotEmpty()) showBatchDeleteDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTaskIds.isNotEmpty()) Color(0xFFFF5252).copy(alpha = 0.2f) else Color(0xFF1B242D),
                            border = BorderStroke(1.dp, if (selectedTaskIds.isNotEmpty()) Color(0xFFFF5252) else Color(0xFF2A3642))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Delete (${selectedTaskIds.size})",
                                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                                )
                            }
                        }

                        // Exit Selection Mode
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedTaskIds.clear()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color(0xFF8BA6B9))
                        }
                    }
                }
            }
        }
    }

    // TOP RIGHT OVERFLOW MENU
    DropdownMenu(
        expanded = showMoreMenu,
        onDismissRequest = { showMoreMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Open UDM Downloads Folder") },
            onClick = {
                openUdmFolder(context)
                showMoreMenu = false
            },
            leadingIcon = {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF00E5FF))
            }
        )
        DropdownMenuItem(
            text = { Text("Select Items (Batch Mode)") },
            onClick = {
                isSelectionMode = true
                showMoreMenu = false
            },
            leadingIcon = {
                Icon(Icons.Default.SelectAll, contentDescription = null, tint = Color(0xFF8BA6B9))
            }
        )
        HorizontalDivider(color = Color(0xFF1C3647))
        DropdownMenuItem(
            text = { Text("Clear Finished History") },
            onClick = {
                showClearHistoryDialog = true
                showMoreMenu = false
            },
            leadingIcon = {
                Icon(Icons.Default.ClearAll, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        )
    }

    // SINGLE ITEM DELETE DIALOG
    taskToDelete?.let { task ->
        DualDeleteConfirmationDialog(
            title = "Delete Downloaded Item",
            subtitle = task.fileName.ifEmpty { task.title },
            onDeleteFromStorageAndApp = {
                onDeleteTask(task, true)
                taskToDelete = null
            },
            onDeleteFromAppOnly = {
                onDeleteTask(task, false)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }

    // BATCH DELETE DIALOG
    if (showBatchDeleteDialog) {
        val targets = completedTasks.filter { selectedTaskIds.contains(it.id) }
        DualDeleteConfirmationDialog(
            title = "Delete ${targets.size} Downloaded Files",
            subtitle = "You have selected ${targets.size} files (${formatStorageSize(targets.sumOf { it.totalBytes })}).",
            onDeleteFromStorageAndApp = {
                onDeleteMultipleTasks(targets, true)
                selectedTaskIds.clear()
                isSelectionMode = false
                showBatchDeleteDialog = false
            },
            onDeleteFromAppOnly = {
                onDeleteMultipleTasks(targets, false)
                selectedTaskIds.clear()
                isSelectionMode = false
                showBatchDeleteDialog = false
            },
            onDismiss = { showBatchDeleteDialog = false }
        )
    }

    // CLEAR HISTORY DIALOG
    if (showClearHistoryDialog) {
        DualDeleteConfirmationDialog(
            title = "Clear Download History",
            subtitle = "Remove all completed items from download history.",
            onDeleteFromStorageAndApp = {
                onClearHistory(true)
                showClearHistoryDialog = false
            },
            onDeleteFromAppOnly = {
                onClearHistory(false)
                showClearHistoryDialog = false
            },
            onDismiss = { showClearHistoryDialog = false }
        )
    }

    // Media3 Player Modal Sheet
    if (previewTask != null) {
        Media3PlayerDialog(
            task = previewTask,
            onDismiss = onDismissPreview
        )
    }
}

/**
 * Top Header with Title, Telemetry, View Mode and Control Buttons
 */
@Composable
fun LibraryTopHeader(
    fileCount: Int,
    totalBytes: Long,
    isSelectionMode: Boolean,
    selectedCount: Int,
    isSearchExpanded: Boolean,
    viewMode: LibraryViewMode,
    onToggleSearch: () -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )
            Text(
                text = if (isSelectionMode) "$selectedCount selected of $fileCount" else "$fileCount files • ${formatStorageSize(totalBytes)} library storage",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelectionMode) Color(0xFF00E5FF) else Color(0xFF8BA6B9)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search toggle button
            Surface(
                onClick = onToggleSearch,
                shape = CircleShape,
                color = if (isSearchExpanded) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF0C1924),
                border = BorderStroke(1.dp, if (isSearchExpanded) Color(0xFF00E5FF) else Color(0xFF1B3547)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchExpanded) Color(0xFF00E5FF) else Color(0xFF8BA6B9),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // View Mode Toggle (List vs Grid)
            Surface(
                onClick = onToggleViewMode,
                shape = CircleShape,
                color = Color(0xFF0C1924),
                border = BorderStroke(1.dp, Color(0xFF1B3547)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (viewMode == LibraryViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = "Toggle View",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Selection Mode Toggle
            Surface(
                onClick = onToggleSelectionMode,
                shape = CircleShape,
                color = if (isSelectionMode) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF0C1924),
                border = BorderStroke(1.dp, if (isSelectionMode) Color(0xFF00E5FF) else Color(0xFF1B3547)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSelectionMode) Icons.Default.Check else Icons.Default.SelectAll,
                        contentDescription = "Select",
                        tint = if (isSelectionMode) Color(0xFF00E5FF) else Color(0xFF8BA6B9),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // More Options
            Surface(
                onClick = onOpenMenu,
                shape = CircleShape,
                color = Color(0xFF0C1924),
                border = BorderStroke(1.dp, Color(0xFF1B3547)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color(0xFF8BA6B9),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Expandable Modern Search Bar
 */
@Composable
fun LibrarySearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit
) {
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
                onValueChange = onQueryChange,
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
                            text = "Filter downloaded files or media format...",
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
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}

/**
 * Storage & Ingest Analytics Hero Card
 * Visualizes storage distribution (Video vs Audio vs Files) with a sleek multi-segment bar and 1-tap folder explorer.
 */
@Composable
fun LibraryStorageHeroCard(
    totalBytes: Long,
    videoBytes: Long,
    audioBytes: Long,
    fileBytes: Long,
    totalFiles: Int,
    onOpenFolder: () -> Unit
) {
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
            // Header: Storage Used + Open Folder Action
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
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "OFFLINE STORAGE USED ($totalFiles FILES)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = Color(0xFF7D9AB0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatStorageSize(totalBytes),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Open UDM Folder pill
                Surface(
                    onClick = onOpenFolder,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F2636),
                    border = BorderStroke(1.dp, Color(0xFF1E435B))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                        Text(
                            text = "Browse Disk",
                            style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF)),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Multi-segment Storage Bar
            val total = totalBytes.coerceAtLeast(1L).toFloat()
            val videoFraction = (videoBytes.toFloat() / total).coerceIn(0f, 1f)
            val audioFraction = (audioBytes.toFloat() / total).coerceIn(0f, 1f)
            val filesFraction = (fileBytes.toFloat() / total).coerceIn(0f, 1f)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF122432))
                ) {
                    if (videoFraction > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(videoFraction)
                                .height(8.dp)
                                .background(Color(0xFF00E5FF))
                        )
                    }
                    if (audioFraction > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(audioFraction)
                                .height(8.dp)
                                .background(Color(0xFFD05CE3))
                        )
                    }
                    if (filesFraction > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(filesFraction)
                                .height(8.dp)
                                .background(Color(0xFF00E676))
                        )
                    }
                }

                // Storage Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StorageLegendItem(color = Color(0xFF00E5FF), label = "Video", sizeText = formatStorageSize(videoBytes))
                    StorageLegendItem(color = Color(0xFFD05CE3), label = "Audio", sizeText = formatStorageSize(audioBytes))
                    StorageLegendItem(color = Color(0xFF00E676), label = "Files", sizeText = formatStorageSize(fileBytes))
                }
            }
        }
    }
}

@Composable
fun StorageLegendItem(color: Color, label: String, sizeText: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: $sizeText",
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF8BA6B9))
        )
    }
}

/**
 * Filter Chips and Sort Selector Row
 */
@Composable
fun LibraryFilterAndSortRow(
    categories: List<Pair<String, Int>>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    sortOrder: FinishedSortOrder,
    showSortMenu: Boolean,
    onOpenSortMenu: () -> Unit,
    onDismissSortMenu: () -> Unit,
    onSelectSortOrder: (FinishedSortOrder) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Full-Width Scrollable Category Filter Chips
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
                        onSelectCategory(cat)
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

        // 2. Sub-Row: Active Category Tag on Left & Sort Dropdown on Right (Zero Overlap Guaranteed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Context Tag
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
                        "Videos" -> "Filtered by Videos"
                        "Audios" -> "Filtered by Audios"
                        "Files" -> "Filtered by Documents & Files"
                        else -> "All Downloaded Library"
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
                        onOpenSortMenu()
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
                    onDismissRequest = onDismissSortMenu,
                    modifier = Modifier.background(Color(0xFF0B1722))
                ) {
                    FinishedSortOrder.entries.forEach { order ->
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
                            onClick = { onSelectSortOrder(order) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Luxury Completed Media Card (List View)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LuxuryCompletedMediaCard(
    task: DownloadTaskEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onPlayPreview: () -> Unit,
    onLongClick: () -> Unit,
    onShare: () -> Unit,
    onOpenFile: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val dateText = remember(task.createdAt) { dateFormat.format(Date(task.createdAt)) }

    val isMedia = task.mediaType == MediaType.VIDEO || task.mediaType == MediaType.AUDIO

    val themeColor = when (task.mediaType) {
        MediaType.VIDEO -> Color(0xFF00E5FF)
        MediaType.AUDIO -> Color(0xFFD05CE3)
        MediaType.DIRECT_FILE -> Color(0xFF00E676)
    }

    val typeIcon = when (task.mediaType) {
        MediaType.VIDEO -> Icons.Default.Movie
        MediaType.AUDIO -> Icons.Default.Audiotrack
        MediaType.DIRECT_FILE -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onSelectToggle() else if (isMedia) onPlayPreview() else onOpenFile()
                },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) Color(0xFF0B2131) else Color(0xFF091622),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF00E5FF) else Color(0xFF132838)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Checkbox / Media Icon, Title & Date, Actions
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
                    if (isSelectionMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Select",
                            tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF5A7285),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Media Category Capsule
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(themeColor.copy(alpha = 0.15f))
                            .border(1.dp, themeColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
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

                        Text(
                            text = "${formatStorageSize(task.totalBytes)} • $dateText",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF7F9CB0)
                            )
                        )
                    }
                }

                // Action Buttons: Share, Open External, Delete
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF8BA6B9), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onOpenFile, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open In", tint = Color(0xFF8BA6B9), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF112330))

            // Row 2: Codec / Format Chip & Big Play Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Codec & Quality badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = themeColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "${task.codecLabel} • ${task.qualityLabel}",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Play / Open action button
                if (isMedia) {
                    val playSource = remember { MutableInteractionSource() }
                    val isPlayPressed by playSource.collectIsPressedAsState()
                    val playScale by animateFloatAsState(
                        targetValue = if (isPlayPressed) 0.94f else 1f,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                        label = "playScale"
                    )

                    Surface(
                        onClick = onPlayPreview,
                        modifier = Modifier.graphicsLayer {
                            scaleX = playScale
                            scaleY = playScale
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                        interactionSource = playSource
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Play Media",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                            )
                        }
                    }
                } else {
                    Surface(
                        onClick = onOpenFile,
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF132838)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Color(0xFF8BA6B9), modifier = Modifier.size(14.dp))
                            Text(
                                text = "Open File",
                                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC4D8E6))
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Luxury 2-Column Grid Card for Media & Downloads
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LuxuryCompletedGridCard(
    task: DownloadTaskEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onPlayPreview: () -> Unit,
    onLongClick: () -> Unit,
    onShare: () -> Unit,
    onOpenFile: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isMedia = task.mediaType == MediaType.VIDEO || task.mediaType == MediaType.AUDIO

    val themeColor = when (task.mediaType) {
        MediaType.VIDEO -> Color(0xFF00E5FF)
        MediaType.AUDIO -> Color(0xFFD05CE3)
        MediaType.DIRECT_FILE -> Color(0xFF00E676)
    }

    val typeIcon = when (task.mediaType) {
        MediaType.VIDEO -> Icons.Default.Movie
        MediaType.AUDIO -> Icons.Default.Audiotrack
        MediaType.DIRECT_FILE -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onSelectToggle() else if (isMedia) onPlayPreview() else onOpenFile()
                },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF0B2131) else Color(0xFF091622),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF00E5FF) else Color(0xFF132838)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Media Preview Poster / Glowing Icon Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(themeColor.copy(alpha = 0.25f), Color(0xFF08141F))
                        )
                    )
                    .border(1.dp, themeColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(38.dp)
                )

                if (isMedia) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Top right selection checkbox
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF7F9CB0),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Format badge in bottom left of poster
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF001F2D).copy(alpha = 0.85f),
                    border = BorderStroke(0.5.dp, themeColor)
                ) {
                    Text(
                        text = task.codecLabel,
                        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = themeColor),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Title & File Size
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = task.fileName.ifEmpty { task.title },
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatStorageSize(task.totalBytes)} • ${task.qualityLabel}",
                    style = TextStyle(fontSize = 10.sp, color = Color(0xFF7F9CB0))
                )
            }

            // Quick Actions: Share + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF8BA6B9), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * Empty Library State Card
 */
@Composable
fun EmptyLibraryStateCard(
    searchQuery: String,
    selectedCategory: String,
    onOpenFolder: () -> Unit
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
                    imageVector = Icons.Default.FolderOpen,
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
                    text = if (searchQuery.isNotEmpty()) "No Files Match Search" else "No Completed Downloads",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )

                Text(
                    text = if (searchQuery.isNotEmpty())
                        "No downloads found matching \"$searchQuery\" in category \"$selectedCategory\"."
                    else
                        "Downloaded video streams, audio tracks, and direct files will appear here ready for instant offline playback.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF88A0B2),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    lineHeight = 20.sp
                )
            }

            Surface(
                onClick = onOpenFolder,
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
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Browse Storage Folder",
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
 * Dual Choice Delete Safeguard Dialog
 * Offers user the freedom to delete from physical disk storage or keep the file and remove history only.
 */
@Composable
fun DualDeleteConfirmationDialog(
    title: String,
    subtitle: String,
    onDeleteFromStorageAndApp: () -> Unit,
    onDeleteFromAppOnly: () -> Unit,
    onDismiss: () -> Unit
) {
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
                        .background(Color(0xFFFF5252).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFFD6E2EB),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Select your preferred deletion method:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7F9DB0)
                )

                // Option 1: Delete from Storage & App
                Button(
                    onClick = onDeleteFromStorageAndApp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete from Disk Storage & App", fontWeight = FontWeight.Bold)
                }

                // Option 2: Remove from History Only
                OutlinedButton(
                    onClick = onDeleteFromAppOnly,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E3C50))
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircleOutline,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove History Only (Keep File)", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold)
                }

                // Cancel
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = Color(0xFF8BA6B9))
                }
            }
        },
        confirmButton = {},
        dismissButton = null,
        shape = RoundedCornerShape(26.dp),
        containerColor = Color(0xFF07131D)
    )
}

// Helpers
private fun formatStorageSize(bytes: Long): String {
    return if (bytes >= 1024L * 1024L * 1024L) {
        String.format(Locale.US, "%.2f GB", bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
    } else if (bytes > 0) {
        String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024.0 * 1024.0))
    } else {
        "0 MB"
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
        try {
            val genericIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(genericIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Storage folder located at /Download/UDM", Toast.LENGTH_LONG).show()
        }
    }
}

private fun shareMedia(context: Context, task: DownloadTaskEntity) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = if (task.mediaType == MediaType.VIDEO) "video/*" else if (task.mediaType == MediaType.AUDIO) "audio/*" else "*/*"
        if (task.filePath.startsWith("content://")) {
            putExtra(Intent.EXTRA_STREAM, Uri.parse(task.filePath))
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
    context.startActivity(Intent.createChooser(shareIntent, "Share Media").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun openExternalFile(context: Context, task: DownloadTaskEntity) {
    try {
        val file = File(task.filePath)
        if (file.exists()) {
            val uri = runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            }.getOrNull() ?: Uri.fromFile(file)

            val mimeType = when (task.mediaType) {
                MediaType.VIDEO -> "video/*"
                MediaType.AUDIO -> "audio/*"
                MediaType.DIRECT_FILE -> "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "File does not exist on disk", Toast.LENGTH_SHORT).show()
        }
    } catch (_: Exception) {
        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060D13)
@Composable
fun LuxuryFinishedScreenPreview() {
    UDMTheme {
        FinishedScreenContent(
            completedTasks = listOf(SampleCompletedTask1, SampleCompletedTask2, SampleCompletedTask3),
            previewTask = null,
            onPlayPreview = {},
            onDeleteTask = { _, _ -> },
            onDeleteMultipleTasks = { _, _ -> },
            onClearHistory = { _ -> },
            onDismissPreview = {}
        )
    }
}
