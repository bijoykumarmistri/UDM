package com.universal.dm.domain.sniffer

import com.universal.dm.data.model.AudioCodec
import com.universal.dm.data.model.VideoCodec
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * High-precision mathematical engine for media stream size estimation,
 * adaptive socket chunk partitioning, EMA throughput smoothing, and ETA calculation.
 */
object MediaSizeCalculator {

    // Codec compression efficiency relative to H.264 (baseline AVC = 1.00)
    // Applied ONLY when estimating from the H.264 baseline table (never double-applied to actual stream tbr).
    const val CODEC_EFFICIENCY_AV1 = 0.62f
    const val CODEC_EFFICIENCY_VP9 = 0.80f
    const val CODEC_EFFICIENCY_H264 = 1.00f

    // Standard paired audio bitrate for DASH video muxing (128 kbps AAC/M4A)
    const val PAIRED_AUDIO_BITRATE_KBPS = 128

    // Reference duration in seconds when video/audio duration is 0 or unavailable (4 minutes)
    const val NOMINAL_DURATION_SECONDS = 240L

    // Typical MP4/MKV muxing container overhead (~1.2%)
    private const val CONTAINER_OVERHEAD_FACTOR = 1.012

    // Minimum chunk size per parallel HTTP socket (256 KiB) to avoid socket setup overhead on small files
    const val MIN_CHUNK_SIZE_BYTES = 256L * 1024L

    // Default EMA smoothing factor (alpha): balances responsiveness to bandwidth shifts vs. spike rejection
    const val DEFAULT_EMA_ALPHA = 0.35

    /**
     * Returns standard baseline video bitrate in kbps for a given vertical resolution height (H.264 baseline).
     */
    fun getBaselineVideoBitrateKbps(height: Int): Int {
        return when {
            height >= 2160 -> 16000 // 4K Ultra HD (3840x2160)
            height >= 1440 -> 8500  // 2K / 1440p Quad HD (2560x1440)
            height >= 1080 -> 3800  // 1080p Full HD (1920x1080)
            height >= 720  -> 1900  // 720p HD (1280x720)
            height >= 480  -> 900   // 480p SD (854x480)
            height >= 360  -> 480   // 360p Data Saver (640x360)
            height >= 240  -> 250   // 240p
            else           -> 150   // 144p
        }
    }

    /**
     * Calculates the estimated size in bytes for a video variant including paired audio.
     *
     * Note: If [variantBitrateKbps] is provided by the extractor, it already reflects the stream's
     * true encoded bitrate (`tbr`) for its codec, so codec efficiency scaling is NOT applied again.
     */
    fun calculateVideoTotalBytes(
        height: Int,
        codec: VideoCodec?,
        durationSeconds: Long?,
        explicitVideoBytes: Long? = null,
        explicitAudioBytes: Long? = null,
        variantBitrateKbps: Int? = null,
        requiresAudioMerge: Boolean = false
    ): Long {
        val duration = if (durationSeconds != null && durationSeconds > 0) durationSeconds else NOMINAL_DURATION_SECONDS

        // 1. Paired Audio Stream Bytes (~128 kbps AAC) ONLY if audio merge is required
        val audioBytes = if (!requiresAudioMerge) {
            0L
        } else if (explicitAudioBytes != null && explicitAudioBytes > 0) {
            explicitAudioBytes
        } else {
            ((PAIRED_AUDIO_BITRATE_KBPS * 1000.0 * duration) / 8.0).roundToLong()
        }

        // 2. Video Stream Bytes
        val videoBytes = if (explicitVideoBytes != null && explicitVideoBytes > 0) {
            explicitVideoBytes
        } else if (variantBitrateKbps != null && variantBitrateKbps > 0) {
            // Actual stream bitrate already accounts for codec compression; only apply small container overhead.
            (((variantBitrateKbps * 1000.0 * duration) / 8.0) * CONTAINER_OVERHEAD_FACTOR).roundToLong()
        } else {
            val baselineH264Kbps = getBaselineVideoBitrateKbps(height)
            val efficiency = when (codec) {
                VideoCodec.AV1 -> CODEC_EFFICIENCY_AV1.toDouble()
                VideoCodec.VP9 -> CODEC_EFFICIENCY_VP9.toDouble()
                else -> CODEC_EFFICIENCY_H264.toDouble()
            }
            val effectiveBitrateKbps = baselineH264Kbps * efficiency
            (((effectiveBitrateKbps * 1000.0 * duration) / 8.0) * CONTAINER_OVERHEAD_FACTOR).roundToLong()
        }

        return (videoBytes + audioBytes).coerceAtLeast(0L)
    }

