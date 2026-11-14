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
        "mp4", "mkv", "webm", "m4v", "avi", "mov", "flv", "3gp",
        "m4a", "mp3", "opus", "flac", "wav", "aac", "ogg",
        "m3u8", "mpd",
        "zip", "apk", "xapk", "apks", "pdf", "iso", "tar", "gz", "7z", "rar", "bz2", "xz",
        "exe", "dmg", "bin", "deb", "rpm", "msi", "torrent",
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"
    )

    private val directFileExtensions = setOf(
        "zip", "apk", "xapk", "apks", "pdf", "iso", "tar", "gz", "7z", "rar", "bz2", "xz",
        "exe", "dmg", "bin", "deb", "rpm", "msi", "torrent",
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"
    )

    fun isDirectFileExtension(url: String): Boolean {
        val path = runCatching { Uri.parse(url).path }.getOrNull()?.lowercase(Locale.ROOT) ?: ""
        val ext = path.substringAfterLast('.', "")
        return directFileExtensions.contains(ext)
    }

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
            return when {
                host.contains("youtube.com") -> {
                    path.contains("/watch") || path.contains("/shorts") || path.contains("/live") || path.contains("/embed")
                }
                host.contains("youtu.be") -> path.length > 1
                host.contains("tiktok.com") -> path.contains("/video/") || path.contains("/@")
                host.contains("instagram.com") -> path.contains("/p/") || path.contains("/reel/") || path.contains("/tv/")
                host.contains("twitter.com") || host.contains("x.com") -> path.contains("/status/")
                host.contains("facebook.com") -> path.contains("/watch") || path.contains("/videos/") || path.contains("/reel/")
                host.contains("reddit.com") -> path.contains("/comments/")
                host.contains("vimeo.com") || host.contains("dailymotion.com") -> {
                    path.length > 3 && !path.startsWith("/home") && !path.startsWith("/feed")
                }
                else -> path.contains("/video") || path.contains("/watch")
            }
        }

        val ext = path.substringAfterLast('.', "")
        return mediaExtensions.contains(ext)
    }

    fun cleanMediaUrl(rawUrl: String): String {
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return rawUrl
        val host = uri.host?.lowercase(Locale.ROOT) ?: ""
        if (host.contains("youtube.com") || host.contains("youtu.be")) {
            val videoId = uri.getQueryParameter("v")
            if (!videoId.isNullOrBlank()) {
                return "https://www.youtube.com/watch?v=$videoId"
            }
            if (host.contains("youtu.be")) {
                val id = uri.pathSegments.firstOrNull()
                if (!id.isNullOrBlank()) {
                    return "https://www.youtube.com/watch?v=$id"
                }
            }
            val path = uri.path ?: ""
            if (path.contains("/shorts/")) {
                val shortId = path.substringAfter("/shorts/").substringBefore('/')
                if (shortId.isNotBlank()) {
                    return "https://www.youtube.com/watch?v=$shortId"
                }
            }
        }
        return rawUrl
    }

    /**
     * Extracts full media metadata, streaming formats, and direct files using YoutubeDL
     * with an OkHttp HEAD request fallback.
     */
    suspend fun extractMedia(url: String, fallbackTitle: String? = null): DetectedPageMedia? =
        withContext(Dispatchers.IO) {
            val cleanUrl = cleanMediaUrl(url)
            val isYouTube = cleanUrl.contains("youtube.com", ignoreCase = true) || cleanUrl.contains("youtu.be", ignoreCase = true)

            // Fast-track: Direct files (apps, zip, pdf, docs, binaries) bypass yt-dlp entirely
            if (!isYouTube && isDirectFileExtension(cleanUrl)) {
                return@withContext extractDirectFileMedia(cleanUrl, fallbackTitle)
            }

            try {
                // 1. First attempt: YoutubeDL stream extraction with --no-playlist protection
                val request = com.yausername.youtubedl_android.YoutubeDLRequest(cleanUrl).apply {
                    addOption("--no-playlist")
                    addOption("--no-warnings")
                    addOption("--no-check-certificates")
                    addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                }

                val videoInfo = runCatching {
                    YoutubeDL.getInstance().getInfo(request)
                }.onFailure {
                    Log.e(TAG, "YoutubeDL stream extraction failed for $cleanUrl", it)
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
                            format.tbr > 0 && duration != null && duration > 0 -> (format.tbr * 1000L * duration) / 8L
                            format.abr > 0 && duration != null && duration > 0 -> (format.abr * 1000L * duration) / 8L
                            else -> 0L
                        }

                        // For non-HLS direct media URLs on non-YouTube sites without reported size, probe real Content-Length via HTTP
                        if (fileSize <= 0 && !isYouTube && formatUrl.startsWith("http", ignoreCase = true) && !formatUrl.contains(".m3u8", ignoreCase = true)) {
                            fileSize = probeContentLength(formatUrl)
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
                                variantBitrateKbps = variant.bitrateKbps.takeIf { it > 0 },
                                requiresAudioMerge = variant.requiresAudioMerge
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
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .head()
            .build()

        return runCatching {
            var response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful || response.code == 405) {
                response.close()
                val getReq = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .header("Range", "bytes=0-0")
                    .build()
                response = okHttpClient.newCall(getReq).execute()
            }

            response.use { resp ->
                if (!resp.isSuccessful && resp.code != 206) return null

                val contentRange = resp.header("Content-Range")
                val contentLength = resp.header("Content-Length")?.toLongOrNull()
                    ?: (if (contentRange != null && contentRange.contains("/")) {
                        contentRange.substringAfterLast("/").trim().toLongOrNull() ?: 0L
                    } else 0L)
                val contentType = resp.header("Content-Type")?.lowercase(Locale.ROOT) ?: ""
                val contentDisposition = resp.header("Content-Disposition") ?: ""

                // Extract filename cleanly
                val rawFilename = if (contentDisposition.contains("filename*=", ignoreCase = true)) {
                    contentDisposition.substringAfter("filename*=").substringAfter("''").substringBefore(";").trim()
                } else if (contentDisposition.contains("filename=", ignoreCase = true)) {
                    contentDisposition.substringAfter("filename=").substringBefore(";").replace("\"", "").trim()
                } else {
                    File(Uri.parse(url).path ?: "").name.ifEmpty { fallbackTitle ?: "downloaded_file" }
                }

                val decodedFilename = runCatching { java.net.URLDecoder.decode(rawFilename, "UTF-8") }.getOrDefault(rawFilename)
                val cleanFilename = decodedFilename.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_").trim()

                var ext = cleanFilename.substringAfterLast('.', "").lowercase(Locale.ROOT)
                if (ext.isEmpty() || ext.length > 5 || ext == cleanFilename.lowercase(Locale.ROOT)) {
                    val guessedExt = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
                    if (!guessedExt.isNullOrEmpty()) {
                        ext = guessedExt.lowercase(Locale.ROOT)
                    }
                }

                // Filter out standard non-media HTML web pages, scripts, and stylesheets
                val isHtmlOrWebPage = (contentType.contains("text/html") ||
                        contentType.contains("application/xhtml") ||
                        ext in listOf("html", "htm", "php", "jsp", "asp", "aspx", "js", "css")) &&
                        !contentDisposition.contains("attachment", ignoreCase = true)

                if (isHtmlOrWebPage) {
                    return null
                }

                val isHlsOrDash = ext in listOf("m3u8", "mpd") ||
                        contentType.contains("mpegurl") ||
                        contentType.contains("dash+xml") ||
                        url.contains(".m3u8", ignoreCase = true) ||
                        url.contains(".mpd", ignoreCase = true)

                val isVideo = isHlsOrDash || contentType.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "flv", "3gp")
                val isAudio = !isVideo && (contentType.startsWith("audio/") || ext in listOf("mp3", "m4a", "opus", "wav", "flac", "aac", "ogg"))
                val isDirectFile = !isVideo && !isAudio

                val mediaType = when {
                    isVideo -> MediaType.VIDEO
                    isAudio -> MediaType.AUDIO
                    else -> MediaType.DIRECT_FILE
                }

                val formatId = if (isHlsOrDash) "hls" else "direct"
                val qualityLabel = when {
                    isHlsOrDash -> "HLS Stream"
                    isVideo -> "Direct Video"
                    isAudio -> "Direct Audio"
                    ext in listOf("apk", "xapk", "apks") -> "Android App (APK)"
                    ext in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") -> "${ext.uppercase()} Archive"
                    ext == "pdf" -> "PDF Document"
                    ext in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt") -> "Office Document"
                    ext in listOf("iso", "dmg", "exe", "msi", "bin", "deb", "rpm") -> "Binary / Installer"
                    ext.isNotEmpty() -> "${ext.uppercase()} File"
                    else -> "Direct File"
                }

                val containerExt = when {
                    isHlsOrDash -> "mp4"
                    ext.isNotEmpty() -> ext
                    isVideo -> "mp4"
                    isAudio -> "mp3"
                    else -> "bin"
                }

                val effectiveSize = if (contentLength > 0) {
                    contentLength
                } else if (isVideo) {
                    MediaSizeCalculator.calculateVideoTotalBytes(
                        height = 720,
                        codec = VideoCodec.H264,
                        durationSeconds = 180,
                        requiresAudioMerge = false
                    )
                } else 0L

                val singleVariant = StreamVariant(
                    formatId = formatId,
                    url = url,
                    mediaType = mediaType,
                    qualityLabel = qualityLabel,
                    resolutionHeight = if (isVideo) 720 else 0,
                    bitrateKbps = 0,
                    videoCodec = if (isVideo) VideoCodec.H264 else null,
                    audioCodec = if (isAudio) AudioCodec.MP3 else null,
                    containerExt = containerExt,
                    fileSizeBytes = effectiveSize,
                    requiresAudioMerge = false
                )

                val displayTitle = if (cleanFilename.isNotBlank() && cleanFilename != "downloaded_file" && cleanFilename != "downloaded_media") {
                    if (cleanFilename.contains('.')) cleanFilename.substringBeforeLast('.') else cleanFilename
                } else {
                    fallbackTitle ?: if (isDirectFile) "Downloaded File" else "Video Media"
                }

                DetectedPageMedia(
                    pageTitle = displayTitle,
                    pageUrl = url,
                    thumbnailUrl = null,
                    durationSeconds = null,
                    videoVariants = if (isVideo) listOf(singleVariant) else emptyList(),
                    audioVariants = if (isAudio) listOf(singleVariant) else emptyList(),
                    directFileUrl = if (isDirectFile) url else null,
                    directFileVariant = if (isDirectFile) singleVariant else null
                )
            }
        }.getOrNull()
    }

    private fun probeContentLength(url: String): Long {
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return 0L
        }
        if (url.contains(".m3u8", ignoreCase = true) || url.contains(".mpd", ignoreCase = true)) {
            return 0L
        }
        return try {
            val req = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .build()
            val len = okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    resp.header("Content-Length")?.toLongOrNull() ?: 0L
                } else 0L
            }
            if (len > 0) return len

            // Fallback for servers rejecting HEAD: GET with Range: bytes=0-0
            val rangeReq = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .build()
            okHttpClient.newCall(rangeReq).execute().use { resp ->
                val contentRange = resp.header("Content-Range")
                if (contentRange != null && contentRange.contains("/")) {
                    contentRange.substringAfterLast("/").trim().toLongOrNull() ?: 0L
                } else 0L
            }
        } catch (_: Exception) {
            0L
        }
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

                    // Capture dynamic media playback on any website
                    var origPlay = HTMLMediaElement.prototype.play;
                    HTMLMediaElement.prototype.play = function() {
                        var mediaSrc = this.currentSrc || this.src;
                        if (mediaSrc && mediaSrc.startsWith('http') && window.UdmMediaBridge && window.UdmMediaBridge.onMediaFound) {
                            window.UdmMediaBridge.onMediaFound(mediaSrc);
                        }
                        return origPlay.apply(this, arguments);
                    };
                }
                var mediaElements = document.querySelectorAll('video, audio, source');
                for (var i = 0; i < mediaElements.length; i++) {
                    var src = mediaElements[i].currentSrc || mediaElements[i].src;
                    if (src && src.startsWith('http') && window.UdmMediaBridge && window.UdmMediaBridge.onMediaFound) {
                        window.UdmMediaBridge.onMediaFound(src);
                    }
                }
            })();
        """.trimIndent()
    }
}
