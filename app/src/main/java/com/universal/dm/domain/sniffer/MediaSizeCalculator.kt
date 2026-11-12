package com.universal.dm.domain.sniffer

import com.universal.dm.data.model.AudioCodec
import com.universal.dm.data.model.VideoCodec
import java.util.Locale

/**
 * High-precision mathematical engine for estimating and calculating digital video and audio
 * stream file sizes based on bitrate, duration, container overhead, and codec compression efficiency.
 */
object MediaSizeCalculator {

    // Codec compression efficiency relative to H.264 (baseline AVC = 1.00)
    // AV1 (AOMedia Video 1) achieves ~38% higher compression efficiency than H.264 at equivalent visual quality
    // VP9 achieves ~20% higher compression efficiency than H.264
    const val CODEC_EFFICIENCY_AV1 = 0.62f
    const val CODEC_EFFICIENCY_VP9 = 0.80f
    const val CODEC_EFFICIENCY_H264 = 1.00f

    // Standard paired audio bitrate for DASH video muxing (128 kbps AAC/M4A)
    const val PAIRED_AUDIO_BITRATE_KBPS = 128

    // Reference duration in seconds when video/audio duration is 0 or unavailable (4 minutes)
    const val NOMINAL_DURATION_SECONDS = 240L

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
     * @param height Resolution vertical height (e.g. 2160, 1440, 1080, 720, 480, 360)
     * @param codec Target video codec (AV1, VP9, H.264)
     * @param durationSeconds Total media duration in seconds
     * @param explicitVideoBytes Known video stream size if provided by server/yt-dlp
     * @param explicitAudioBytes Known audio stream size if provided by server/yt-dlp
     * @param variantBitrateKbps Bitrate from stream format if provided
     */
    fun calculateVideoTotalBytes(
        height: Int,
        codec: VideoCodec?,
        durationSeconds: Long?,
        explicitVideoBytes: Long? = null,
        explicitAudioBytes: Long? = null,
        variantBitrateKbps: Int? = null
    ): Long {
        val duration = if (durationSeconds != null && durationSeconds > 0) durationSeconds else NOMINAL_DURATION_SECONDS

        // 1. Calculate Paired Audio Stream Bytes (~128 kbps AAC)
        val audioBytes = if (explicitAudioBytes != null && explicitAudioBytes > 0) {
            explicitAudioBytes
        } else {
            (PAIRED_AUDIO_BITRATE_KBPS * 1000L * duration) / 8L
        }

        // 2. Calculate Video Stream Bytes
        val videoBytes = if (explicitVideoBytes != null && explicitVideoBytes > 0) {
            explicitVideoBytes
        } else {
            val baseBitrate = if (variantBitrateKbps != null && variantBitrateKbps > 0) {
                variantBitrateKbps
            } else {
                getBaselineVideoBitrateKbps(height)
            }

            val efficiency = when (codec) {
                VideoCodec.AV1 -> CODEC_EFFICIENCY_AV1
                VideoCodec.VP9 -> CODEC_EFFICIENCY_VP9
                else -> CODEC_EFFICIENCY_H264
            }

            val effectiveBitrate = (baseBitrate * efficiency).toLong()
            (effectiveBitrate * 1000L * duration) / 8L
        }

        return videoBytes + audioBytes
    }

    /**
     * Calculates the estimated size in bytes for audio-only streams.
     *
     * @param targetBitrateKbps Target bitrate (e.g. 256 for High, 128 for Standard, 64 for Data Saver)
     * @param codec Target audio codec (M4A, OPUS, MP3)
     * @param durationSeconds Total media duration in seconds
     * @param explicitAudioBytes Known audio size if provided
     */
    fun calculateAudioTotalBytes(
        targetBitrateKbps: Int,
        codec: AudioCodec?,
        durationSeconds: Long?,
        explicitAudioBytes: Long? = null
    ): Long {
        val duration = if (durationSeconds != null && durationSeconds > 0) durationSeconds else NOMINAL_DURATION_SECONDS

        if (explicitAudioBytes != null && explicitAudioBytes > 0) {
            // Scale explicit bytes according to codec efficiency if converting
            return when (codec) {
                AudioCodec.OPUS -> (explicitAudioBytes * 0.75f).toLong().coerceAtLeast(500_000L)
                AudioCodec.MP3 -> (explicitAudioBytes * 1.25f).toLong().coerceAtLeast(1_000_000L)
                else -> explicitAudioBytes
            }
        }

        val effectiveKbps = when (codec) {
            AudioCodec.OPUS -> (targetBitrateKbps * 0.75f).toInt().coerceAtLeast(48)
            AudioCodec.MP3 -> (targetBitrateKbps * 1.25f).toInt().coerceAtLeast(96)
            else -> targetBitrateKbps
        }

        return (effectiveKbps * 1000L * duration) / 8L
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
