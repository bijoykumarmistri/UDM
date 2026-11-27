package com.universal.dm.domain.sniffer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSnifferTest {

    private val sniffer = MediaSniffer()

    @Test
    fun `domain boundary matching does not hijack dropbox or netflix via x_com substring`() {
        assertFalse(sniffer.isSupportedPlatformUrl("https://www.dropbox.com/s/123/file.zip"))
        assertFalse(sniffer.isSupportedPlatformUrl("https://netflix.com/watch/123"))
        assertFalse(sniffer.isSupportedPlatformUrl("https://app.box.com/s/abc"))
        assertFalse(sniffer.isSupportedPlatformUrl("https://myyoutube.com/watch?v=123"))

        // Direct files on dropbox/box MUST still be detected as downloadable!
        assertTrue(sniffer.isMediaOrDownloadableUrl("https://www.dropbox.com/s/123/archive.zip"))
        assertTrue(sniffer.isMediaOrDownloadableUrl("https://dl.dropboxusercontent.com/u/1/video.mp4"))
        assertTrue(sniffer.isMediaOrDownloadableUrl("https://app.box.com/shared/installer.apk"))
    }

    @Test
    fun `supported platform URLs match subdomains properly`() {
        assertTrue(sniffer.isSupportedPlatformUrl("https://x.com/nasa/status/123456"))
        assertTrue(sniffer.isSupportedPlatformUrl("https://mobile.twitter.com/nasa/status/123456"))
        assertTrue(sniffer.isSupportedPlatformUrl("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(sniffer.isSupportedPlatformUrl("https://youtu.be/dQw4w9WgXcQ"))

        assertTrue(sniffer.isMediaOrDownloadableUrl("https://x.com/nasa/status/123456"))
        assertTrue(sniffer.isMediaOrDownloadableUrl("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(sniffer.isMediaOrDownloadableUrl("https://www.youtube.com/shorts/abc123"))
        assertFalse(sniffer.isMediaOrDownloadableUrl("https://www.youtube.com/youtubei/v1/player"))
        assertFalse(sniffer.isMediaOrDownloadableUrl("https://www.youtube.com/api/stats/playback"))
    }

    @Test
    fun `cleanMediaUrl canonicalizes YouTube formats without touching unrelated domains`() {
        assertEquals(
            "https://www.youtube.com/watch?v=abc123XYZ",
            sniffer.cleanMediaUrl("https://m.youtube.com/watch?list=PL1&v=abc123XYZ&t=42s")
        )
        assertEquals(
            "https://www.youtube.com/watch?v=abc123XYZ",
            sniffer.cleanMediaUrl("https://youtu.be/abc123XYZ?si=tracking")
        )
        assertEquals(
            "https://www.youtube.com/watch?v=shortId99",
            sniffer.cleanMediaUrl("https://www.youtube.com/shorts/shortId99?feature=share")
        )
        assertEquals(
            "https://www.dropbox.com/s/file.zip?dl=1",
            sniffer.cleanMediaUrl("https://www.dropbox.com/s/file.zip?dl=1")
        )
    }

    @Test
    fun `extractYoutubeVideoId extracts ID across watch shorts live embed and shortlinks`() {
        assertEquals("dQw4w9WgXcQ", sniffer.extractYoutubeVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", sniffer.extractYoutubeVideoId("https://m.youtube.com/watch?list=PL1&v=dQw4w9WgXcQ&t=10"))
        assertEquals("dQw4w9WgXcQ", sniffer.extractYoutubeVideoId("https://youtu.be/dQw4w9WgXcQ?si=xyz"))
        assertEquals("shortId99", sniffer.extractYoutubeVideoId("https://www.youtube.com/shorts/shortId99"))
        assertEquals("liveId88", sniffer.extractYoutubeVideoId("https://www.youtube.com/live/liveId88"))
        assertEquals("embedId77", sniffer.extractYoutubeVideoId("https://www.youtube.com/embed/embedId77"))
        assertEquals(null, sniffer.extractYoutubeVideoId("https://vimeo.com/123456"))
    }
}
