package com.universal.dm.domain.sniffer

import com.universal.dm.data.model.AudioCodec
import com.universal.dm.data.model.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSizeCalculatorTest {

    @Test
    fun `variantBitrateKbps is not double-discounted for AV1 or VP9`() {
        val duration = 100L
        val variantBitrateKbps = 2000 // already actual AV1 tbr
        val expectedApproxBytes = (2000 * 1000.0 * duration / 8.0) * 1.012 // ~25,300,000

        val av1Bytes = MediaSizeCalculator.calculateVideoTotalBytes(
            height = 1080,
            codec = VideoCodec.AV1,
            durationSeconds = duration,
            variantBitrateKbps = variantBitrateKbps,
            requiresAudioMerge = false
        )
        val h264Bytes = MediaSizeCalculator.calculateVideoTotalBytes(
            height = 1080,
            codec = VideoCodec.H264,
            durationSeconds = duration,
            variantBitrateKbps = variantBitrateKbps,
            requiresAudioMerge = false
        )

        assertEquals(expectedApproxBytes.toLong(), av1Bytes)
        assertEquals(av1Bytes, h264Bytes)
    }

    @Test
    fun `baseline table estimation applies codec efficiency relative to H264`() {
        val duration = 100L
        val h264Baseline = MediaSizeCalculator.calculateVideoTotalBytes(
            height = 1080,
            codec = VideoCodec.H264,
            durationSeconds = duration,
            variantBitrateKbps = null,
            requiresAudioMerge = false
        )
        val av1Baseline = MediaSizeCalculator.calculateVideoTotalBytes(
            height = 1080,
            codec = VideoCodec.AV1,
            durationSeconds = duration,
            variantBitrateKbps = null,
            requiresAudioMerge = false
        )
        val vp9Baseline = MediaSizeCalculator.calculateVideoTotalBytes(
            height = 1080,
            codec = VideoCodec.VP9,
            durationSeconds = duration,
            variantBitrateKbps = null,
            requiresAudioMerge = false
        )

        assertTrue(av1Baseline < vp9Baseline)
        assertTrue(vp9Baseline < h264Baseline)
    }

    @Test
    fun `calculateAudioTotalBytes scales cleanly without 1MB minimum distortion on short clips`() {
        val shortMp3 = MediaSizeCalculator.calculateAudioTotalBytes(
            targetBitrateKbps = 128,
            codec = AudioCodec.MP3,
            durationSeconds = 5L
        )
        // 128 * 1.15 = 147.2 kbps -> ~92 KB for 5 seconds
        assertTrue("Expected < 200KB for 5s clip, got $shortMp3", shortMp3 in 80_000L..120_000L)

        val scaledFromSource = MediaSizeCalculator.calculateAudioTotalBytes(
            targetBitrateKbps = 256,
            codec = AudioCodec.M4A,
            durationSeconds = 120L,
            explicitAudioBytes = 1_000_000L,
            sourceBitrateKbps = 128
        )
        assertEquals(2_000_000L, scaledFromSource)
    }

    @Test
    fun `computeAdaptiveThreadCount prevents zero-byte chunks on small files`() {
        assertEquals(1, MediaSizeCalculator.computeAdaptiveThreadCount(contentLength = 10L, requestedThreads = 16))
        assertEquals(1, MediaSizeCalculator.computeAdaptiveThreadCount(contentLength = 100_000L, requestedThreads = 16))
        assertEquals(2, MediaSizeCalculator.computeAdaptiveThreadCount(contentLength = 600_000L, requestedThreads = 16))
        assertEquals(16, MediaSizeCalculator.computeAdaptiveThreadCount(contentLength = 50_000_000L, requestedThreads = 16))
        assertEquals(4, MediaSizeCalculator.computeAdaptiveThreadCount(contentLength = 50_000_000L, requestedThreads = 4))
    }

    @Test
    fun `computeChunkRanges partitions contiguously with balanced remainders`() {
        val contentLength = 1003L
        val ranges = MediaSizeCalculator.computeChunkRanges(contentLength, 4)

        assertEquals(4, ranges.size)
        assertEquals(0L, ranges.first().first)
        assertEquals(contentLength - 1L, ranges.last().last)

        var expectedStart = 0L
        var totalCovered = 0L
        val lengths = mutableListOf<Long>()
        for (r in ranges) {
            assertEquals(expectedStart, r.first)
            val len = r.last - r.first + 1L
            assertTrue(len >= 1L)
            lengths.add(len)
            totalCovered += len
            expectedStart = r.last + 1L
        }

        assertEquals(contentLength, totalCovered)
        assertEquals(listOf(251L, 251L, 251L, 250L), lengths)
    }

    @Test
    fun `smoothSpeedEma and ETA calculation behave accurately`() {
        val initial = MediaSizeCalculator.smoothSpeedEma(0L, 1_000_000L)
        assertEquals(1_000_000L, initial)

        // With alpha = 0.35: 0.35 * 2_000_000 + 0.65 * 1_000_000 = 1_350_000
        val next = MediaSizeCalculator.smoothSpeedEma(1_000_000L, 2_000_000L, alpha = 0.35)
        assertEquals(1_350_000L, next)

        // Ceiling ETA: 1500 bytes at 1000 B/s = 2 seconds
        assertEquals(2L, MediaSizeCalculator.calculateEtaSeconds(downloadedBytes = 500L, totalBytes = 2000L, speedBytesPerSec = 1000L))
        assertEquals("00:02 left", MediaSizeCalculator.formatEta(2L))
        assertEquals("1h 05m left", MediaSizeCalculator.formatEta(3900L))
    }
}
