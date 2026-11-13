package com.universal.dm.domain.downloader

import android.util.Log
import com.universal.dm.data.local.DownloadDao
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.data.model.MediaType
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

class StreamExtractorDownloader(
    private val downloadDao: DownloadDao
) {

    private val speedMibPattern = Pattern.compile("([0-9.]+)\\s*MiB/s")
    private val speedKibPattern = Pattern.compile("([0-9.]+)\\s*KiB/s")
    private val sizeOfPattern = Pattern.compile("of\\s*~?\\s*([0-9.]+)\\s*(GiB|MiB|KiB|B)", Pattern.CASE_INSENSITIVE)

    suspend fun downloadStream(
        taskId: String,
        url: String,
        outputDirectory: File,
        mediaType: MediaType,
        qualityLabel: String,
        codecLabel: String,
        threadCount: Int,
        onProgress: suspend (progressPercent: Int, speedBytesPerSec: Long, isMuxing: Boolean) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (!outputDirectory.exists()) outputDirectory.mkdirs()

        // Create a unique temporary subfolder per task so we always pinpoint the exact finished file
        val taskOutputDir = File(outputDirectory, "task_$taskId").apply { if (!exists()) mkdirs() }

        val request = YoutubeDLRequest(url)
        // High compatibility flags for modern YouTube and web stream extraction
        request.addOption("-o", "${taskOutputDir.absolutePath}/%(title).100B.%(ext)s")
        request.addOption("--no-check-certificates")
        request.addOption("--no-warnings")
        request.addOption("--no-playlist")
        request.addOption("--concurrent-fragments", threadCount.coerceIn(1, 16))
        request.addOption("--retries", "10")
        request.addOption("--fragment-retries", "10")
        request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")

        // Extract target height (e.g. 2160, 1440, 1080, 720)
        val height = Regex("\\d+").find(qualityLabel)?.value?.toIntOrNull() ?: 1080

        if (mediaType == MediaType.VIDEO) {
            when {
                codecLabel.contains("AV1", ignoreCase = true) -> {
                    request.addOption(
                        "-f",
                        "bestvideo[height<=$height][vcodec^=av01]+bestaudio[ext=m4a]/bestvideo[height<=$height][vcodec^=av01]+bestaudio/bestvideo[height<=$height]+bestaudio/best"
                    )
                    request.addOption("--merge-output-format", "mp4")
                }
                codecLabel.contains("VP9", ignoreCase = true) -> {
                    request.addOption(
                        "-f",
                        "bestvideo[height<=$height][vcodec^=vp]+bestaudio[ext=m4a]/bestvideo[height<=$height][vcodec^=vp]+bestaudio/bestvideo[height<=$height]+bestaudio/best"
                    )
                    request.addOption("--merge-output-format", "mkv")
                }
                else -> {
                    // Standard H.264 Universal
                    request.addOption(
                        "-f",
                        "bestvideo[height<=$height][vcodec^=avc1]+bestaudio[ext=m4a]/bestvideo[height<=$height]+bestaudio[ext=m4a]/bestvideo[height<=$height]+bestaudio/best"
                    )
                    request.addOption("--merge-output-format", "mp4")
                }
            }
        } else {
            // High-Fidelity Audio-only extraction: extract target bitrate (e.g. 320, 256, 128, 64)
            val targetBitrateKbps = Regex("(\\d+)").find(qualityLabel)?.value?.toIntOrNull() ?: 256
            val bitrateStr = "${targetBitrateKbps}k"

            // Best audio source selection: prioritize highest bitrate & frequency response available (Opus itag 251 48kHz or AAC itag 141/140)
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
                    // Default to high quality MP3 at requested target bitrate
                    request.addOption("--audio-format", "mp3")
                    request.addOption("--audio-quality", bitrateStr)
                }
            }
        }

        var isMuxing = false
        var lastDbUpdateTime = 0L

        val startTime = System.currentTimeMillis()

        YoutubeDL.getInstance().execute(request, taskId) { progress, eta, line ->
            // Check if FFmpeg has started merging separate video & audio streams or extracting audio
            if (line.contains("Merging", ignoreCase = true) ||
                line.contains("[Merger]", ignoreCase = true) ||
                line.contains("[ffmpeg]", ignoreCase = true) ||
                line.contains("[ExtractAudio]", ignoreCase = true) ||
                line.contains("Post-process", ignoreCase = true) ||
                line.contains("Destination:", ignoreCase = true)
            ) {
                isMuxing = true
            }

            // Parse speed from command line output
            var speedBytes = 0L
            val mibMatcher = speedMibPattern.matcher(line)
            if (mibMatcher.find()) {
                val mib = mibMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                speedBytes = (mib * 1024 * 1024).toLong()
            } else {
                val kibMatcher = speedKibPattern.matcher(line)
                if (kibMatcher.find()) {
                    val kib = kibMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                    speedBytes = (kib * 1024).toLong()
                }
            }

            // Parse stream size if printed by yt-dlp (e.g. "of 45.2MiB" or "of ~120MiB")
            var parsedStreamBytes = 0L
            val sizeMatcher = sizeOfPattern.matcher(line)
            if (sizeMatcher.find()) {
                val num = sizeMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                val unit = sizeMatcher.group(2)?.uppercase(java.util.Locale.ROOT) ?: ""
                parsedStreamBytes = when (unit) {
                    "GIB" -> (num * 1024 * 1024 * 1024).toLong()
                    "MIB" -> (num * 1024 * 1024).toLong()
                    "KIB" -> (num * 1024).toLong()
                    else -> num.toLong()
                }
            }

            val progressPercent = progress.toInt().coerceIn(0, 100)

            val now = System.currentTimeMillis()
            if (now - lastDbUpdateTime > 400 || progressPercent == 100) {
                lastDbUpdateTime = now
                kotlinx.coroutines.runBlocking {
                    onProgress(progressPercent, speedBytes, isMuxing)
                    val status = if (isMuxing) DownloadStatus.MERGING_MUXING else DownloadStatus.DOWNLOADING
                    val existingTask = downloadDao.getTaskById(taskId)
                    val initialTotal = existingTask?.totalBytes ?: 0L
                    val taskTotal = if (parsedStreamBytes > 0) parsedStreamBytes else initialTotal
                    val downloadedCalc = if (taskTotal > 0) {
                        (taskTotal * progressPercent) / 100L
                    } else {
                        // Fallback: estimate from progress and temporary folder contents
                        (taskOutputDir.listFiles()?.sumOf { it.length() } ?: 0L).coerceAtLeast((progressPercent * 500_000L))
                    }
                    downloadDao.updateTaskProgress(taskId, downloadedCalc, taskTotal, speedBytes, status)
                }
            }
        }

        // Search taskOutputDir for the generated file (ignoring temporary .part / .ytdl files)
        val files = taskOutputDir.listFiles()?.filter {
            it.isFile && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") && !it.name.endsWith(".temp")
        }?.sortedByDescending { it.length() }

        val finishedFile = files?.firstOrNull() ?: run {
            // Fallback search in outputDirectory
            outputDirectory.listFiles()?.filter {
                it.isFile && it.lastModified() >= startTime - 5000 &&
                        !it.name.endsWith(".part") && !it.name.endsWith(".ytdl")
            }?.maxByOrNull { it.lastModified() }
        }

        if (finishedFile == null || !finishedFile.exists()) {
            throw IllegalStateException("Download finished but final media file was not found")
        }

        Log.d("StreamDownloader", "Successfully produced media file: ${finishedFile.absolutePath} (${finishedFile.length()} bytes)")
        finishedFile
    }
}
