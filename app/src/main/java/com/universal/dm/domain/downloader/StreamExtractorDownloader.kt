package com.universal.dm.domain.downloader

import android.util.Log
import com.universal.dm.data.local.DownloadDao
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.data.model.MediaType
import com.universal.dm.domain.sniffer.MediaSizeCalculator
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.roundToLong

class StreamExtractorDownloader(
    private val downloadDao: DownloadDao
) {

    private data class ProgressSnapshot(
        val percent: Int,
        val speedBytesPerSec: Long,
        val isMuxing: Boolean,
        val parsedStreamBytes: Long
    )

    suspend fun downloadStream(
        taskId: String,
        url: String,
        outputDirectory: File,
        mediaType: MediaType,
        qualityLabel: String,
        codecLabel: String,
        formatId: String? = null,
        threadCount: Int,
        referer: String? = null,
        onProgress: suspend (progressPercent: Int, speedBytesPerSec: Long, isMuxing: Boolean) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (!outputDirectory.exists()) outputDirectory.mkdirs()

        // Create a unique temporary subfolder per task so finished artifacts are isolated cleanly
        val taskOutputDir = File(outputDirectory, "task_$taskId").apply { if (!exists()) mkdirs() }

        val request = YoutubeDLRequest(url)
        request.addOption("-o", "${taskOutputDir.absolutePath}/%(title).100B.%(ext)s")
        request.addOption("--no-check-certificates")
        request.addOption("--no-warnings")
        request.addOption("--no-playlist")
        request.addOption("--concurrent-fragments", threadCount.coerceIn(1, 16))
        request.addOption("--retries", "10")
        request.addOption("--fragment-retries", "10")
        request.addOption("--geo-bypass")

        val isYouTube = url.contains("youtube.com", ignoreCase = true) ||
            url.contains("youtu.be", ignoreCase = true)

        if (!isYouTube) {
            request.addOption(
                "--user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
            )
        }

        if (!referer.isNullOrBlank() && !referer.equals(url, ignoreCase = true)) {
            request.addOption("--referer", referer)
        }

        val height = NUMBER_REGEX.find(qualityLabel)?.value?.toIntOrNull() ?: 1080

        if (mediaType == MediaType.VIDEO) {
            if (isYouTube) {
                val formatIdPrefix = if (!formatId.isNullOrBlank() &&
                    formatId != "direct" && formatId != "video" && formatId != "hls" && formatId != "stream"
                ) {
                    "$formatId+bestaudio/$formatId/"
                } else ""

                when {
                    codecLabel.contains("AV1", ignoreCase = true) -> {
                        request.addOption(
                            "-f",
                            "${formatIdPrefix}bestvideo[height<=$height][vcodec^=av01]+bestaudio[ext=m4a]/bestvideo[height<=$height][vcodec^=av01]+bestaudio/bestvideo[height<=$height]+bestaudio/best[height<=$height]/bestvideo[height<=$height]/best"
                        )
                        request.addOption("--merge-output-format", "mp4")
                    }
                    codecLabel.contains("VP9", ignoreCase = true) -> {
                        request.addOption(
                            "-f",
                            "${formatIdPrefix}bestvideo[height<=$height][vcodec^=vp]+bestaudio[ext=m4a]/bestvideo[height<=$height][vcodec^=vp]+bestaudio/bestvideo[height<=$height]+bestaudio/best[height<=$height]/bestvideo[height<=$height]/best"
                        )
                        request.addOption("--merge-output-format", "mkv")
                    }
                    else -> {
                        request.addOption(
                            "-f",
                            "${formatIdPrefix}bestvideo[height<=$height][vcodec^=avc1]+bestaudio[ext=m4a]/bestvideo[height<=$height]+bestaudio[ext=m4a]/bestvideo[height<=$height]+bestaudio/best[height<=$height]/bestvideo[height<=$height]/best"
                        )
                        request.addOption("--merge-output-format", "mp4")
                    }
                }
            } else {
                val formatSpec = when {
                    !formatId.isNullOrBlank() && formatId != "direct" && formatId != "video" && formatId != "hls" && formatId != "stream" -> {
                        "$formatId/best[height<=$height]/bestvideo[height<=$height]+bestaudio/best/bestvideo+bestaudio"
                    }
                    else -> {
                        "best[height<=$height]/bestvideo[height<=$height]+bestaudio/best/bestvideo+bestaudio"
                    }
                }
                request.addOption("-f", formatSpec)
                request.addOption("--merge-output-format", "mp4")
            }
        } else {
            val targetBitrateKbps = NUMBER_REGEX.find(qualityLabel)?.value?.toIntOrNull() ?: 256
            val bitrateStr = "${targetBitrateKbps}k"

            request.addOption("-f", "bestaudio/best")
            request.addOption("-x")

            when {
                codecLabel.contains("M4A", ignoreCase = true) -> {
                    request.addOption("--audio-format", "m4a")
                    request.addOption("--audio-quality", bitrateStr)
                }
                codecLabel.contains("Opus", ignoreCase = true) -> {
                    request.addOption("--audio-format", "opus")
                    request.addOption("--audio-quality", bitrateStr)
                }
                codecLabel.contains("FLAC", ignoreCase = true) -> {
                    request.addOption("--audio-format", "flac")
                    request.addOption("--audio-quality", "0")
                }
                codecLabel.contains("WAV", ignoreCase = true) -> {
                    request.addOption("--audio-format", "wav")
                }
                else -> {
                    request.addOption("--audio-format", "mp3")
                    request.addOption("--audio-quality", bitrateStr)
                }
            }
        }

        // Pre-fetch initial expected size once before launching process (eliminates per-tick SQLite SELECTs)
        val initialTotal = downloadDao.getTaskById(taskId)?.totalBytes ?: 0L
        val startTime = System.currentTimeMillis()
        var lastOutputLine = ""

        com.universal.dm.UdmApplication.ensureYoutubeDlInitialized(com.universal.dm.UdmApplication.instance)

        val cancelHandle = currentCoroutineContext()[Job]?.invokeOnCompletion {
            runCatching { YoutubeDL.getInstance().destroyProcessById(taskId) }
        }

        try {
            coroutineScope {
                val progressChannel = Channel<ProgressSnapshot>(Channel.CONFLATED)

                val consumerJob = launch {
                    var maxKnownTotal = initialTotal
                    var maxDownloadedBytes = 0L
                    var emaSpeed = 0L

                    for (snapshot in progressChannel) {
                        val effectiveSpeed = if (snapshot.speedBytesPerSec > 0L) {
                            emaSpeed = MediaSizeCalculator.smoothSpeedEma(emaSpeed, snapshot.speedBytesPerSec)
                            emaSpeed
                        } else if (snapshot.isMuxing) {
                            0L
                        } else {
                            emaSpeed
                        }

                        if (snapshot.parsedStreamBytes > maxKnownTotal) {
                            maxKnownTotal = snapshot.parsedStreamBytes
                        }

                        val status = if (snapshot.isMuxing) DownloadStatus.MERGING_MUXING else DownloadStatus.DOWNLOADING
                        val estimatedDownloaded = if (maxKnownTotal > 0L) {
                            ((maxKnownTotal.toDouble() * snapshot.percent) / 100.0).roundToLong()
                        } else {
                            (snapshot.percent * 500_000L)
                        }
                        if (estimatedDownloaded > maxDownloadedBytes && !snapshot.isMuxing) {
                            maxDownloadedBytes = estimatedDownloaded
                        }

                        onProgress(snapshot.percent, effectiveSpeed, snapshot.isMuxing)
                        downloadDao.updateTaskProgress(
                            taskId = taskId,
                            downloadedBytes = maxDownloadedBytes.coerceAtMost(
                                if (maxKnownTotal > 0L) maxKnownTotal else Long.MAX_VALUE
                            ),
                            totalBytes = maxKnownTotal,
                            speedBytesPerSec = effectiveSpeed,
                            status = status
                        )
                    }
                }

                var isMuxing = false
                var lastEmitNanos = 0L

                try {
                    YoutubeDL.getInstance().execute(request, taskId) { progress, _, line ->
                        lastOutputLine = line
                        Log.d(TAG, "yt-dlp [$taskId]: $line")

                        // Trigger muxing ONLY on actual FFmpeg / post-processor stages (never on initial [download] Destination:)
                        if (line.contains("[Merger]", ignoreCase = true) ||
                            line.contains("Merging formats", ignoreCase = true) ||
                            line.contains("[ffmpeg]", ignoreCase = true) ||
                            line.contains("[ExtractAudio]", ignoreCase = true) ||
                            line.contains("[VideoRemuxer]", ignoreCase = true) ||
                            line.contains("[Fixup", ignoreCase = true) ||
                            line.contains("Post-process", ignoreCase = true) ||
                            line.contains("Deleting original file", ignoreCase = true)
                        ) {
                            isMuxing = true
                        }

                        val progressPercent = progress.toInt().coerceIn(0, 100)
                        val nowNanos = System.nanoTime()
                        if (nowNanos - lastEmitNanos >= CALLBACK_THROTTLE_NANOS || progressPercent == 100 || isMuxing) {
                            lastEmitNanos = nowNanos
                            val speedBytes = parseSpeedBytesPerSec(line)
                            val parsedStreamBytes = parseStreamSizeBytes(line)
                            progressChannel.trySend(
                                ProgressSnapshot(
                                    percent = progressPercent,
                                    speedBytesPerSec = speedBytes,
                                    isMuxing = isMuxing,
                                    parsedStreamBytes = parsedStreamBytes
                                )
                            )
                        }
                    }
                } finally {
                    progressChannel.close()
                    consumerJob.join()
                }
            }
        } finally {
            cancelHandle?.dispose()
        }

        // Search taskOutputDir for the generated file (ignoring temporary .part / .ytdl files)
        val files = taskOutputDir.listFiles()?.filter {
            it.isFile && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") && !it.name.endsWith(".temp")
        }?.sortedByDescending { it.length() }

        val finishedFile = files?.firstOrNull() ?: run {
            outputDirectory.listFiles()?.filter {
                it.isFile && it.lastModified() >= startTime - 5000 &&
                    !it.name.endsWith(".part") && !it.name.endsWith(".ytdl")
            }?.maxByOrNull { it.lastModified() }
        }

        if (finishedFile == null || !finishedFile.exists()) {
            throw IllegalStateException(
                "Download finished but final media file was not found" +
                    if (lastOutputLine.isNotBlank()) " (yt-dlp output: $lastOutputLine)" else ""
            )
        }

        Log.d(TAG, "Successfully produced media file: ${finishedFile.absolutePath} (${finishedFile.length()} bytes)")
        finishedFile
    }

    private fun parseSpeedBytesPerSec(line: String): Long {
        val matcher = SPEED_PATTERN.matcher(line)
        if (!matcher.find()) return 0L
        val value = matcher.group(1)?.toDoubleOrNull() ?: return 0L
        val unit = matcher.group(2)?.uppercase(Locale.ROOT) ?: return 0L
        return when (unit) {
            "GIB/S" -> (value * 1024.0 * 1024.0 * 1024.0).roundToLong()
            "GB/S" -> (value * 1_000_000_000.0).roundToLong()
            "MIB/S" -> (value * 1024.0 * 1024.0).roundToLong()
            "MB/S" -> (value * 1_000_000.0).roundToLong()
            "KIB/S" -> (value * 1024.0).roundToLong()
            "KB/S" -> (value * 1_000.0).roundToLong()
            else -> value.roundToLong()
        }
    }

    private fun parseStreamSizeBytes(line: String): Long {
        val matcher = SIZE_OF_PATTERN.matcher(line)
        if (!matcher.find()) return 0L
        val value = matcher.group(1)?.toDoubleOrNull() ?: return 0L
        val unit = matcher.group(2)?.uppercase(Locale.ROOT) ?: return 0L
        return when (unit) {
            "GIB" -> (value * 1024.0 * 1024.0 * 1024.0).roundToLong()
            "GB" -> (value * 1_000_000_000.0).roundToLong()
            "MIB" -> (value * 1024.0 * 1024.0).roundToLong()
            "MB" -> (value * 1_000_000.0).roundToLong()
            "KIB" -> (value * 1024.0).roundToLong()
            "KB" -> (value * 1_000.0).roundToLong()
            else -> value.roundToLong()
        }
    }

    companion object {
        private const val TAG = "StreamDownloader"
        private const val CALLBACK_THROTTLE_NANOS = 400_000_000L
        private val NUMBER_REGEX = Regex("\\d+")
        private val SPEED_PATTERN = Pattern.compile("([0-9.]+)\\s*(GiB/s|GB/s|MiB/s|MB/s|KiB/s|KB/s|B/s)", Pattern.CASE_INSENSITIVE)
        private val SIZE_OF_PATTERN = Pattern.compile("of\\s*~?\\s*([0-9.]+)\\s*(GiB|GB|MiB|MB|KiB|KB|B)", Pattern.CASE_INSENSITIVE)
    }
}
