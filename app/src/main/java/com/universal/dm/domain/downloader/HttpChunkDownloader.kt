package com.universal.dm.domain.downloader

import com.universal.dm.data.local.DownloadChunkEntity
import com.universal.dm.data.local.DownloadDao
import com.universal.dm.data.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class HttpChunkDownloader(
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            if (original.header("User-Agent") == null) {
                requestBuilder.header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                )
            }
            chain.proceed(requestBuilder.build())
        }
        .build()
) {

    suspend fun downloadDirectHttp(
        taskId: String,
        url: String,
        targetFile: File,
        requestedThreadCount: Int,
        referer: String? = null,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long, speedBytesPerSec: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        // 1. Probe Server Headers (HEAD request with Range fallback)
        val probeResult = probeServer(url, referer)
        val contentLength = probeResult.contentLength
        val supportsRanges = probeResult.supportsRanges && contentLength > 0L

        if (supportsRanges) {
            downloadParallelChunks(
                taskId = taskId,
                url = url,
                targetFile = targetFile,
                contentLength = contentLength,
                requestedThreadCount = requestedThreadCount,
                referer = referer,
                onProgress = onProgress
            )
        } else {
            downloadSingleStream(
                taskId = taskId,
                url = url,
                targetFile = targetFile,
                contentLength = contentLength,
                referer = referer,
                onProgress = onProgress
            )
        }
    }

    private suspend fun downloadParallelChunks(
        taskId: String,
        url: String,
        targetFile: File,
        contentLength: Long,
        requestedThreadCount: Int,
        referer: String? = null,
        onProgress: suspend (Long, Long, Long) -> Unit
    ): File = coroutineScope {
        // Pre-allocate file space
        if (!targetFile.exists() || targetFile.length() != contentLength) {
            RandomAccessFile(targetFile, "rw").use { it.setLength(contentLength) }
        }

        // Check if chunks already exist in Room (for Pause / Resume)
        var existingChunks = downloadDao.getChunksForTask(taskId)
        val threadCount = requestedThreadCount.coerceIn(1, 16)

        if (existingChunks.isEmpty()) {
            val chunkSize = contentLength / threadCount
            val newChunks = (0 until threadCount).map { index ->
                val start = index * chunkSize
                val end = if (index == threadCount - 1) contentLength - 1 else (index + 1) * chunkSize - 1
                DownloadChunkEntity(
                    taskId = taskId,
                    threadIndex = index,
                    startByte = start,
                    endByte = end,
                    currentByte = start,
                    isCompleted = false
                )
            }
            downloadDao.insertChunks(newChunks)
            existingChunks = downloadDao.getChunksForTask(taskId)
        }

        // Compute already downloaded bytes
        val initialDownloaded = existingChunks.sumOf {
            (it.currentByte - it.startByte).coerceAtLeast(0L)
        }
        val downloadedCounter = AtomicLong(initialDownloaded)
        val lastSampleBytes = AtomicLong(initialDownloaded)
        val lastSampleTime = AtomicLong(System.currentTimeMillis())
        val currentSpeed = AtomicLong(0L)

        // Progress ticker coroutine updating Room & callback every 500ms
        val progressJob = launch {
            while (isActive) {
                delay(500)
                val now = System.currentTimeMillis()
                val currentBytes = downloadedCounter.get()
                val elapsedMs = (now - lastSampleTime.get()).coerceAtLeast(1L)
                val deltaBytes = (currentBytes - lastSampleBytes.get()).coerceAtLeast(0L)

                val speed = (deltaBytes * 1000L) / elapsedMs
                currentSpeed.set(speed)
                lastSampleBytes.set(currentBytes)
                lastSampleTime.set(now)

                onProgress(currentBytes, contentLength, speed)
                downloadDao.updateTaskProgress(taskId, currentBytes, speed, DownloadStatus.DOWNLOADING)
            }
        }

        try {
            // Launch parallel coroutines for each incomplete chunk
            val incompleteChunks = existingChunks.filter { !it.isCompleted && it.currentByte <= it.endByte }

            val downloadJobs = incompleteChunks.map { chunk ->
                async(Dispatchers.IO) {
                    downloadSingleChunk(
                        chunk = chunk,
                        url = url,
                        targetFile = targetFile,
                        referer = referer,
                        downloadedCounter = downloadedCounter
                    )
                }
            }

            downloadJobs.awaitAll()
        } finally {
            progressJob.cancel()
        }

        // Final progress update
        val finalDownloaded = downloadedCounter.get()
        onProgress(finalDownloaded, contentLength, 0L)
        targetFile
    }

    private suspend fun downloadSingleChunk(
        chunk: DownloadChunkEntity,
        url: String,
        targetFile: File,
        referer: String? = null,
        downloadedCounter: AtomicLong
    ) {
        val start = chunk.currentByte
        val end = chunk.endByte
        if (start > end) return

        val request = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=$start-$end")
            .apply {
                if (!referer.isNullOrBlank()) {
                    addHeader("Referer", referer)
                }
            }
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful && response.code != 206) {
            throw IOException("Unexpected HTTP code ${response.code} for chunk ${chunk.threadIndex}")
        }
        val body = response.body ?: throw IllegalStateException("Empty response body for chunk ${chunk.threadIndex}")

        val buffer = ByteArray(32 * 1024)
        var currentOffset = start
        var lastDbUpdateTime = System.currentTimeMillis()

        RandomAccessFile(targetFile, "rw").use { raf ->
            raf.seek(start)
            body.byteStream().use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break

                    raf.write(buffer, 0, read)
                    currentOffset += read
                    downloadedCounter.addAndGet(read.toLong())

                    // Save chunk offset to DB periodically
                    val now = System.currentTimeMillis()
                    if (now - lastDbUpdateTime > 1000) {
                        downloadDao.updateChunkProgress(chunk.chunkId, currentOffset, false)
                        lastDbUpdateTime = now
                    }
                }
            }
        }

        // Mark chunk as completed
        val isFinished = currentOffset >= end
        downloadDao.updateChunkProgress(chunk.chunkId, currentOffset, isFinished)
    }

    private suspend fun downloadSingleStream(
        taskId: String,
        url: String,
        targetFile: File,
        contentLength: Long,
        referer: String? = null,
        onProgress: suspend (Long, Long, Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply {
                if (!referer.isNullOrBlank()) {
                    addHeader("Referer", referer)
                }
            }
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Unexpected HTTP code ${response.code} downloading stream")
        }
        val body = response.body ?: throw IllegalStateException("Empty response body")

        val total = if (contentLength > 0) contentLength else body.contentLength()
        var downloaded = 0L
        val buffer = ByteArray(32 * 1024)
        var lastSampleTime = System.currentTimeMillis()
        var lastSampleBytes = 0L

        FileOutputStream(targetFile).use { output ->
            body.byteStream().use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break

                    output.write(buffer, 0, read)
                    downloaded += read

                    val now = System.currentTimeMillis()
                    if (now - lastSampleTime >= 500) {
                        val elapsedMs = (now - lastSampleTime).coerceAtLeast(1L)
                        val delta = (downloaded - lastSampleBytes).coerceAtLeast(0L)
                        val speed = (delta * 1000L) / elapsedMs

                        lastSampleBytes = downloaded
                        lastSampleTime = now

                        onProgress(downloaded, total, speed)
                        downloadDao.updateTaskProgress(taskId, downloaded, speed, DownloadStatus.DOWNLOADING)
                    }
                }
            }
        }

        onProgress(downloaded, total, 0L)
        targetFile
    }

    private fun probeServer(url: String, referer: String? = null): ProbeResult {
        return try {
            val headReq = Request.Builder()
                .url(url)
                .apply {
                    if (!referer.isNullOrBlank()) {
                        addHeader("Referer", referer)
                    }
                }
                .head()
                .build()
            val (length, acceptsRanges, isHeadSuccess) = okHttpClient.newCall(headReq).execute().use { response ->
                val accepts = response.header("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true ||
                        response.header("Content-Range") != null
                val len = response.header("Content-Length")?.toLongOrNull() ?: 0L
                Triple(len, accepts, response.isSuccessful)
            }

            if (length > 0 && isHeadSuccess) {
                ProbeResult(length, acceptsRanges)
            } else {
                // If HEAD had no length, probe with a 1-byte range GET
                val rangeReq = Request.Builder()
                    .url(url)
                    .addHeader("Range", "bytes=0-0")
                    .apply {
                        if (!referer.isNullOrBlank()) {
                            addHeader("Referer", referer)
                        }
                    }
                    .build()
                okHttpClient.newCall(rangeReq).execute().use { rangeResp ->
                    val rangeHeader = rangeResp.header("Content-Range")
                    val totalLength = rangeHeader?.substringAfterLast('/')?.toLongOrNull() ?: 0L
                    val isRangeAccepted = rangeResp.code == 206 || rangeHeader != null

                    ProbeResult(totalLength, isRangeAccepted)
                }
            }
        } catch (_: Exception) {
            ProbeResult(0L, false)
        }
    }

    private data class ProbeResult(
        val contentLength: Long,
        val supportsRanges: Boolean
    )
}
