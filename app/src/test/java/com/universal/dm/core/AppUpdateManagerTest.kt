package com.universal.dm.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun `normalizeVersion strips prefixes and build metadata`() {
        assertEquals("3.5", AppUpdateManager.normalizeVersion("v3.5"))
        assertEquals("3.10.2", AppUpdateManager.normalizeVersion("V3.10.2"))
        assertEquals("3.6", AppUpdateManager.normalizeVersion("v3.6-beta.1+build5"))
    }

    @Test
    fun `isNewerVersion handles semantic and double digit segments`() {
        assertTrue(AppUpdateManager.isNewerVersion("v3.6", "3.5"))
        assertTrue(AppUpdateManager.isNewerVersion("V3.10", "v3.5"))
        assertTrue(AppUpdateManager.isNewerVersion("4.0.0", "3.9.9"))
        assertTrue(AppUpdateManager.isNewerVersion("3.5.1", "3.5"))

        assertFalse(AppUpdateManager.isNewerVersion("v3.5", "3.5"))
        assertFalse(AppUpdateManager.isNewerVersion("3.5.0", "v3.5"))
        assertFalse(AppUpdateManager.isNewerVersion("v3.1", "3.5"))
        assertFalse(AppUpdateManager.isNewerVersion("V1.0", "3.5"))
    }

    @Test
    fun `parseReleaseJson extracts release metadata and apk asset`() {
        val sampleJson = """
            {
              "tag_name": "v3.6",
              "name": "UDM v3.6 Release",
              "body": "Faster downloads and in-app updater.",
              "html_url": "https://github.com/bijoykumarmistri/UDM/releases/tag/v3.6",
              "published_at": "2026-11-27T10:00:00Z",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "name": "checksums.txt",
                  "content_type": "text/plain",
                  "size": 128,
                  "browser_download_url": "https://github.com/bijoykumarmistri/UDM/releases/download/v3.6/checksums.txt"
                },
                {
                  "name": "UDM-v3.6.apk",
                  "content_type": "application/vnd.android.package-archive",
                  "size": 255968580,
                  "browser_download_url": "https://github.com/bijoykumarmistri/UDM/releases/download/v3.6/UDM-v3.6.apk"
                }
              ]
            }
        """.trimIndent()

        val release = AppUpdateManager.parseReleaseJson(sampleJson)
        assertNotNull(release)
        release!!
        assertEquals("v3.6", release.tagName)
        assertEquals("3.6", release.versionName)
        assertEquals("UDM v3.6 Release", release.title)
        assertEquals("Faster downloads and in-app updater.", release.releaseNotes)
        assertEquals("https://github.com/bijoykumarmistri/UDM/releases/tag/v3.6", release.htmlUrl)
        assertEquals("UDM-v3.6.apk", release.apkFileName)
        assertEquals(255968580L, release.apkSizeBytes)
        assertEquals(
            "https://github.com/bijoykumarmistri/UDM/releases/download/v3.6/UDM-v3.6.apk",
            release.apkDownloadUrl
        )
    }

    @Test
    fun `parseReleaseListJson skips drafts and prereleases and picks highest version`() {
        val arrayJson = """
            [
              {
                "tag_name": "v4.0-rc1",
                "name": "Prerelease",
                "draft": false,
                "prerelease": true,
                "assets": []
              },
              {
                "tag_name": "v3.1",
                "name": "UDM v3.1",
                "draft": false,
                "prerelease": false,
                "html_url": "https://github.com/bijoykumarmistri/UDM/releases/tag/v3.1",
                "assets": [
                  {
                    "name": "UDM-v3.1.apk",
                    "size": 255968580,
                    "browser_download_url": "https://github.com/bijoykumarmistri/UDM/releases/download/v3.1/UDM-v3.1.apk"
                  }
                ]
              },
              {
                "tag_name": "v3.0",
                "name": "UDM v3.0",
                "draft": false,
                "prerelease": false,
                "assets": []
              }
            ]
        """.trimIndent()

        val best = AppUpdateManager.parseReleaseListJson(arrayJson)
        assertNotNull(best)
        assertEquals("v3.1", best?.tagName)
        assertEquals("UDM-v3.1.apk", best?.apkFileName)
    }

    @Test
    fun `parseReleaseJson ignores draft releases`() {
        val draftJson = """
            {
              "tag_name": "v3.9",
              "draft": true,
              "assets": []
            }
        """.trimIndent()

        assertNull(AppUpdateManager.parseReleaseJson(draftJson))
    }
}
