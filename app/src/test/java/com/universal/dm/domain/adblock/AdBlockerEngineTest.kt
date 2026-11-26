package com.universal.dm.domain.adblock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBlockerEngineTest {

    private val engine = AdBlockerEngine(context = null)

    @Test
    fun `domain trie blocks exact host and subdomains without false-positive suffix collisions`() {
        assertTrue(engine.isHostBlocked("doubleclick.net"))
        assertTrue(engine.isHostBlocked("ad.doubleclick.net"))
        assertTrue(engine.isHostBlocked("GOOGLEADS.G.DOUBLECLICK.NET"))
        assertTrue(engine.isHostBlocked("stats.youtube.com"))

        // Boundary safety: 'mydoubleclick.net' must NOT match 'doubleclick.net'
        assertFalse(engine.isHostBlocked("mydoubleclick.net"))
        assertFalse(engine.isHostBlocked("notads.youtube.com"))
        assertFalse(engine.isHostBlocked("www.wikipedia.org"))
    }

    @Test
    fun `shouldBlockUrl blocks ad query params and paths while sparing legitimate videoplayback`() {
        assertTrue(engine.shouldBlockUrl("https://example.com/script.js?ad_type=video"))
        assertTrue(engine.shouldBlockUrl("https://example.com/page?foo=1&adformat=15_1"))
        assertTrue(engine.shouldBlockUrl("https://www.youtube.com/api/stats/ads?ns=yt"))
        assertTrue(engine.shouldBlockUrl("https://www.youtube.com/ptracking?pltype=content"))

        // Legitimate YouTube CDN media chunk must NOT be blocked even if path contains matched substrings
        assertFalse(engine.shouldBlockUrl("https://rr3---sn-abc.googlevideo.com/videoplayback?expire=1700&itag=137"))
        // Videoplayback WITH adformat= MUST be blocked
        assertTrue(engine.shouldBlockUrl("https://rr3---sn-abc.googlevideo.com/videoplayback?adformat=15_2&itag=137"))
    }
}
