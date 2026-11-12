package com.universal.dm.domain.sniffer

import android.net.Uri
import android.util.Log
import com.universal.dm.data.model.AudioCodec
import com.universal.dm.data.model.DetectedPageMedia
import com.universal.dm.data.model.MediaType
import com.universal.dm.data.model.StreamVariant
import com.universal.dm.data.model.VideoCodec
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class MediaSniffer(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    private val supportedPlatformDomains = listOf(
        "youtube.com", "youtu.be", "vimeo.com", "dailymotion.com",
        "tiktok.com", "instagram.com", "twitter.com", "x.com",
        "facebook.com", "reddit.com", "bilibili.com", "twitch.tv"
    )

    private val mediaExtensions = setOf(
        "mp4", "mkv", "webm", "m4v", "avi", "mov", "flv",
        "m4a", "mp3", "opus", "flac", "wav", "aac", "ogg",
        "m3u8", "mpd",
        "zip", "apk", "pdf", "iso", "tar", "gz", "7z", "rar", "exe"
    )

    fun isSupportedPlatformUrl(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase(Locale.ROOT) ?: ""
        return supportedPlatformDomains.any { host.contains(it) }
    }

    fun isMediaOrDownloadableUrl(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase(Locale.ROOT) ?: ""
        val path = uri.path?.lowercase(Locale.ROOT) ?: ""

        // Filter out API, telemetry, analytics, and internal sub-requests
        if (path.contains("/api/") || path.contains("/youtubei/") || path.contains("/stats/") ||
            path.contains("/ptracking") || path.contains("/gen_204") || path.contains("/log_event")) {
            return false
        }

        if (supportedPlatformDomains.any { host.contains(it) }) {
            return path.contains("/watch") || path.contains("/video") || path.contains("/reel") ||
                    path.contains("/shorts") || path.contains("/p/") || path.contains("/status") ||
                    path.length > 2
        }

        val ext = path.substringAfterLast('.', "")
        return mediaExtensions.contains(ext)
    }

    /**
     * Extracts full media metadata, streaming formats, and direct files using YoutubeDL
     * with an OkHttp HEAD request fallback.
     */
    suspend fun extractMedia(url: String, fallbackTitle: String? = null): DetectedPageMedia? =
        withContext(Dispatchers.IO) {
            try {
                // 1. First attempt: YoutubeDL stream extraction
                val videoInfo = runCatching {
                    YoutubeDL.getInstance().getInfo(url)
                }.getOrNull()

                if (videoInfo != null && !videoInfo.formats.isNullOrEmpty()) {
                    val title = videoInfo.title ?: videoInfo.fulltitle ?: fallbackTitle ?: "Video Media"
                    val thumbnail = videoInfo.thumbnail
                    val duration = videoInfo.duration.toLong().takeIf { it > 0 }

                    val audioVariants = mutableListOf<StreamVariant>()
                    val videoVariants = mutableListOf<StreamVariant>()

                    videoInfo.formats?.forEach { format ->
                        val formatUrl = format.url ?: return@forEach
                        val vcodec = format.vcodec?.lowercase(Locale.ROOT)
                        val acodec = format.acodec?.lowercase(Locale.ROOT)
                        val ext = format.ext ?: "mp4"

                        val isAudioOnly = (vcodec == null || vcodec == "none" || format.height <= 0) &&
                                (acodec != null && acodec != "none")
                        val isVideo = vcodec != null && vcodec != "none" && format.height > 0

                        val mappedVideoCodec = when {
                            vcodec?.startsWith("av01") == true -> VideoCodec.AV1
                            vcodec?.startsWith("avc1") == true -> VideoCodec.H264
                            vcodec?.startsWith("vp9") == true || vcodec?.startsWith("vp09") == true -> VideoCodec.VP9
                            isVideo -> VideoCodec.OTHER
                            else -> null
                        }

                        val mappedAudioCodec = when {
                            acodec?.startsWith("mp4a") == true -> AudioCodec.M4A
                            acodec?.startsWith("opus") == true -> AudioCodec.OPUS
                            acodec?.startsWith("mp3") == true -> AudioCodec.MP3
                            else -> null
                        }

                        // Calculate accurate base stream size from explicit header, approximate size, or mathematical bitrate
                        var fileSize = when {
                            format.fileSize > 0 -> format.fileSize
                            format.fileSizeApproximate > 0 -> format.fileSizeApproximate
                            else -> 0L
                        }

                        if (isAudioOnly) {
                            if (fileSize <= 0) {
                                val rateKbps = if (format.abr > 0) format.abr else if (format.tbr > 0) format.tbr else 128
                                fileSize = MediaSizeCalculator.calculateAudioTotalBytes(
                                    targetBitrateKbps = rateKbps,
                                    codec = mappedAudioCodec,
                                    durationSeconds = duration
                                )
                            }
                            val qualityLabel = when {
                                format.abr > 0 -> "${format.abr} kbps"
                                format.tbr > 0 -> "${format.tbr} kbps"
                                else -> "High Quality Audio"
                            }
                            audioVariants.add(
                                StreamVariant(
                                    formatId = format.formatId ?: "audio",
                                    url = formatUrl,
                                    mediaType = MediaType.AUDIO,
                                    qualityLabel = qualityLabel,
                                    resolutionHeight = 0,
                                    bitrateKbps = if (format.abr > 0) format.abr else format.tbr,
                                    videoCodec = null,
                                    audioCodec = mappedAudioCodec ?: AudioCodec.M4A,
                                    containerExt = ext,
                                    fileSizeBytes = fileSize,
                                    requiresAudioMerge = false
                                )
                            )
                        } else if (isVideo) {
                            val requiresAudio = acodec == null || acodec == "none"
                            val qualityLabel = "${format.height}p" + (if (format.fps > 30) " ${format.fps}fps" else "")

                            videoVariants.add(
                                StreamVariant(
                                    formatId = format.formatId ?: "video",
                                    url = formatUrl,
                                    mediaType = MediaType.VIDEO,
                                    qualityLabel = qualityLabel,
                                    resolutionHeight = format.height,
                                    bitrateKbps = format.tbr,
                                    videoCodec = mappedVideoCodec,
                                    audioCodec = mappedAudioCodec,
                                    containerExt = ext,
                                    fileSizeBytes = fileSize,
                                    requiresAudioMerge = requiresAudio
                                )
                            )
                        }
                    }

                    // Pair video streams requiring audio merge with the highest quality M4A or audio stream,
                    // and mathematically add the audio stream bytes to video fileSizeBytes so that the total
                    // reflects the complete merged download size!
                    val bestAudio = audioVariants.maxByOrNull { it.bitrateKbps }
                    val finalVideoVariants = videoVariants.map { variant ->
                        val pairedAudioSize = if (variant.requiresAudioMerge && bestAudio != null) {
                            if (bestAudio.fileSizeBytes > 0) {
                                bestAudio.fileSizeBytes
                            } else {
                                MediaSizeCalculator.calculateAudioTotalBytes(
                                    targetBitrateKbps = bestAudio.bitrateKbps.coerceAtLeast(128),
                                    codec = bestAudio.audioCodec,
                                    durationSeconds = duration
                                )
                            }
                        } else 0L

                        val totalSize = if (variant.fileSizeBytes > 0) {
                            variant.fileSizeBytes + pairedAudioSize
                        } else {
                            MediaSizeCalculator.calculateVideoTotalBytes(
                                height = variant.resolutionHeight,
                                codec = variant.videoCodec,
                                durationSeconds = duration,
                                explicitAudioBytes = pairedAudioSize,
                                variantBitrateKbps = variant.bitrateKbps.takeIf { it > 0 }
                            )
                        }

                        if (variant.requiresAudioMerge && bestAudio != null) {
                            variant.copy(
                                pairedAudioStreamUrl = bestAudio.url,
                                pairedAudioFormatId = bestAudio.formatId,
                                fileSizeBytes = totalSize
                            )
                        } else {
                            variant.copy(fileSizeBytes = totalSize)
                        }
                    }.sortedByDescending { it.resolutionHeight }

                    val sortedAudioVariants = audioVariants.sortedByDescending { it.bitrateKbps }

                    if (finalVideoVariants.isNotEmpty() || sortedAudioVariants.isNotEmpty()) {
                        return@withContext DetectedPageMedia(
                            pageTitle = title,
                            pageUrl = url,
                            thumbnailUrl = thumbnail,
                            durationSeconds = duration,
                            videoVariants = finalVideoVariants,
                            audioVariants = sortedAudioVariants
                        )
                    }
                }

                // 2. Fallback: Direct link inspection via HTTP HEAD
                extractDirectFileMedia(url, fallbackTitle)
            } catch (e: Exception) {
                Log.e(TAG, "Media extraction failed for $url", e)
                extractDirectFileMedia(url, fallbackTitle)
            }
        }

    private fun extractDirectFileMedia(url: String, fallbackTitle: String?): DetectedPageMedia? {
        val request = Request.Builder()
            .url(url)
            .head()
            .build()

        return runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null

                val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
                val contentType = response.header("Content-Type")?.lowercase(Locale.ROOT) ?: ""
                val contentDisposition = response.header("Content-Disposition") ?: ""

                // Extract filename
                val filename = if (contentDisposition.contains("filename=")) {
                    contentDisposition.substringAfter("filename=").replace("\"", "").trim()
                } else {
                    File(Uri.parse(url).path ?: "").name.ifEmpty { "downloaded_file" }
                }

                val title = fallbackTitle ?: filename
                val ext = filename.substringAfterLast('.', "")

                val isVideo = contentType.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "avi", "mov")
                val isAudio = contentType.startsWith("audio/") || ext in listOf("mp3", "m4a", "opus", "wav", "flac")

                val mediaType = when {
                    isVideo -> MediaType.VIDEO
                    isAudio -> MediaType.AUDIO
                    else -> MediaType.DIRECT_FILE
                }

                val singleVariant = StreamVariant(
                    formatId = "direct",
                    url = url,
                    mediaType = mediaType,
                    qualityLabel = if (isVideo) "Direct Video" else if (isAudio) "Direct Audio" else "Direct File",
                    resolutionHeight = if (isVideo) 1080 else 0,
                    bitrateKbps = 0,
                    videoCodec = if (isVideo) VideoCodec.H264 else null,
                    audioCodec = if (isAudio) AudioCodec.MP3 else null,
                    containerExt = ext.ifEmpty { "bin" },
                    fileSizeBytes = contentLength,
                    requiresAudioMerge = false
                )

                DetectedPageMedia(
                    pageTitle = title,
                    pageUrl = url,
                    thumbnailUrl = null,
                    durationSeconds = null,
                    videoVariants = if (isVideo) listOf(singleVariant) else emptyList(),
                    audioVariants = if (isAudio) listOf(singleVariant) else emptyList(),
                    directFileUrl = url
                )
            }
        }.getOrNull()
    }

    companion object {
        private const val TAG = "MediaSniffer"

        val JS_MEDIA_SCANNER = """
            (function() {
                if (!window.__udm_spa_hooked) {
                    window.__udm_spa_hooked = true;
                    var origPush = history.pushState;
                    history.pushState = function() {
                        origPush.apply(history, arguments);
                        if (window.UdmMediaBridge && window.UdmMediaBridge.onUrlChanged) {
                            window.UdmMediaBridge.onUrlChanged(window.location.href);
                        }
                    };
                    var origReplace = history.replaceState;
                    history.replaceState = function() {
                        origReplace.apply(history, arguments);
                        if (window.UdmMediaBridge && window.UdmMediaBridge.onUrlChanged) {
                            window.UdmMediaBridge.onUrlChanged(window.location.href);
                        }
                    };
                    window.addEventListener('popstate', function() {
                        if (window.UdmMediaBridge && window.UdmMediaBridge.onUrlChanged) {
                            window.UdmMediaBridge.onUrlChanged(window.location.href);
                        }
                    });
                }
                var mediaElements = document.querySelectorAll('video, audio, source');
                for (var i = 0; i < mediaElements.length; i++) {
                    var src = mediaElements[i].currentSrc || mediaElements[i].src;
                    if (src && src.startsWith('http')) {
                        if (window.UdmMediaBridge) {
                            window.UdmMediaBridge.onMediaFound(src);
                        }
                        break;
                    }
                }
            })();
        """.trimIndent()
    }
}
