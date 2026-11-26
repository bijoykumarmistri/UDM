package com.universal.dm.domain.downloader

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.universal.dm.UdmApplication
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
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

        val tempRootDir = File(applicationContext.cacheDir, "downloads").apply { if (!exists()) mkdirs() }
        // Per-task isolated workspace prevents filename collisions and enables clean recursive cleanup
        val taskTempDir = File(tempRootDir, "task_$taskId").apply { if (!exists()) mkdirs() }
        val safeTempFileName = DownloadEngineHelper.sanitizeFileName(task.fileName, "download_$taskId.bin")
        val tempFile = File(taskTempDir, safeTempFileName)

        var shouldPreserveTempForResume = false

        try {
            val isYouTube = task.sourcePageUrl.contains("youtube.com", ignoreCase = true) ||
                task.sourcePageUrl.contains("youtu.be", ignoreCase = true) ||
                task.downloadUrl.contains("youtube.com", ignoreCase = true) ||
                task.downloadUrl.contains("youtu.be", ignoreCase = true)

            val isHlsOrDash = task.downloadUrl.contains(".m3u8", ignoreCase = true) ||
                task.downloadUrl.contains(".mpd", ignoreCase = true)

            val hasSeparateAudio = !task.pairedAudioUrl.isNullOrBlank()

            val isDirectDownload = !isYouTube && !isHlsOrDash && (
                task.mediaType == MediaType.DIRECT_FILE ||
                    task.formatId == "direct" ||
                    !hasSeparateAudio
                )

            val streamTargetUrl = if (isYouTube) {
                task.sourcePageUrl.ifEmpty { task.downloadUrl }
            } else {
                task.downloadUrl.ifEmpty { task.sourcePageUrl }
            }

            val refererUrl = task.sourcePageUrl.takeIf { it.isNotBlank() }

            val completedFile: File = if (isDirectDownload) {
                try {
                    httpChunkDownloader.downloadDirectHttp(
                        taskId = taskId,
                        url = task.downloadUrl,
                        targetFile = tempFile,
                        requestedThreadCount = task.threadCount,
                        referer = refererUrl
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
                } catch (directEx: Exception) {
                    if (isStopped) throw directEx
                    Log.w(TAG, "Direct download failed, attempting stream extractor fallback", directEx)
                    streamExtractorDownloader.downloadStream(
                        taskId = taskId,
                        url = streamTargetUrl,
                        outputDirectory = tempRootDir,
                        mediaType = task.mediaType,
                        qualityLabel = task.qualityLabel,
                        codecLabel = task.codecLabel,
                        formatId = task.formatId,
                        threadCount = task.threadCount,
                        referer = refererUrl
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
            } else {
                try {
                    streamExtractorDownloader.downloadStream(
                        taskId = taskId,
                        url = streamTargetUrl,
                        outputDirectory = tempRootDir,
                        mediaType = task.mediaType,
                        qualityLabel = task.qualityLabel,
                        codecLabel = task.codecLabel,
                        formatId = task.formatId,
                        threadCount = task.threadCount,
                        referer = refererUrl
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
                } catch (streamEx: Exception) {
                    if (isStopped) throw streamEx
                    if (!isYouTube && task.downloadUrl.startsWith("http", ignoreCase = true) && !isHlsOrDash) {
                        Log.w(TAG, "Stream extractor failed, falling back to direct HTTP chunk download", streamEx)
                        httpChunkDownloader.downloadDirectHttp(
                            taskId = taskId,
                            url = task.downloadUrl,
                            targetFile = tempFile,
                            requestedThreadCount = task.threadCount,
                            referer = refererUrl
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
                        throw streamEx
                    }
                }
            }

            // Capture exact byte length BEFORE saveFileToPublicDownloads moves & deletes sourceFile!
            val actualCompletedBytes = completedFile.length()

            val fileExt = (completedFile.extension.ifEmpty { task.fileName.substringAfterLast('.', "") }).lowercase()
            val resolvedMime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExt)
            val mimeType = when (task.mediaType) {
                MediaType.VIDEO -> when (fileExt) {
                    "mkv" -> "video/x-matroska"
                    "webm" -> "video/webm"
                    "avi" -> "video/x-msvideo"
                    else -> resolvedMime ?: "video/mp4"
                }
                MediaType.AUDIO -> when (fileExt) {
                    "mp3" -> "audio/mpeg"
                    "opus", "ogg" -> "audio/ogg"
                    "flac" -> "audio/flac"
                    "wav" -> "audio/wav"
                    else -> resolvedMime ?: "audio/mp4"
                }
                MediaType.DIRECT_FILE -> when (fileExt) {
                    "apk" -> "application/vnd.android.package-archive"
                    "pdf" -> "application/pdf"
                    "zip" -> "application/zip"
                    "tar" -> "application/x-tar"
                    "gz" -> "application/gzip"
                    "7z" -> "application/x-7z-compressed"
                    "rar" -> "application/vnd.rar"
                    else -> resolvedMime ?: "application/octet-stream"
                }
            }

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

            // Fetch latest task row so progress fields stay accurate, then mark COMPLETED
            val latestTask = downloadDao.getTaskById(taskId) ?: task
            val resolvedTotalBytes = if (actualCompletedBytes > 0L) actualCompletedBytes else latestTask.totalBytes
            val updatedTask = latestTask.copy(
                status = DownloadStatus.COMPLETED,
                filePath = finalPublicPath,
                fileName = finalFileName,
                downloadedBytes = resolvedTotalBytes,
                totalBytes = resolvedTotalBytes,
                speedBytesPerSec = 0L,
                errorMessage = null
            )
            downloadDao.updateTask(updatedTask)
            downloadDao.deleteChunksForTask(taskId)

            val prefs = applicationContext.getSharedPreferences("udm_app_settings", Context.MODE_PRIVATE)
            val soundEnabled = prefs.getBoolean("key_sound_completion", true)
            DownloadEngineHelper.notifyDownloadCompleted(
                context = applicationContext,
                notificationId = taskId.hashCode(),
                title = finalFileName,
                soundEnabled = soundEnabled
            )

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for task $taskId (isStopped=$isStopped)", e)
            withContext(NonCancellable) {
                if (isStopped) {
                    // Preserve temp chunks so multi-part HTTP resume continues seamlessly
                    shouldPreserveTempForResume = true
                    downloadDao.updateTaskStatus(taskId, DownloadStatus.PAUSED, "Paused by user")
                } else {
                    downloadDao.updateTaskStatus(taskId, DownloadStatus.FAILED, e.message ?: "Unknown error")
                }
            }
            Result.failure()
        } finally {
            if (!shouldPreserveTempForResume) {
                runCatching {
                    if (taskTempDir.exists()) taskTempDir.deleteRecursively()
                }
            }
        }
    }

    companion object {
        private const val TAG = "UdmDownloadWorker"
        const val KEY_TASK_ID = "TASK_ID"
    }
}
