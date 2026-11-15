package com.universal.dm

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.universal.dm", appContext.packageName)
    }

    @Test
    fun testYouTubeExtraction() = kotlinx.coroutines.runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        UdmApplication.ensureYoutubeDlInitialized(appContext)

        val cleanUrl = "https://www.youtube.com/watch?v=qisu2_ZouI8"
        val sniffer = com.universal.dm.domain.sniffer.MediaSniffer()
        val media = sniffer.extractMedia(cleanUrl)

        assertNotNull("Expected media to be detected", media)
        android.util.Log.d("TestExtraction", "SUCCESS: Title='${media!!.pageTitle}', Videos=${media.videoVariants.size}, Audios=${media.audioVariants.size}")
        media.videoVariants.take(8).forEach {
            android.util.Log.d("TestExtraction", "   Video Variant: label=${it.qualityLabel}, ${it.resolutionHeight}p, codec=${it.videoCodec}, size=${it.fileSizeBytes}, merge=${it.requiresAudioMerge}")
        }
        media.audioVariants.take(5).forEach {
            android.util.Log.d("TestExtraction", "   Audio Variant: label=${it.qualityLabel}, codec=${it.audioCodec}, size=${it.fileSizeBytes}")
        }

        assertTrue("Expected video variants", media.videoVariants.isNotEmpty())
        assertTrue("Expected audio variants", media.audioVariants.isNotEmpty())
    }
}