    /**
     * Calculates the estimated size in bytes for audio-only streams.
     */
    fun calculateAudioTotalBytes(
        targetBitrateKbps: Int,
        codec: AudioCodec?,
        durationSeconds: Long?,
        explicitAudioBytes: Long? = null,
        sourceBitrateKbps: Int? = null
    ): Long {
        val duration = if (durationSeconds != null && durationSeconds > 0) durationSeconds else NOMINAL_DURATION_SECONDS
        val codecFactor = when (codec) {
            AudioCodec.OPUS -> 0.78
            AudioCodec.MP3 -> 1.15
            else -> 1.0
        }

        if (explicitAudioBytes != null && explicitAudioBytes > 0) {
            if (targetBitrateKbps <= 0) return explicitAudioBytes
            if (sourceBitrateKbps != null && sourceBitrateKbps > 0) {
                val ratio = (targetBitrateKbps * codecFactor) / sourceBitrateKbps.toDouble()
                return (explicitAudioBytes * ratio).roundToLong().coerceAtLeast(1024L)
            }
            // No source bitrate known; apply codec overhead adjustment without distorting short clips
            return (explicitAudioBytes * codecFactor).roundToLong().coerceAtLeast(1024L)
        }

        val clampedTargetKbps = targetBitrateKbps.coerceAtLeast(32)
        val effectiveKbps = when (codec) {
            AudioCodec.OPUS -> (clampedTargetKbps * 0.78).coerceAtLeast(48.0)
            AudioCodec.MP3 -> (clampedTargetKbps * 1.15).coerceAtLeast(64.0)
            else -> clampedTargetKbps.toDouble()
        }

        return ((effectiveKbps * 1000.0 * duration) / 8.0).roundToLong().coerceAtLeast(0L)
    }

    /**
     * Computes optimal thread count clamped to [1, 16] so no chunk is smaller than [minChunkSizeBytes]
     * (unless the entire file is smaller than [minChunkSizeBytes], in which case 1 thread is used).
     */
    fun computeAdaptiveThreadCount(
        contentLength: Long,
        requestedThreads: Int,
        minChunkSizeBytes: Long = MIN_CHUNK_SIZE_BYTES
    ): Int {
        val clampedRequested = requestedThreads.coerceIn(1, 16)
        if (contentLength <= 0L) return 1
        val maxUsefulThreads = (contentLength / minChunkSizeBytes.coerceAtLeast(1L)).toInt().coerceAtLeast(1)
        return minOf(clampedRequested, maxUsefulThreads).coerceIn(1, 16)
    }

    /**
     * Partitions [0, contentLength - 1] into [threadCount] balanced, contiguous inclusive byte ranges.
     * Distributes remainder bytes across the first `remainder` chunks so all parallel sockets have
     * nearly identical byte workloads (eliminating single-thread tail lag).
     */
    fun computeChunkRanges(contentLength: Long, threadCount: Int): List<LongRange> {
        if (contentLength <= 0L) return emptyList()
        val effectiveThreads = threadCount.coerceIn(1, 16).coerceAtMost(contentLength.toInt().coerceAtLeast(1))
        val baseSize = contentLength / effectiveThreads
        val remainder = contentLength % effectiveThreads

        val ranges = ArrayList<LongRange>(effectiveThreads)
        var offset = 0L
        for (i in 0 until effectiveThreads) {
            val chunkLen = baseSize + if (i < remainder) 1L else 0L
            val end = offset + chunkLen - 1L
            ranges.add(offset..end)
            offset = end + 1L
        }
        return ranges
    }

    /**
     * Exponential Moving Average (EMA) speed smoother:
     * S_t = alpha * sample + (1 - alpha) * S_{t-1}
     */
    fun smoothSpeedEma(
        previousEmaBps: Long,
        sampleBps: Long,
        alpha: Double = DEFAULT_EMA_ALPHA
    ): Long {
        val safeSample = sampleBps.coerceAtLeast(0L)
        if (previousEmaBps <= 0L) return safeSample
        val a = alpha.coerceIn(0.05, 1.0)
        return ((a * safeSample) + ((1.0 - a) * previousEmaBps)).roundToLong().coerceAtLeast(0L)
    }

    /**
     * Computes remaining download time in whole seconds (ceiling), or 0L if unknown/stalled.
     */
    fun calculateEtaSeconds(
        downloadedBytes: Long,
        totalBytes: Long,
        speedBytesPerSec: Long
    ): Long {
        if (speedBytesPerSec <= 0L || totalBytes <= 0L || downloadedBytes >= totalBytes) return 0L
        val remaining = totalBytes - downloadedBytes.coerceAtLeast(0L)
        return ceil(remaining.toDouble() / speedBytesPerSec.toDouble()).toLong().coerceAtLeast(1L)
    }

    /**
     * Formats ETA seconds cleanly into `Xh XXm left` or `MM:SS left`.
     */
    fun formatEta(etaSeconds: Long): String {
        if (etaSeconds <= 0L) return "--"
        val hours = etaSeconds / 3600L
        val minutes = (etaSeconds % 3600L) / 60L
        val seconds = etaSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%dh %02dm left", hours, minutes)
        } else {
            String.format(Locale.US, "%02d:%02d left", minutes, seconds)
        }
    }

    /**
     * Formats bytes into a human-readable string (e.g. "1.24 GB", "45.8 MB", "850 KB").
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "Unknown"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 100.0 -> String.format(Locale.US, "%.0f MB", mb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", kb.coerceAtLeast(1.0))
        }
    }
}
