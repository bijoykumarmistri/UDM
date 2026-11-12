package com.universal.dm.ui.finished

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universal.dm.data.local.DownloadTaskEntity
import com.universal.dm.data.model.MediaType
import com.universal.dm.ui.theme.CodecAv1Badge
import com.universal.dm.ui.theme.CodecM4aBadge
import androidx.compose.ui.platform.LocalInspectionMode
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
            onDeleteTask = {},
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
            onDeleteTask = { vm.deleteCompletedTask(it) },
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
    onDeleteTask: (DownloadTaskEntity) -> Unit,
    onDismissPreview: () -> Unit
) {
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf("All") }
    val categories = listOf("All", "Videos (AV1/MP4)", "Audio (M4A/MP3)", "Files")

    val filteredList = remember(completedTasks, selectedFilter) {
        when (selectedFilter) {
            "Videos (AV1/MP4)" -> completedTasks.filter { it.mediaType == MediaType.VIDEO }
            "Audio (M4A/MP3)" -> completedTasks.filter { it.mediaType == MediaType.AUDIO }
            "Files" -> completedTasks.filter { it.mediaType == MediaType.DIRECT_FILE }
            else -> completedTasks
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedFilter == cat,
                        onClick = { selectedFilter = cat },
                        label = { Text(cat) },
                        shape = MaterialTheme.shapes.medium,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Empty Library State
        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "No Completed Media",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Downloaded AV1 videos, M4A music, and files will appear here for instant in-app playback and sharing.",
                            style = MaterialTheme.typography.bodyMedium,
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
                onDelete = { onDeleteTask(task) }
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    // Media3 Player Modal Sheet
    if (previewTask != null) {
        Media3PlayerDialog(
            task = previewTask,
            onDismiss = onDismissPreview
        )
    }
}

@Composable
fun CompletedMediaCard(
    task: DownloadTaskEntity,
    onPlayPreview: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
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
            .clickable(enabled = isMedia, onClick = onPlayPreview),
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
                    IconButton(onClick = onDelete) {
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
                        Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (task.mediaType == MediaType.VIDEO) "Watch Video" else "Listen Audio")
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CompletedMediaCardPreview() {
    com.universal.dm.ui.theme.UDMTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CompletedMediaCard(
                task = DownloadTaskEntity(
                    id = "completed_sample",
                    title = "Interstellar Main Theme Soundtrack (M4A AAC)",
                    sourcePageUrl = "https://example.com/audio",
                    downloadUrl = "https://example.com/audio.m4a",
                    mediaType = MediaType.AUDIO,
                    qualityLabel = "256 kbps",
                    codecLabel = "M4A",
                    fileName = "Interstellar_Theme.m4a",
                    filePath = "/storage/emulated/0/Download/UDM/Interstellar_Theme.m4a",
                    totalBytes = 18_400_000L,
                    downloadedBytes = 18_400_000L,
                    status = com.universal.dm.data.model.DownloadStatus.COMPLETED
                ),
                onPlayPreview = {},
                onShare = {},
                onDelete = {}
            )
        }
    }
}
