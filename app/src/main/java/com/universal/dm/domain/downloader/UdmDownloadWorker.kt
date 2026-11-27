package com.universal.dm.domain.downloader

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.universal.dm.UdmApplication
import com.universal.dm.core.NetworkClient
import com.universal.dm.data.model.AudioCodec
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.data.model.MediaType
import com.universal.dm.domain.sniffer.MediaSniffer
import com.universal.dm.ui.settings.AppSettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class UdmDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val database = UdmApplication.getDatabase(appContext)
    private val downloadDao = database.downloadDao()

    private val httpChunkDownloader = HttpChunkDownloader(downloadDao)
    private val streamExtractorDownloader = StreamExtractorDownloader(downloadDao)
    private val lastForegroundUpdateNanos = AtomicLong(0L)

    private suspend fun updateForegroundThrottled(
        notificationId: Int,
        title: String,
        progressPercent: Int,
        speedBytesPerSec: Long,
        isMuxing: Boolean = false,
        force: Boolean = false
    ) {
        val now = System.nanoTime()
        val prev = lastForegroundUpdateNanos.get()
        if (!force && now - prev < NOTIFICATION_THROTTLE_NANOS) return
        if (!force && !lastForegroundUpdateNanos.compareAndSet(prev, now)) return
        if (force) lastForegroundUpdateNanos.set(now)
        runCatching {
            setForeground(
                DownloadEngineHelper.createForegroundInfo(
                    context = applicationContext,
                    notificationId = notificationId,
                    title = title,
                    progressPercent = progressPercent,
                    speedBytesPerSec = speedBytesPerSec,
                    isMuxing = isMuxing
                )
            )
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val taskId = inputData.getString(KEY_TASK_ID) ?: "udm_task"
        return DownloadEngineHelper.createForegroundInfo(
            context = applicationContext,
            notificationId = taskId.hashCode(),
            title = "Preparing download…",
            progressPercent = 0,
            speedBytesPerSec = 0L,
            isIndeterminate = true
        )
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return@withContext Result.failure()
        val task = downloadDao.getTaskById(taskId) ?: return@withContext Result.failure()

        val notificationId = taskId.hashCode()
        val prefs = applicationContext.getSharedPreferences(AppSettingsState.PREFS_NAME, Context.MODE_PRIVATE)
        val embedAudioThumbnail = prefs.getBoolean(AppSettingsState.KEY_EMBED_AUDIO_THUMBNAIL, true)

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

            val mediaSniffer = MediaSniffer(NetworkClient.client)
            val youtubeVideoId = mediaSniffer.getYoutubeVideoId(task.sourcePageUrl)
                ?: mediaSniffer.getYoutubeVideoId(task.downloadUrl)

            var effectiveDownloadUrl = task.downloadUrl
            var effectivePairedAudioUrl = task.pairedAudioUrl
            var effectiveThumbnailUrl = task.thumbnailUrl
            var effectiveFormatId = task.formatId

            // If user enqueued a YouTube page URL manually (e.g. + Add URL), resolve direct CDN streams
            // via fast InnerTube API (~300ms) instead of waiting 15-25s for Python/yt-dlp extraction.
            if (isYouTube && task.mediaType != MediaType.DIRECT_FILE &&
                effectiveDownloadUrl.equals(task.sourcePageUrl, ignoreCase = true)
            ) {
                val fastResolved = runCatching {
                    mediaSniffer.resolveYoutubeFast(task.sourcePageUrl, task.title)
                }.getOrNull()

                if (fastResolved != null) {
                    if (effectiveThumbnailUrl.isNullOrBlank()) {
                        effectiveThumbnailUrl = fastResolved.thumbnailUrl
                    }
                    if (task.mediaType == MediaType.AUDIO) {
                        val pickedAudio = fastResolved.audioVariants
                            .firstOrNull { it.audioCodec == AudioCodec.M4A }
                            ?: fastResolved.audioVariants.firstOrNull()
                        if (pickedAudio != null) {
                            effectiveDownloadUrl = pickedAudio.url
                            effectiveFormatId = pickedAudio.formatId
                        }
                    } else {
                        val targetHeight = NUMBER_REGEX.find(task.qualityLabel)?.value?.toIntOrNull() ?: 1080
                        val pickedVideo = fastResolved.videoVariants
                            .filter { it.resolutionHeight <= targetHeight }
                            .maxByOrNull { it.resolutionHeight }
                            ?: fastResolved.videoVariants.lastOrNull()
                        if (pickedVideo != null) {
                            effectiveDownloadUrl = pickedVideo.url
                            effectivePairedAudioUrl = pickedVideo.pairedAudioStreamUrl
                            effectiveFormatId = pickedVideo.formatId
                        }
                    }
                }
            }

            val isHlsOrDash = effectiveDownloadUrl.contains(".m3u8", ignoreCase = true) ||
                effectiveDownloadUrl.contains(".mpd", ignoreCase = true)

            val hasDirectCdnStream = effectiveDownloadUrl.startsWith("http", ignoreCase = true) &&
                !isHlsOrDash &&
                (!isYouTube || !effectiveDownloadUrl.equals(task.sourcePageUrl, ignoreCase = true))

            val streamTargetUrl = if (isYouTube) {
                task.sourcePageUrl.ifEmpty { task.downloadUrl }
            } else {
                task.downloadUrl.ifEmpty { task.sourcePageUrl }
            }

            val isGoogleVideoCdn = effectiveDownloadUrl.contains("googlevideo.com", ignoreCase = true)
            val refererUrl = if (isGoogleVideoCdn) null else task.sourcePageUrl.takeIf { it.isNotBlank() }
            val cdnUserAgent = MediaSniffer.resolveYoutubeCdnUserAgent(effectiveDownloadUrl)
            val effectiveDirectThreads = if (isGoogleVideoCdn) task.threadCount.coerceAtMost(4) else task.threadCount

            val completedFile: File = when (task.mediaType) {
                MediaType.DIRECT_FILE -> {
                    if (hasDirectCdnStream) {
                        try {
                            httpChunkDownloader.downloadDirectHttp(
                                taskId = taskId,
                                url = effectiveDownloadUrl,
                                targetFile = tempFile,
                                requestedThreadCount = effectiveDirectThreads,
                                referer = refererUrl,
                                userAgent = cdnUserAgent
                            ) { downloaded, total, speed ->
                                val percent = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0
                                updateForegroundThrottled(
                                    notificationId = notificationId,
                                    title = task.title,
                                    progressPercent = percent,
                                    speedBytesPerSec = speed
                                )
                            }
                        } catch (directEx: Exception) {
                            if (isStopped) throw directEx
                            Log.w(TAG, "Direct file download failed, falling back to stream extractor", directEx)
                            runStreamExtractorFallback(
                                taskId = taskId,
                                url = streamTargetUrl,
                                tempRootDir = tempRootDir,
                                task = task,
                                formatId = effectiveFormatId,
                                referer = task.sourcePageUrl.takeIf { it.isNotBlank() },
                                embedThumbnail = embedAudioThumbnail,
                                notificationId = notificationId
                            )
                        }
                    } else {
                        runStreamExtractorFallback(
                            taskId = taskId,
                            url = streamTargetUrl,
                            tempRootDir = tempRootDir,
                            task = task,
                            formatId = effectiveFormatId,
                            referer = task.sourcePageUrl.takeIf { it.isNotBlank() },
                            embedThumbnail = embedAudioThumbnail,
                            notificationId = notificationId
                        )
                    }
                }

                MediaType.VIDEO -> {
                    if (hasDirectCdnStream) {
                        try {
                            if (!effectivePairedAudioUrl.isNullOrBlank()) {
                                downloadAndMuxPairedVideo(
                                    taskId = taskId,
                                    taskTitle = task.title,
                                    videoUrl = effectiveDownloadUrl,
                                    audioUrl = effectivePairedAudioUrl,
                                    taskTempDir = taskTempDir,
                                    outputFile = tempFile,
                                    threadCount = task.threadCount,
                                    referer = refererUrl,
                                    notificationId = notificationId
                                )
                            } else {
                                httpChunkDownloader.downloadDirectHttp(
                                    taskId = taskId,
                                    url = effectiveDownloadUrl,
                                    targetFile = tempFile,
                                    requestedThreadCount = effectiveDirectThreads,
                                    referer = refererUrl,
                                    userAgent = cdnUserAgent
                                ) { downloaded, total, speed ->
                                    val percent = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0
                                    updateForegroundThrottled(
                                        notificationId = notificationId,
                                        title = task.title,
                                        progressPercent = percent,
                                        speedBytesPerSec = speed
                                    )
                                }
                            }
                        } catch (fastEx: Exception) {
                            if (isStopped) throw fastEx
                            Log.w(TAG, "Fast direct video download failed, falling back to yt-dlp", fastEx)
                            runStreamExtractorFallback(
                                taskId = taskId,
                                url = streamTargetUrl,
                                tempRootDir = tempRootDir,
                                task = task,
                                formatId = effectiveFormatId,
                                referer = task.sourcePageUrl.takeIf { it.isNotBlank() },
                                embedThumbnail = embedAudioThumbnail,
                                notificationId = notificationId
                            )
                        }
                    } else {
                        runStreamExtractorFallback(
                            taskId = taskId,
                            url = streamTargetUrl,
                            tempRootDir = tempRootDir,
                            task = task,
                            formatId = effectiveFormatId,
                            referer = task.sourcePageUrl.takeIf { it.isNotBlank() },
                            embedThumbnail = embedAudioThumbnail,
                            notificationId = notificationId
                        )
                    }
                }

                MediaType.AUDIO -> {
                    if (hasDirectCdnStream) {
                        try {
                            downloadAndPackageAudio(
                                taskId = taskId,
                                taskTitle = task.title,
                                audioUrl = effectiveDownloadUrl,
                                thumbnailUrl = effectiveThumbnailUrl,
                                youtubeVideoId = youtubeVideoId,
                                taskTempDir = taskTempDir,
                                targetOutputFile = tempFile,
                                qualityLabel = task.qualityLabel,
                                codecLabel = task.codecLabel,
                                threadCount = task.threadCount,
                                referer = refererUrl,
                                embedThumbnail = embedAudioThumbnail,
                                notificationId = notificationId
                            )
                        } catch (fastAudioEx: Exception) {
                            if (isStopped) throw fastAudioEx
                            Log.w(TAG, "Fast direct audio download failed, falling back to yt-dlp", fastAudioEx)
                            runStreamExtractorFallback(
                                taskId = taskId,
                                url = streamTargetUrl,
                                tempRootDir = tempRootDir,
                                task = task,
                                formatId = effectiveFormatId,
                                referer = task.sourcePageUrl.takeIf { it.isNotBlank() },
                                embedThumbnail = embedAudioThumbnail,
                                notificationId = notificationId
                            )
                        }
                    } else {
                        runStreamExtractorFallback(
                            taskId = taskId,
                            url = streamTargetUrl,
                            tempRootDir = tempRootDir,
                            task = task,
                            formatId = effectiveFormatId,
                            referer = task.sourcePageUrl.takeIf { it.isNotBlank() },
                            embedThumbnail = embedAudioThumbnail,
                            notificationId = notificationId
                        )
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
                    "m4a" -> "audio/mp4"
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

            val soundEnabled = prefs.getBoolean("key_sound_completion", true)
            DownloadEngineHelper.notifyDownloadCompleted(
                context = applicationContext,
                notificationId = taskId.hashCode(),
                title = finalFileName,
                soundEnabled = soundEnabled
            )

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for task $taskId (isStopped=$isStopped, attempt=$runAttemptCount)", e)
            val autoRetry = prefs.getBoolean(AppSettingsState.KEY_AUTO_RETRY, true)
            val canRetry = !isStopped && autoRetry && runAttemptCount < MAX_AUTO_RETRIES && isTransientFailure(e)

            withContext(NonCancellable) {
                if (isStopped) {
                    // Preserve temp chunks so multi-part HTTP resume continues seamlessly
                    shouldPreserveTempForResume = true
                    downloadDao.updateTaskStatus(taskId, DownloadStatus.PAUSED, "Paused by user")
                } else if (canRetry) {
                    shouldPreserveTempForResume = true
                    downloadDao.updateTaskStatus(
                        taskId,
                        DownloadStatus.QUEUED,
                        "Retrying (${runAttemptCount + 1}/$MAX_AUTO_RETRIES)…"
                    )
                } else {
                    downloadDao.updateTaskStatus(taskId, DownloadStatus.FAILED, e.message ?: "Unknown error")
                }
            }
            if (canRetry) Result.retry() else Result.failure()
        } finally {
            if (!shouldPreserveTempForResume) {
                runCatching {
                    if (taskTempDir.exists()) taskTempDir.deleteRecursively()
                }
            }
        }
    }

    private suspend fun downloadAndMuxPairedVideo(
        taskId: String,
        taskTitle: String,
        videoUrl: String,
        audioUrl: String,
        taskTempDir: File,
        outputFile: File,
        threadCount: Int,
        referer: String?,
        notificationId: Int
    ): File = coroutineScope {
        val videoPartFile = File(taskTempDir, "video_stream.part")
        val audioPartFile = File(taskTempDir, "audio_stream.part")

        val videoDownloaded = AtomicLong(0L)
        val videoTotal = AtomicLong(0L)
        val videoSpeed = AtomicLong(0L)

        val audioDownloaded = AtomicLong(0L)
        val audioTotal = AtomicLong(0L)
        val audioSpeed = AtomicLong(0L)

        val lastDbSyncNanos = AtomicLong(0L)

        suspend fun publishCombinedProgress(force: Boolean = false) {
            val now = System.nanoTime()
            val prev = lastDbSyncNanos.get()
            if (!force && now - prev < 400_000_000L) return
            if (!force && !lastDbSyncNanos.compareAndSet(prev, now)) return

            val totalDone = videoDownloaded.get() + audioDownloaded.get()
            val totalKnown = videoTotal.get() + audioTotal.get()
            val totalRate = videoSpeed.get() + audioSpeed.get()
            val percent = if (totalKnown > 0L) ((totalDone * 100L) / totalKnown).toInt().coerceIn(0, 100) else 0

            downloadDao.updateTaskProgress(
                taskId = taskId,
                downloadedBytes = totalDone,
                totalBytes = totalKnown,
                speedBytesPerSec = totalRate,
                status = DownloadStatus.DOWNLOADING
            )
            runCatching {
                setForeground(
                    DownloadEngineHelper.createForegroundInfo(
                        context = applicationContext,
                        notificationId = notificationId,
                        title = taskTitle,
                        progressPercent = percent,
                        speedBytesPerSec = totalRate
                    )
                )
            }
        }

        val videoUserAgent = MediaSniffer.resolveYoutubeCdnUserAgent(videoUrl)
        val audioUserAgent = MediaSniffer.resolveYoutubeCdnUserAgent(audioUrl)
        val isGoogleVideo = videoUrl.contains("googlevideo.com", ignoreCase = true)
        val videoThreads = if (isGoogleVideo) threadCount.coerceAtMost(4) else threadCount
        val audioThreads = if (audioUrl.contains("googlevideo.com", ignoreCase = true)) threadCount.coerceAtMost(2) else threadCount.coerceAtMost(4)

        val videoJob = async {
            httpChunkDownloader.downloadDirectHttp(
                taskId = taskId,
                url = videoUrl,
                targetFile = videoPartFile,
                requestedThreadCount = videoThreads,
                referer = referer,
                userAgent = videoUserAgent,
                persistChunksInDb = true,
                updateTaskProgressInDb = false
            ) { downloaded, total, speed ->
                videoDownloaded.set(downloaded)
                if (total > 0L) videoTotal.set(total)
                videoSpeed.set(speed)
                publishCombinedProgress()
            }
        }

        val audioJob = async {
            httpChunkDownloader.downloadDirectHttp(
                taskId = taskId,
                url = audioUrl,
                targetFile = audioPartFile,
                requestedThreadCount = audioThreads,
                referer = referer,
                userAgent = audioUserAgent,
                persistChunksInDb = false,
                updateTaskProgressInDb = false
            ) { downloaded, total, speed ->
                audioDownloaded.set(downloaded)
                if (total > 0L) audioTotal.set(total)
                audioSpeed.set(speed)
                publishCombinedProgress()
            }
        }

        videoJob.await()
        audioJob.await()
        publishCombinedProgress(force = true)

        downloadDao.updateTaskStatus(taskId, DownloadStatus.MERGING_MUXING)
        runCatching {
            setForeground(
                DownloadEngineHelper.createForegroundInfo(
                    context = applicationContext,
                    notificationId = notificationId,
                    title = taskTitle,
                    progressPercent = 100,
                    speedBytesPerSec = 0L,
                    isMuxing = true
                )
            )
        }

        val muxed = FfmpegHelper.muxVideoAndAudio(
            context = applicationContext,
            videoFile = videoPartFile,
            audioFile = audioPartFile,
            outputFile = outputFile
        )
        runCatching { videoPartFile.delete() }
        runCatching { audioPartFile.delete() }
        muxed
    }

    private suspend fun downloadAndPackageAudio(
        taskId: String,
        taskTitle: String,
        audioUrl: String,
        thumbnailUrl: String?,
        youtubeVideoId: String?,
        taskTempDir: File,
        targetOutputFile: File,
        qualityLabel: String,
        codecLabel: String,
        threadCount: Int,
        referer: String?,
        embedThumbnail: Boolean,
        notificationId: Int
    ): File = coroutineScope {
        val rawAudioFile = File(taskTempDir, "raw_audio.part")
        val thumbFile = File(taskTempDir, "cover.jpg")

        val thumbDeferred = if (embedThumbnail && (!thumbnailUrl.isNullOrBlank() || !youtubeVideoId.isNullOrBlank())) {
            async {
                FfmpegHelper.downloadThumbnail(
                    okHttpClient = NetworkClient.client,
                    thumbnailUrl = thumbnailUrl,
                    youtubeVideoId = youtubeVideoId,
                    targetFile = thumbFile
                )
            }
        } else null

        val audioUserAgent = MediaSniffer.resolveYoutubeCdnUserAgent(audioUrl)
        val audioThreads = if (audioUrl.contains("googlevideo.com", ignoreCase = true)) threadCount.coerceAtMost(4) else threadCount

        val audioDeferred = async {
            httpChunkDownloader.downloadDirectHttp(
                taskId = taskId,
                url = audioUrl,
                targetFile = rawAudioFile,
                requestedThreadCount = audioThreads,
                referer = referer,
                userAgent = audioUserAgent,
                persistChunksInDb = true,
                updateTaskProgressInDb = true
            ) { downloaded, total, speed ->
                val percent = if (total > 0L) ((downloaded * 100L) / total).toInt().coerceIn(0, 100) else 0
                updateForegroundThrottled(
                    notificationId = notificationId,
                    title = taskTitle,
                    progressPercent = percent,
                    speedBytesPerSec = speed
                )
            }
        }

        audioDeferred.await()
        val downloadedThumb = thumbDeferred?.await()

        val targetExt = when {
            codecLabel.contains("M4A", ignoreCase = true) -> "m4a"
            codecLabel.contains("Opus", ignoreCase = true) -> "opus"
            codecLabel.contains("FLAC", ignoreCase = true) -> "flac"
            codecLabel.contains("WAV", ignoreCase = true) -> "wav"
            else -> targetOutputFile.extension.lowercase().ifBlank { "mp3" }
        }
        val finalOutput = if (targetOutputFile.extension.equals(targetExt, ignoreCase = true)) {
            targetOutputFile
        } else {
            File(taskTempDir, "${targetOutputFile.nameWithoutExtension}.$targetExt")
        }

        val inputIsM4a = audioUrl.contains("mime=audio%2Fmp4", ignoreCase = true) ||
            audioUrl.contains("mime=audio/mp4", ignoreCase = true)

        if (targetExt == "m4a" && inputIsM4a && (!embedThumbnail || downloadedThumb == null)) {
            if (rawAudioFile.renameTo(finalOutput)) {
                return@coroutineScope finalOutput
            }
        }

        val bitrateKbps = NUMBER_REGEX.find(qualityLabel)?.value?.toIntOrNull() ?: 256

        downloadDao.updateTaskStatus(taskId, DownloadStatus.MERGING_MUXING)
        runCatching {
            setForeground(
                DownloadEngineHelper.createForegroundInfo(
                    context = applicationContext,
                    notificationId = notificationId,
                    title = taskTitle,
                    progressPercent = 100,
                    speedBytesPerSec = 0L,
                    isMuxing = true
                )
            )
        }

        val packaged = FfmpegHelper.packageAudioWithThumbnail(
            context = applicationContext,
            inputAudioFile = rawAudioFile,
            thumbnailFile = downloadedThumb,
            outputFile = finalOutput,
            targetExt = targetExt,
            bitrateKbps = bitrateKbps,
            title = taskTitle,
            inputIsM4aContainer = inputIsM4a
        )
        runCatching { rawAudioFile.delete() }
        runCatching { thumbFile.delete() }
        packaged
    }

    private suspend fun runStreamExtractorFallback(
        taskId: String,
        url: String,
        tempRootDir: File,
        task: com.universal.dm.data.local.DownloadTaskEntity,
        formatId: String?,
        referer: String?,
        embedThumbnail: Boolean,
        notificationId: Int
    ): File {
        var muxNotified = false
        return streamExtractorDownloader.downloadStream(
            taskId = taskId,
            url = url,
            outputDirectory = tempRootDir,
            mediaType = task.mediaType,
            qualityLabel = task.qualityLabel,
            codecLabel = task.codecLabel,
            formatId = formatId,
            threadCount = task.threadCount,
            referer = referer,
            embedThumbnail = embedThumbnail
        ) { percent, speed, isMuxing ->
            val shouldForceMux = isMuxing && !muxNotified
            if (shouldForceMux) muxNotified = true
            updateForegroundThrottled(
                notificationId = notificationId,
                title = task.title,
                progressPercent = percent,
                speedBytesPerSec = speed,
                isMuxing = isMuxing,
                force = shouldForceMux
            )
        }
    }

    private fun isTransientFailure(e: Exception): Boolean {
        val msg = e.message.orEmpty()
        if (msg.contains("403") ||
            msg.contains("404") ||
            msg.contains("410") ||
            msg.contains("Unsupported URL", ignoreCase = true) ||
            msg.contains("Video unavailable", ignoreCase = true) ||
            msg.contains("Private video", ignoreCase = true)
        ) {
            return false
        }
        return e is java.io.IOException ||
            msg.contains("timeout", ignoreCase = true) ||
            msg.contains("reset", ignoreCase = true) ||
            msg.contains("unable to resolve", ignoreCase = true) ||
            msg.contains("failed to connect", ignoreCase = true) ||
            msg.contains("network", ignoreCase = true) ||
            msg.contains("socket", ignoreCase = true) ||
            msg.contains("HTTP 5", ignoreCase = true)
    }

    companion object {
        private const val TAG = "UdmDownloadWorker"
        const val KEY_TASK_ID = "TASK_ID"
        private const val MAX_AUTO_RETRIES = 2
        private const val NOTIFICATION_THROTTLE_NANOS = 900_000_000L
        private val NUMBER_REGEX = Regex("\\d+")
    }
}
