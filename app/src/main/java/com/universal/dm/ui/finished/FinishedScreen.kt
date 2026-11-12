package com.universal.dm.ui.finished

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universal.dm.data.local.DownloadTaskEntity
import com.universal.dm.data.model.MediaType
import com.universal.dm.ui.theme.CodecAv1Badge
import com.universal.dm.ui.theme.CodecM4aBadge
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            completedTasks = emptyList(),
            previewTask = null,
            onPlayPreview = {},
            onDeleteTask = { _, _ -> },
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
            onDismissPreview = { vm.selectTaskForPreview(null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedScreenContent(
    modifier: Modifier = Modifier,
    completedTasks: List<DownloadTaskEntity>,
    previewTask: DownloadTaskEntity?,
    onPlayPreview: (DownloadTaskEntity) -> Unit,
    onDeleteTask: (DownloadTaskEntity, Boolean) -> Unit,
    onDismissPreview: () -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("All") }
    var taskToDelete by remember { mutableStateOf<DownloadTaskEntity?>(null) }

    val filteredList = remember(completedTasks, selectedFilter) {
        when (selectedFilter) {
            "Videos" -> completedTasks.filter { it.mediaType == MediaType.VIDEO }
            "Audios" -> completedTasks.filter { it.mediaType == MediaType.AUDIO }
            "Files" -> completedTasks.filter { it.mediaType == MediaType.DIRECT_FILE }
            else -> completedTasks
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Library & Finished",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${completedTasks.size} files downloaded • Tap to play • Press & hold for delete options",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val categories = listOf("All", "Videos", "Audios", "Files")
                items(categories) { category ->
                    FilterChip(
                        selected = selectedFilter == category,
                        onClick = { selectedFilter = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Empty Library Placeholder
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No completed downloads in this filter",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Completed Media Items
        items(filteredList, key = { it.id }) { task ->
            CompletedMediaCard(
                task = task,
                onPlayPreview = { onPlayPreview(task) },
                onLongClick = { taskToDelete = task },
                onShare = {
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
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                },
                onDeleteClick = { taskToDelete = task }
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    // Delete Confirmation Dialog with Dual Delete Choices (Delete from App vs Delete from Storage)
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("Delete Downloaded File", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = task.fileName.ifEmpty { task.title },
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Choose how you want to delete this file:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: Delete from App & Device Storage
                    Button(
                        onClick = {
                            onDeleteTask(task, true)
                            taskToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
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
                        Text("Delete from Storage & App")
                    }

                    // Option 2: Delete from App Only (Keeps physical file on disk)
                    OutlinedButton(
                        onClick = {
                            onDeleteTask(task, false)
                            taskToDelete = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete from App Only (Keep File)")
                    }

                    // Cancel
                    FilledTonalButton(
                        onClick = { taskToDelete = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompletedMediaCard(
    task: DownloadTaskEntity,
    onPlayPreview: () -> Unit,
    onLongClick: () -> Unit,
    onShare: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val dateText = remember(task.createdAt) { dateFormat.format(Date(task.createdAt)) }

    val codecColor = when {
        task.codecLabel.contains("AV1", ignoreCase = true) -> CodecAv1Badge
        task.codecLabel.contains("M4A", ignoreCase = true) -> CodecM4aBadge
        else -> MaterialTheme.colorScheme.primary
    }

    val isMedia = task.mediaType == MediaType.VIDEO || task.mediaType == MediaType.AUDIO

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isMedia) onPlayPreview() },
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Category Icon
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (task.mediaType) {
                                MediaType.VIDEO -> Icons.Default.Movie
                                MediaType.AUDIO -> Icons.Default.Audiotrack
                                MediaType.DIRECT_FILE -> Icons.Default.Folder
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = task.fileName.ifEmpty { task.title },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val sizeMb = task.totalBytes.toFloat() / (1024 * 1024)
                        Text(
                            text = String.format(Locale.US, "%.1f MB • %s", sizeMb, dateText),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onShare) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Bottom row: Codec badge & In-App Preview Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = "${task.codecLabel} • ${task.qualityLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = codecColor.copy(alpha = 0.15f),
                        labelColor = codecColor
                    ),
                    border = null
                )

                if (isMedia) {
                    FilledTonalButton(
                        onClick = onPlayPreview,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
