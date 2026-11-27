package com.universal.dm.domain.downloader

import com.universal.dm.core.NetworkClient
import com.universal.dm.data.local.DownloadChunkEntity
import com.universal.dm.data.local.DownloadDao
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.domain.sniffer.MediaSizeCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
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
import java.util.concurrent.atomic.AtomicLong

class HttpChunkDownloader(
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient = NetworkClient.client
) {

    suspend fun downloadDirectHttp(
        taskId: String,
        url: String,
        targetFile: File,
        requestedThreadCount: Int,
        referer: String? = null,
        userAgent: String? = null,
        persistChunksInDb: Boolean = true,
        updateTaskProgressInDb: Boolean = true,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long, speedBytesPerSec: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        targetFile.parentFile?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }

        // 1. Probe server capabilities (Content-Length & HTTP Range support)
        val probeResult = probeServer(url, referer, userAgent)
        val contentLength = probeResult.contentLength
        val supportsRanges = probeResult.supportsRanges && contentLength > 0L

        if (supportsRanges) {
            try {
                downloadParallelChunks(
                    taskId = taskId,
                    url = url,
                    targetFile = targetFile,
                    contentLength = contentLength,
                    requestedThreadCount = requestedThreadCount,
                    referer = referer,
                    userAgent = userAgent,
                    persistChunksInDb = persistChunksInDb,
                    updateTaskProgressInDb = updateTaskProgressInDb,
                    onProgress = onProgress
                )
            } catch (e: HttpStatusException) {
                if (e.code == 403 || e.code == 416) {
                    downloadSingleStream(
                        taskId = taskId,
                        url = url,
                        targetFile = targetFile,
                        contentLength = contentLength,
                        referer = referer,
                        userAgent = userAgent,
                        updateTaskProgressInDb = updateTaskProgressInDb,
                        onProgress = onProgress
                    )
                } else {
                    throw e
                }
            }
        } else {
            downloadSingleStream(
                taskId = taskId,
                url = url,
                targetFile = targetFile,
                contentLength = contentLength,
                referer = referer,
                userAgent = userAgent,
                updateTaskProgressInDb = updateTaskProgressInDb,
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
        userAgent: String? = null,
        persistChunksInDb: Boolean = true,
        updateTaskProgressInDb: Boolean = true,
        onProgress: suspend (Long, Long, Long) -> Unit
    ): File = coroutineScope {
        // Pre-allocate target file to exact content length
        if (!targetFile.exists() || targetFile.length() != contentLength) {
            RandomAccessFile(targetFile, "rw").use { it.setLength(contentLength) }
        }

        var existingChunks = if (persistChunksInDb) {
            downloadDao.getChunksForTask(taskId)
        } else {
            emptyList()
        }
        // Re-partition if no chunks exist or if remote contentLength changed since last pause
        val storedEnd = existingChunks.maxOfOrNull { it.endByte }
        if (existingChunks.isEmpty() || storedEnd != contentLength - 1L) {
            if (persistChunksInDb && existingChunks.isNotEmpty()) {
                downloadDao.deleteChunksForTask(taskId)
            }
            val effectiveThreads = MediaSizeCalculator.computeAdaptiveThreadCount(
                contentLength = contentLength,
                requestedThreads = requestedThreadCount
            )
            val ranges = MediaSizeCalculator.computeChunkRanges(contentLength, effectiveThreads)
            val newChunks = ranges.mapIndexed { index, range ->
                DownloadChunkEntity(
                    taskId = taskId,
                    threadIndex = index,
                    startByte = range.first,
                    endByte = range.last,
                    currentByte = range.first,
                    isCompleted = false
                )
            }
            if (persistChunksInDb) {
                downloadDao.insertChunks(newChunks)
                existingChunks = downloadDao.getChunksForTask(taskId)
            } else {
                existingChunks = newChunks
            }
        }

        val initialDownloaded = existingChunks.sumOf { chunk ->
            val chunkLen = (chunk.endByte - chunk.startByte + 1L).coerceAtLeast(0L)
            (chunk.currentByte - chunk.startByte).coerceIn(0L, chunkLen)
        }
        val downloadedCounter = AtomicLong(initialDownloaded)
        var lastSampleBytes = initialDownloaded
        var lastSampleNanos = System.nanoTime()
        var emaSpeedBps = 0L

        val progressJob = launch {
            while (isActive) {
                delay(500)
                val nowNanos = System.nanoTime()
                val currentBytes = downloadedCounter.get().coerceAtMost(contentLength)
                val elapsedNanos = (nowNanos - lastSampleNanos).coerceAtLeast(1L)
                val deltaBytes = (currentBytes - lastSampleBytes).coerceAtLeast(0L)

                val instantSpeed = ((deltaBytes.toDouble() * 1_000_000_000.0) / elapsedNanos.toDouble()).toLong()
                emaSpeedBps = MediaSizeCalculator.smoothSpeedEma(emaSpeedBps, instantSpeed)

                lastSampleBytes = currentBytes
                lastSampleNanos = nowNanos

                onProgress(currentBytes, contentLength, emaSpeedBps)
                if (updateTaskProgressInDb) {
                    downloadDao.updateTaskProgress(taskId, currentBytes, contentLength, emaSpeedBps, DownloadStatus.DOWNLOADING)
                }
            }
        }

        try {
            val incompleteChunks = existingChunks.filter { !it.isCompleted && it.currentByte <= it.endByte }
            val isMultiPart = existingChunks.size > 1

            val downloadJobs = incompleteChunks.map { chunk ->
                async(Dispatchers.IO) {
                    downloadSingleChunk(
                        chunk = chunk,
                        url = url,
                        targetFile = targetFile,
                        contentLength = contentLength,
                        isMultiPart = isMultiPart,
                        referer = referer,
                        userAgent = userAgent,
                        persistChunksInDb = persistChunksInDb,
                        downloadedCounter = downloadedCounter
                    )
                }
            }

            downloadJobs.awaitAll()
        } finally {
            progressJob.cancel()
        }

        val finalDownloaded = downloadedCounter.get().coerceAtMost(contentLength)
        onProgress(finalDownloaded, contentLength, 0L)
        if (updateTaskProgressInDb) {
            downloadDao.updateTaskProgress(taskId, finalDownloaded, contentLength, 0L, DownloadStatus.DOWNLOADING)
        }
        targetFile
    }

    private suspend fun downloadSingleChunk(
        chunk: DownloadChunkEntity,
        url: String,
        targetFile: File,
        contentLength: Long,
        isMultiPart: Boolean,
        referer: String? = null,
        userAgent: String? = null,
        persistChunksInDb: Boolean = true,
        downloadedCounter: AtomicLong
    ) {
        var currentOffset = chunk.currentByte
        val end = chunk.endByte
        if (currentOffset > end) {
            if (persistChunksInDb && !chunk.isCompleted) {
                downloadDao.updateChunkProgress(chunk.chunkId, currentOffset, true)
            }
            return
        }

        val buffer = ByteArray(IO_BUFFER_SIZE)
        var attempt = 0
        var lastDbUpdateNanos = System.nanoTime()

        try {
            while (currentOffset <= end) {
                val ctx = currentCoroutineContext()
                if (!ctx.isActive) {
                    throw CancellationException("Chunk ${chunk.threadIndex} cancelled")
                }

                try {
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Range", "bytes=$currentOffset-$end")
                        .apply {
                            if (!userAgent.isNullOrBlank()) {
                                header("User-Agent", userAgent)
                            }
                            if (!referer.isNullOrBlank()) {
                                addHeader("Referer", referer)
                            }
                        }
                        .build()

                    val call = okHttpClient.newCall(request)
                    val completionHandle = ctx[Job]?.invokeOnCompletion { call.cancel() }

                    try {
                        call.execute().use { response ->
                            if (response.code == 401 || response.code == 403 || response.code == 404 || response.code == 410 || response.code == 416) {
                                call.cancel()
                                throw HttpStatusException(
                                    response.code,
                                    "HTTP ${response.code} for chunk ${chunk.threadIndex}"
                                )
                            }
                            // Strict Range enforcement: if requesting a partial slice (multi-part or resumed),
                            // HTTP 200 means the server ignored Range and would overwrite other chunks!
                            val expectsPartial = isMultiPart || currentOffset > 0L || end < contentLength - 1L
                            if (expectsPartial && response.code != 206) {
                                call.cancel()
                                throw IOException("Server ignored Range request (HTTP ${response.code}) for chunk ${chunk.threadIndex}")
                            }
                            if (!response.isSuccessful && response.code != 206) {
                                throw IOException("Unexpected HTTP code ${response.code} for chunk ${chunk.threadIndex}")
                            }

                            val body = response.body
                                ?: throw IOException("Empty response body for chunk ${chunk.threadIndex}")

                            RandomAccessFile(targetFile, "rw").use { raf ->
                                raf.seek(currentOffset)
                                body.byteStream().use { input ->
                                    while (currentOffset <= end) {
                                        if (!ctx.isActive) {
                                            throw CancellationException("Chunk ${chunk.threadIndex} paused/cancelled")
                                        }
                                        val remaining = (end - currentOffset) + 1L
                                        val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                                        val read = input.read(buffer, 0, toRead)
                                        if (read == -1) break

                                        raf.write(buffer, 0, read)
                                        currentOffset += read
                                        downloadedCounter.addAndGet(read.toLong())

                                        val nowNanos = System.nanoTime()
                                        if (persistChunksInDb && nowNanos - lastDbUpdateNanos >= CHUNK_DB_SYNC_INTERVAL_NANOS) {
                                            downloadDao.updateChunkProgress(chunk.chunkId, currentOffset, false)
                                            lastDbUpdateNanos = nowNanos
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        completionHandle?.dispose()
                    }

                    if (currentOffset <= end) {
                        throw IOException("Premature EOF for chunk ${chunk.threadIndex} at $currentOffset/$end")
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (hse: HttpStatusException) {
                    throw hse
                } catch (ioe: IOException) {
                    attempt++
                    if (attempt > MAX_CHUNK_RETRIES || !ctx.isActive) {
                        throw ioe
                    }
                    // Save partial progress before backoff retry
                    if (persistChunksInDb) {
                        withContext(NonCancellable) {
                            downloadDao.updateChunkProgress(chunk.chunkId, currentOffset, false)
                        }
                    }
                    delay(RETRY_BASE_DELAY_MS * (1L shl (attempt - 1)))
                }
            }
        } finally {
            // Persist exact offset (even on pause/cancellation) so resume never loses buffered bytes
            if (persistChunksInDb) {
                val isFinished = currentOffset > end
                withContext(NonCancellable) {
                    downloadDao.updateChunkProgress(chunk.chunkId, currentOffset, isFinished)
                }
            }
        }
    }

    private suspend fun downloadSingleStream(
        taskId: String,
        url: String,
        targetFile: File,
        contentLength: Long,
        referer: String? = null,
        userAgent: String? = null,
        updateTaskProgressInDb: Boolean = true,
        onProgress: suspend (Long, Long, Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply {
                if (!userAgent.isNullOrBlank()) {
                    header("User-Agent", userAgent)
                }
                if (!referer.isNullOrBlank()) {
                    addHeader("Referer", referer)
                }
            }
            .build()

        val call = okHttpClient.newCall(request)
        val completionHandle = coroutineContext[Job]?.invokeOnCompletion { call.cancel() }

        var downloaded = 0L
        var total = contentLength
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected HTTP code ${response.code} downloading stream")
                }
                val body = response.body ?: throw IOException("Empty response body")

                if (total <= 0L) {
                    total = body.contentLength().coerceAtLeast(0L)
                }

                val buffer = ByteArray(IO_BUFFER_SIZE)
                var lastSampleNanos = System.nanoTime()
                var lastSampleBytes = 0L
                var emaSpeedBps = 0L

                FileOutputStream(targetFile).use { output ->
                    body.byteStream().use { input ->
                        while (true) {
                            if (!isActive) {
                                throw CancellationException("Single stream cancelled")
                            }
                            val read = input.read(buffer)
                            if (read == -1) break

                            output.write(buffer, 0, read)
                            downloaded += read

                            val nowNanos = System.nanoTime()
                            val elapsedNanos = nowNanos - lastSampleNanos
                            if (elapsedNanos >= PROGRESS_TICK_NANOS) {
                                val delta = (downloaded - lastSampleBytes).coerceAtLeast(0L)
                                val instantSpeed = ((delta.toDouble() * 1_000_000_000.0) / elapsedNanos.toDouble()).toLong()
                                emaSpeedBps = MediaSizeCalculator.smoothSpeedEma(emaSpeedBps, instantSpeed)

                                lastSampleBytes = downloaded
                                lastSampleNanos = nowNanos

                                onProgress(downloaded, total, emaSpeedBps)
                                if (updateTaskProgressInDb) {
                                    downloadDao.updateTaskProgress(taskId, downloaded, total, emaSpeedBps, DownloadStatus.DOWNLOADING)
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            completionHandle?.dispose()
        }

        val finalTotal = if (total > 0L) total else downloaded
        onProgress(downloaded, finalTotal, 0L)
        if (updateTaskProgressInDb) {
            downloadDao.updateTaskProgress(taskId, downloaded, finalTotal, 0L, DownloadStatus.DOWNLOADING)
        }
        targetFile
    }

    private suspend fun probeServer(
        url: String,
        referer: String? = null,
        userAgent: String? = null
    ): ProbeResult {
        return try {
            val ctx = currentCoroutineContext()
            var headLength = 0L

            if (!url.contains("googlevideo.com", ignoreCase = true)) {
                val headReq = Request.Builder()
                    .url(url)
                    .apply {
                        if (!userAgent.isNullOrBlank()) {
                            header("User-Agent", userAgent)
                        }
                        if (!referer.isNullOrBlank()) {
                            addHeader("Referer", referer)
                        }
                    }
                    .head()
                    .build()

                val headCall = okHttpClient.newCall(headReq)
                val headHandle = ctx[Job]?.invokeOnCompletion { headCall.cancel() }

                val (len, headAcceptsRanges, isHeadSuccess) = try {
                    headCall.execute().use { response ->
                        val accepts = response.header("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true ||
                            response.header("Content-Range") != null
                        val parsedLen = response.header("Content-Length")?.toLongOrNull() ?: 0L
                        Triple(parsedLen, accepts, response.isSuccessful)
                    }
                } finally {
                    headHandle?.dispose()
                }
                headLength = len

                if (isHeadSuccess && headLength > 0L && headAcceptsRanges) {
                    return ProbeResult(headLength, true)
                }
            }

            // Probe with 1-byte Range GET either to discover Content-Length or verify Range support
            val rangeReq = Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=0-0")
                .apply {
                    if (!userAgent.isNullOrBlank()) {
                        header("User-Agent", userAgent)
                    }
                    if (!referer.isNullOrBlank()) {
                        addHeader("Referer", referer)
                    }
                }
                .build()

            val rangeCall = okHttpClient.newCall(rangeReq)
            val rangeHandle = ctx[Job]?.invokeOnCompletion { rangeCall.cancel() }

            try {
                rangeCall.execute().use { rangeResp ->
                    val rangeHeader = rangeResp.header("Content-Range")
                    val rangeTotal = rangeHeader?.substringAfterLast('/')?.trim()?.toLongOrNull() ?: 0L
                    val isRangeAccepted = rangeResp.code == 206 && rangeTotal > 0L

                    if (isRangeAccepted) {
                        ProbeResult(rangeTotal, true)
                    } else {
                        // Server returned 200 OK or 403 — cancel call immediately before closing so body isn't drained
                        val fallbackLength = (rangeResp.header("Content-Length")?.toLongOrNull() ?: 0L)
                            .takeIf { it > 0L } ?: headLength
                        rangeCall.cancel()
                        ProbeResult(fallbackLength, false)
                    }
                }
            } finally {
                rangeHandle?.dispose()
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            ProbeResult(0L, false)
        }
    }

    private class HttpStatusException(val code: Int, message: String) : IOException(message)

    private data class ProbeResult(
        val contentLength: Long,
        val supportsRanges: Boolean
    )

    companion object {
        private const val IO_BUFFER_SIZE = 64 * 1024
        private const val MAX_CHUNK_RETRIES = 3
        private const val RETRY_BASE_DELAY_MS = 500L
        private const val PROGRESS_TICK_NANOS = 500_000_000L
        private const val CHUNK_DB_SYNC_INTERVAL_NANOS = 1_000_000_000L
    }
}
