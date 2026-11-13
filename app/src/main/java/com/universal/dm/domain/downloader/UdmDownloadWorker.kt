package com.universal.dm.domain.downloader

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.universal.dm.UdmApplication
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UdmDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val database = UdmApplication.getDatabase(appContext)
    private val downloadDao = database.downloadDao()

    private val httpChunkDownloader = HttpChunkDownloader(downloadDao)
    private val streamExtractorDownloader = StreamExtractorDownloader(downloadDao)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return@withContext Result.failure()
        val task = downloadDao.getTaskById(taskId) ?: return@withContext Result.failure()

        val notificationId = taskId.hashCode()

        // 1. Establish as Foreground Service immediately with progress notification
        try {
            setForeground(
                DownloadEngineHelper.createForegroundInfo(
                    context = applicationContext,
                    notificationId = notificationId,
                    title = task.title,
                    progressPercent = 0,
                    speedBytesPerSec = 0L,
                    isIndeterminate = true
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Unable to set foreground info immediately", e)
        }

        downloadDao.updateTaskStatus(taskId, DownloadStatus.DOWNLOADING)

        val tempDir = File(applicationContext.cacheDir, "downloads").apply { if (!exists()) mkdirs() }
        val tempFile = File(tempDir, task.fileName)

        try {
            val isDirectDownload = task.mediaType == MediaType.DIRECT_FILE ||
                    task.downloadUrl.endsWith(".zip", ignoreCase = true) ||
                    task.downloadUrl.endsWith(".apk", ignoreCase = true) ||
                    task.downloadUrl.endsWith(".pdf", ignoreCase = true) ||
                    task.downloadUrl.endsWith(".iso", ignoreCase = true) ||
                    task.downloadUrl.endsWith(".tar", ignoreCase = true) ||
                    task.downloadUrl.endsWith(".gz", ignoreCase = true)

            val completedFile: File = if (isDirectDownload) {
                // ENGINE A: Direct HTTP Multi-Threaded Chunk Downloader (OkHttp)
                httpChunkDownloader.downloadDirectHttp(
                    taskId = taskId,
                    url = task.downloadUrl,
                    targetFile = tempFile,
                    requestedThreadCount = task.threadCount
                ) { downloaded, total, speed ->
                    val percent = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0
                    runCatching {
                        setForeground(
                            DownloadEngineHelper.createForegroundInfo(
                                context = applicationContext,
                                notificationId = notificationId,
                                title = task.title,
                                progressPercent = percent,
                                speedBytesPerSec = speed
                            )
                        )
                    }
                }
            } else {
                // ENGINE B: YoutubeDL + FFmpeg Stream Extraction & Merging
                streamExtractorDownloader.downloadStream(
                    taskId = taskId,
                    url = task.sourcePageUrl.ifEmpty { task.downloadUrl },
                    outputDirectory = tempDir,
                    mediaType = task.mediaType,
                    qualityLabel = task.qualityLabel,
                    codecLabel = task.codecLabel,
                    threadCount = task.threadCount
                ) { percent, speed, isMuxing ->
                    runCatching {
                        setForeground(
                            DownloadEngineHelper.createForegroundInfo(
                                context = applicationContext,
                                notificationId = notificationId,
                                title = task.title,
                                progressPercent = percent,
                                speedBytesPerSec = speed,
                                isMuxing = isMuxing
                            )
                        )
                    }
                }
            }

            // Determine exact MIME type & extension from the actual finished file
            val fileExt = (completedFile.extension.ifEmpty { task.fileName.substringAfterLast('.', "") }).lowercase()
            val mimeType = when (task.mediaType) {
                MediaType.VIDEO -> when (fileExt) {
                    "mkv" -> "video/x-matroska"
                    "webm" -> "video/webm"
                    "avi" -> "video/x-msvideo"
                    else -> "video/mp4"
                }
                MediaType.AUDIO -> when (fileExt) {
                    "mp3" -> "audio/mpeg"
                    "opus", "ogg" -> "audio/ogg"
                    "flac" -> "audio/flac"
                    "wav" -> "audio/wav"
                    else -> "audio/mp4"
                }
                MediaType.DIRECT_FILE -> "application/octet-stream"
            }

            // Ensure task.fileName has the correct extension matching the actual completed file
            val finalFileName = if (fileExt.isNotEmpty() && !task.fileName.endsWith(".$fileExt", ignoreCase = true)) {
                "${task.fileName.substringBeforeLast('.')}.$fileExt"
            } else {
                task.fileName
            }

            val finalPublicPath = DownloadEngineHelper.saveFileToPublicDownloads(
                context = applicationContext,
                sourceFile = completedFile,
                fileName = finalFileName,
                mimeType = mimeType
            )

            // Update database task state to COMPLETED
            val finalBytes = if (completedFile.exists()) completedFile.length() else task.totalBytes
            val updatedTask = task.copy(
                status = DownloadStatus.COMPLETED,
                filePath = finalPublicPath,
                fileName = finalFileName,
                downloadedBytes = if (finalBytes > 0) finalBytes else task.downloadedBytes,
                totalBytes = if (finalBytes > 0) finalBytes else task.totalBytes,
                speedBytesPerSec = 0L
            )
            downloadDao.updateTask(updatedTask)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for task $taskId", e)
            if (isStopped) {
                downloadDao.updateTaskStatus(taskId, DownloadStatus.PAUSED, "Paused by user")
            } else {
                downloadDao.updateTaskStatus(taskId, DownloadStatus.FAILED, e.message ?: "Unknown error")
            }
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "UdmDownloadWorker"
        const val KEY_TASK_ID = "TASK_ID"
    }
}
