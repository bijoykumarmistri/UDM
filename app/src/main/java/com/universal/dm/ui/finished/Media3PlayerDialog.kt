package com.universal.dm.ui.finished

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.universal.dm.data.local.DownloadTaskEntity
import com.universal.dm.data.model.MediaType
import com.universal.dm.ui.theme.CodecAv1Badge
import com.universal.dm.ui.theme.CodecM4aBadge
import java.io.File

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun Media3PlayerDialog(
    task: DownloadTaskEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Build and prepare ExoPlayer instance
    val exoPlayer = remember(task.filePath) {
        ExoPlayer.Builder(context).build().apply {
            val uri = runCatching {
                if (task.filePath.startsWith("content://")) {
                    Uri.parse(task.filePath)
                } else {
                    val file = File(task.filePath)
                    if (file.exists()) {
                        Uri.fromFile(file)
                    } else {
                        Uri.parse(task.downloadUrl)
                    }
                }
            }.getOrDefault(Uri.parse(task.downloadUrl))

            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    val codecColor = when {
        task.codecLabel.contains("AV1", ignoreCase = true) -> CodecAv1Badge
        task.codecLabel.contains("M4A", ignoreCase = true) -> CodecM4aBadge
        else -> MaterialTheme.colorScheme.primary
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            shape = MaterialTheme.shapes.small,
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = codecColor.copy(alpha = 0.15f),
                                labelColor = codecColor
                            ),
                            border = null
                        )
                    }

                    Text(
                        text = task.fileName.ifEmpty { task.title },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Player")
                }
            }

            // ExoPlayer Video / Audio Surface Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (task.mediaType == MediaType.AUDIO) 2.2f else 16f / 9f),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                player = exoPlayer
                                useController = true
                                setShowNextButton(false)
                                setShowPreviousButton(false)
                            }
                        }
                    )
                }
            }

            // Action Buttons: Share & Open With External App
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
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
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }

                Button(
                    onClick = {
                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            val mime = if (task.mediaType == MediaType.VIDEO) "video/*" else if (task.mediaType == MediaType.AUDIO) "audio/*" else "*/*"
                            val uri = if (task.filePath.startsWith("content://")) {
                                Uri.parse(task.filePath)
                            } else {
                                val file = File(task.filePath)
                                runCatching {
                                    FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                }.getOrNull() ?: Uri.fromFile(file)
                            }
                            setDataAndType(uri, mime)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching { context.startActivity(viewIntent) }
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open App")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
