package com.universal.dm.domain.downloader

import android.content.Context
import android.util.Log
import com.universal.dm.UdmApplication
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Direct native FFmpeg runner invoking the bundled `libffmpeg.so` binary
 * without spawning a Python/yt-dlp process.
 *
 * Used for:
 * - Instant zero-reencode muxing of parallel-downloaded video + audio streams
 * - Fast audio packaging/transcoding with embedded cover-art thumbnails
 */
object FfmpegHelper {

    private const val TAG = "FfmpegHelper"

    suspend fun downloadThumbnail(
        okHttpClient: OkHttpClient,
        thumbnailUrl: String?,
        youtubeVideoId: String?,
        targetFile: File
    ): File? = withContext(Dispatchers.IO) {
        val candidates = buildList {
            if (!thumbnailUrl.isNullOrBlank()) add(thumbnailUrl)
            if (!youtubeVideoId.isNullOrBlank()) {
                add("https://i.ytimg.com/vi/$youtubeVideoId/hqdefault.jpg")
                add("https://i.ytimg.com/vi/$youtubeVideoId/mqdefault.jpg")
            }
        }.distinct()

        for (candidate in candidates) {
            val saved = runCatching {
                val request = Request.Builder().url(candidate).build()
                okHttpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val body = resp.body ?: return@use null
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { out ->
                        body.byteStream().copyTo(out)
                    }
                    if (targetFile.exists() && targetFile.length() > 1024L) targetFile else null
                }
            }.getOrNull()
            if (saved != null) return@withContext saved
        }
        null
    }

    suspend fun muxVideoAndAudio(
        context: Context,
        videoFile: File,
        audioFile: File,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        outputFile.parentFile?.mkdirs()
        val isMp4 = outputFile.extension.equals("mp4", ignoreCase = true)

        val args = buildList {
            add("-y")
            add("-i"); add(videoFile.absolutePath)
            add("-i"); add(audioFile.absolutePath)
            add("-map"); add("0:v:0")
            add("-map"); add("1:a:0")
            add("-c"); add("copy")
            if (isMp4) {
                add("-movflags"); add("+faststart")
            }
            add(outputFile.absolutePath)
        }

        val exitCode = executeFfmpeg(context, args)
        if (exitCode == 0 && outputFile.exists() && outputFile.length() > 0L) {
            return@withContext outputFile
        }

        // If MP4 container rejects e.g. Opus audio or VP9 video, cleanly fall back to MKV remux
        if (isMp4) {
            val mkvFallback = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}.mkv")
            val mkvArgs = listOf(
                "-y",
                "-i", videoFile.absolutePath,
                "-i", audioFile.absolutePath,
                "-map", "0:v:0",
                "-map", "1:a:0",
                "-c", "copy",
                mkvFallback.absolutePath
            )
            val mkvExit = executeFfmpeg(context, mkvArgs)
            if (mkvExit == 0 && mkvFallback.exists() && mkvFallback.length() > 0L) {
                return@withContext mkvFallback
            }
        }

        throw IOException("FFmpeg muxing failed (exit=$exitCode)")
    }

    suspend fun packageAudioWithThumbnail(
        context: Context,
        inputAudioFile: File,
        thumbnailFile: File?,
        outputFile: File,
        targetExt: String,
        bitrateKbps: Int,
        title: String,
        inputIsM4aContainer: Boolean
    ): File = withContext(Dispatchers.IO) {
        outputFile.parentFile?.mkdirs()
        val normalizedExt = targetExt.lowercase()
        val safeBitrate = "${bitrateKbps.coerceIn(64, 320)}k"
        val hasThumb = thumbnailFile != null && thumbnailFile.exists() && thumbnailFile.length() > 0L
        val supportsAttachedPic = normalizedExt == "mp3" || normalizedExt == "m4a" || normalizedExt == "flac"

        if (hasThumb && supportsAttachedPic) {
            val withCoverArgs = buildAudioArgs(
                inputAudioFile = inputAudioFile,
                thumbnailFile = thumbnailFile,
                outputFile = outputFile,
                targetExt = normalizedExt,
                safeBitrate = safeBitrate,
                title = title,
                inputIsM4aContainer = inputIsM4aContainer
            )
            val exitWithCover = executeFfmpeg(context, withCoverArgs)
            if (exitWithCover == 0 && outputFile.exists() && outputFile.length() > 0L) {
                return@withContext outputFile
            }
            Log.w(TAG, "FFmpeg cover art embed failed (exit=$exitWithCover) for $normalizedExt; retrying audio-only")
            runCatching { if (outputFile.exists()) outputFile.delete() }
        }

        val audioOnlyArgs = buildAudioArgs(
            inputAudioFile = inputAudioFile,
            thumbnailFile = null,
            outputFile = outputFile,
            targetExt = normalizedExt,
            safeBitrate = safeBitrate,
            title = title,
            inputIsM4aContainer = inputIsM4aContainer
        )
        val exitCode = executeFfmpeg(context, audioOnlyArgs)
        if (exitCode == 0 && outputFile.exists() && outputFile.length() > 0L) {
            return@withContext outputFile
        }
        throw IOException("FFmpeg audio conversion failed (exit=$exitCode)")
    }

    private fun buildAudioArgs(
        inputAudioFile: File,
        thumbnailFile: File?,
        outputFile: File,
        targetExt: String,
        safeBitrate: String,
        title: String,
        inputIsM4aContainer: Boolean
    ): List<String> = buildList {
        add("-y")
        add("-i"); add(inputAudioFile.absolutePath)
        if (thumbnailFile != null) {
            add("-i"); add(thumbnailFile.absolutePath)
            add("-map"); add("0:a:0")
            add("-map"); add("1:v:0")
        } else {
            add("-vn")
        }

        when (targetExt) {
            "m4a" -> {
                if (inputIsM4aContainer) {
                    add("-c:a"); add("copy")
                } else {
                    add("-c:a"); add("aac")
                    add("-b:a"); add(safeBitrate)
                }
                if (thumbnailFile != null) {
                    add("-c:v"); add("mjpeg")
                    add("-disposition:v:0"); add("attached_pic")
                }
                add("-movflags"); add("+faststart")
            }
            "mp3" -> {
                add("-c:a"); add("libmp3lame")
                add("-b:a"); add(safeBitrate)
                if (thumbnailFile != null) {
                    add("-c:v"); add("mjpeg")
                    add("-id3v2_version"); add("3")
                    add("-metadata:s:v"); add("title=Album cover")
                    add("-metadata:s:v"); add("comment=Cover (front)")
                }
            }
            "flac" -> {
                add("-c:a"); add("flac")
                if (thumbnailFile != null) {
                    add("-c:v"); add("mjpeg")
                    add("-disposition:v:0"); add("attached_pic")
                }
            }
            "opus" -> {
                add("-c:a"); add("libopus")
                add("-b:a"); add(safeBitrate)
            }
            "wav" -> {
                add("-c:a"); add("pcm_s16le")
            }
            else -> {
                add("-c:a"); add("libmp3lame")
                add("-b:a"); add(safeBitrate)
            }
        }

        if (title.isNotBlank()) {
            add("-metadata"); add("title=$title")
        }
        add(outputFile.absolutePath)
    }

    private suspend fun executeFfmpeg(context: Context, args: List<String>): Int = withContext(Dispatchers.IO) {
        UdmApplication.ensureYoutubeDlInitialized(context.applicationContext)

        val nativeBinDir = File(context.applicationInfo.nativeLibraryDir)
        val ffmpegBin = File(nativeBinDir, "libffmpeg.so")
        if (!ffmpegBin.exists()) {
            throw IOException("FFmpeg binary not found at ${ffmpegBin.absolutePath}")
        }

        val baseDir = File(context.noBackupFilesDir, YoutubeDL.baseName)
        val packagesDir = File(baseDir, "packages")
        val ldLibraryPath = listOf(
            File(packagesDir, "python/usr/lib").absolutePath,
            File(packagesDir, "ffmpeg/usr/lib").absolutePath,
            File(packagesDir, "aria2c/usr/lib").absolutePath
        ).joinToString(":")

        val command = ArrayList<String>(args.size + 1).apply {
            add(ffmpegBin.absolutePath)
            addAll(args)
        }

        val processBuilder = ProcessBuilder(command).redirectErrorStream(true)
        processBuilder.environment().apply {
            this["LD_LIBRARY_PATH"] = ldLibraryPath
            this["PATH"] = (System.getenv("PATH") ?: "") + ":" + nativeBinDir.absolutePath
            this["TMPDIR"] = context.cacheDir.absolutePath
        }

        val process = processBuilder.start()
        val ctx = currentCoroutineContext()
        val cancelHandle = ctx[Job]?.invokeOnCompletion {
            runCatching { process.destroy() }
        }

        try {
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                Log.w(TAG, "FFmpeg exited with $exitCode: ${output.takeLast(600)}")
            }
            exitCode
        } catch (ce: CancellationException) {
            runCatching { process.destroy() }
            throw ce
        } finally {
            cancelHandle?.dispose()
        }
    }
}
