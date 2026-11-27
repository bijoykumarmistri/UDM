package com.universal.dm.domain.sniffer

import android.util.Log
import android.util.LruCache
import com.universal.dm.core.NetworkClient
import com.universal.dm.data.model.AudioCodec
import com.universal.dm.data.model.DetectedPageMedia
import com.universal.dm.data.model.MediaType
import com.universal.dm.data.model.StreamVariant
import com.universal.dm.data.model.VideoCodec
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

class MediaSniffer(
    private val okHttpClient: OkHttpClient = NetworkClient.client
) {

    private val mediaCache = LruCache<String, DetectedPageMedia>(64)

    fun getCachedMedia(url: String): DetectedPageMedia? {
        val cleanUrl = cleanMediaUrl(url)
        return mediaCache.get(cleanUrl) ?: if (cleanUrl != url) mediaCache.get(url) else null
    }

    internal data class UrlParts(
        val scheme: String,
        val host: String,
        val path: String,
        val query: String
    )

    internal fun parseUrlParts(rawUrl: String): UrlParts? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return null
        val schemeIdx = trimmed.indexOf("://")
        if (schemeIdx <= 0) return null
        val scheme = trimmed.substring(0, schemeIdx).lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") return null

        val afterScheme = trimmed.substring(schemeIdx + 3)
        val fragIdx = afterScheme.indexOf('#')
        val withoutFrag = if (fragIdx >= 0) afterScheme.substring(0, fragIdx) else afterScheme

        val queryIdx = withoutFrag.indexOf('?')
        val authorityAndPath = if (queryIdx >= 0) withoutFrag.substring(0, queryIdx) else withoutFrag
        val query = if (queryIdx >= 0) withoutFrag.substring(queryIdx + 1) else ""

        val slashIdx = authorityAndPath.indexOf('/')
        val rawAuthority = if (slashIdx >= 0) authorityAndPath.substring(0, slashIdx) else authorityAndPath
        val path = if (slashIdx >= 0) authorityAndPath.substring(slashIdx) else ""

        // Strip optional userinfo (`user@host`) and port (`host:8080`)
        val withoutUserInfo = rawAuthority.substringAfterLast('@')
        val host = if (withoutUserInfo.startsWith('[')) {
            withoutUserInfo.substringBefore(']') + "]"
        } else {
            withoutUserInfo.substringBefore(':')
        }.lowercase(Locale.ROOT).trimEnd('.')

        if (host.isEmpty()) return null
        return UrlParts(scheme = scheme, host = host, path = path, query = query)
    }

    internal fun hostMatchesDomain(host: String, domain: String): Boolean {
        return host == domain || host.endsWith(".$domain")
    }

    private fun extractYoutubeVideoIdParam(query: String): String? {
        if (query.isEmpty()) return null
        var start = 0
        while (start < query.length) {
            val amp = query.indexOf('&', start)
            val pair = if (amp >= 0) query.substring(start, amp) else query.substring(start)
            val eq = pair.indexOf('=')
            if (eq == 1 && pair[0] == 'v') {
                return pair.substring(2).takeIf { it.isNotBlank() }
            }
            if (amp < 0) break
            start = amp + 1
        }
        return null
    }

    fun isDirectFileExtension(url: String): Boolean {
        val path = parseUrlParts(url)?.path?.lowercase(Locale.ROOT) ?: return false
        val fileName = path.substringAfterLast('/', "")
        val ext = fileName.substringAfterLast('.', "")
        return directFileExtensions.contains(ext)
    }

    fun isSupportedPlatformUrl(url: String): Boolean {
        val host = parseUrlParts(url)?.host ?: return false
        return supportedPlatformDomains.any { hostMatchesDomain(host, it) }
    }

    fun isMediaOrDownloadableUrl(url: String): Boolean {
        val parts = parseUrlParts(url) ?: return false
        val host = parts.host
        val path = parts.path.lowercase(Locale.ROOT)

        // Filter out API, telemetry, analytics, and internal sub-requests
        if (path.contains("/api/") || path.contains("/youtubei/") || path.contains("/stats/") ||
            path.contains("/ptracking") || path.contains("/gen_204") || path.contains("/log_event")
        ) {
            return false
        }

        if (supportedPlatformDomains.any { hostMatchesDomain(host, it) }) {
            return when {
                hostMatchesDomain(host, "youtube.com") -> {
                    path.contains("/watch") || path.contains("/shorts") || path.contains("/live") || path.contains("/embed")
                }
                hostMatchesDomain(host, "youtu.be") -> path.length > 1
                hostMatchesDomain(host, "tiktok.com") -> path.contains("/video/") || path.contains("/@")
                hostMatchesDomain(host, "instagram.com") -> path.contains("/p/") || path.contains("/reel/") || path.contains("/tv/")
                hostMatchesDomain(host, "twitter.com") || hostMatchesDomain(host, "x.com") -> path.contains("/status/")
                hostMatchesDomain(host, "facebook.com") -> path.contains("/watch") || path.contains("/videos/") || path.contains("/reel/")
                hostMatchesDomain(host, "reddit.com") -> path.contains("/comments/")
                hostMatchesDomain(host, "vimeo.com") || hostMatchesDomain(host, "dailymotion.com") -> {
                    path.length > 3 && !path.startsWith("/home") && !path.startsWith("/feed")
                }
                hostMatchesDomain(host, "soundcloud.com") -> {
                    val trimmed = path.trim('/')
                    val firstSeg = trimmed.substringBefore('/')
                    trimmed.contains('/') &&
                        firstSeg !in setOf("discover", "search", "stream", "charts", "you", "settings", "feed", "pages", "terms-of-use", "login", "popular")
                }
                else -> path.contains("/video") || path.contains("/watch")
            }
        }

        val fileName = path.substringAfterLast('/', "")
        if (fileName.startsWith("init.", ignoreCase = true) ||
            fileName.startsWith("seg-", ignoreCase = true) ||
            fileName.startsWith("chunk-", ignoreCase = true)
        ) {
            return false
        }
        val ext = fileName.substringAfterLast('.', "")
        return mediaExtensions.contains(ext)
    }

    internal fun extractYoutubeVideoId(rawUrl: String): String? {
        val parts = parseUrlParts(rawUrl) ?: return null
        val host = parts.host
        if (!hostMatchesDomain(host, "youtube.com") && !hostMatchesDomain(host, "youtu.be")) {
            return null
        }

        val queryId = extractYoutubeVideoIdParam(parts.query)
        if (!queryId.isNullOrBlank()) return queryId

        if (hostMatchesDomain(host, "youtu.be")) {
            val id = parts.path.trimStart('/').substringBefore('/')
            if (id.isNotBlank()) return id
        }

        val path = parts.path
        val marker = when {
            path.contains("/shorts/") -> "/shorts/"
            path.contains("/live/") -> "/live/"
            path.contains("/embed/") -> "/embed/"
            else -> null
        }
        if (marker != null) {
            val extractedId = path.substringAfter(marker).substringBefore('/').substringBefore('?')
            if (extractedId.isNotBlank()) return extractedId
        }
        return null
    }

    fun cleanMediaUrl(rawUrl: String): String {
        val videoId = extractYoutubeVideoId(rawUrl)
        if (!videoId.isNullOrBlank()) {
            return "https://www.youtube.com/watch?v=$videoId"
        }
        return rawUrl
    }

    fun getYoutubeVideoId(rawUrl: String): String? = extractYoutubeVideoId(rawUrl)

    /**
     * Fast YouTube InnerTube resolution without falling back to Python/yt-dlp.
     * Used by UdmDownloadWorker when a YouTube watch URL is enqueued manually
     * so playback streams and cover thumbnail start in ~300ms.
     */
    suspend fun resolveYoutubeFast(url: String, fallbackTitle: String? = null): DetectedPageMedia? =
        withContext(Dispatchers.IO) {
            val cleanUrl = cleanMediaUrl(url)
            getCachedMedia(cleanUrl)?.let { return@withContext it }
            val videoId = extractYoutubeVideoId(cleanUrl) ?: return@withContext null
            extractYoutubeViaInnerTube(
                videoId = videoId,
                pageUrl = url,
                fallbackTitle = fallbackTitle
            )?.also { media ->
                mediaCache.put(cleanUrl, media)
                if (cleanUrl != url) mediaCache.put(url, media)
            }
        }

    /**
     * Extracts full media metadata, streaming formats, and direct files.
     *
     * For YouTube URLs, uses a fast native Kotlin InnerTube API extractor (~250-600ms)
     * before falling back to YoutubeDL (which spawns a Python process) only when needed
     * (e.g. age-restricted or cookie-protected videos).
     */
    suspend fun extractMedia(url: String, fallbackTitle: String? = null): DetectedPageMedia? =
        withContext(Dispatchers.IO) {
            val cleanUrl = cleanMediaUrl(url)

            // Cache hit check (0ms return for visited / already extracted videos or files)
            getCachedMedia(cleanUrl)?.let { return@withContext it }

            val cleanHost = parseUrlParts(cleanUrl)?.host ?: ""
            val isYouTube = hostMatchesDomain(cleanHost, "youtube.com") || hostMatchesDomain(cleanHost, "youtu.be")

            // Fast-track: Direct files (apps, zip, pdf, docs, binaries) bypass yt-dlp entirely
            if (!isYouTube && isDirectFileExtension(cleanUrl)) {
                return@withContext extractDirectFileMedia(cleanUrl, fallbackTitle)
            }

            // Tier 1 Fast Path for YouTube: Direct InnerTube API via OkHttp (zero Python startup / JS decipher overhead)
            if (isYouTube) {
                val videoId = extractYoutubeVideoId(cleanUrl)
                if (!videoId.isNullOrBlank()) {
                    val startedAt = System.currentTimeMillis()
                    val innerTubeMedia = extractYoutubeViaInnerTube(
                        videoId = videoId,
                        pageUrl = url,
                        fallbackTitle = fallbackTitle
                    )
                    if (innerTubeMedia != null) {
                        Log.d(
                            TAG,
                            "Fast InnerTube extraction succeeded in ${System.currentTimeMillis() - startedAt}ms for $videoId " +
                                "(video=${innerTubeMedia.videoVariants.size}, audio=${innerTubeMedia.audioVariants.size})"
                        )
                        mediaCache.put(cleanUrl, innerTubeMedia)
                        if (cleanUrl != url) mediaCache.put(url, innerTubeMedia)
                        return@withContext innerTubeMedia
                    }
                    Log.d(
                        TAG,
                        "Fast InnerTube extraction did not yield usable streams (${System.currentTimeMillis() - startedAt}ms); falling back to YoutubeDL"
                    )
                }
            }

            try {
                com.universal.dm.UdmApplication.ensureYoutubeDlInitialized(com.universal.dm.UdmApplication.instance)

                // Tier 2: Native yt-dlp extractor (skipped heavy web JS player parsing for YouTube)
                val request = YoutubeDLRequest(cleanUrl).apply {
                    addOption("--no-playlist")
                    addOption("--no-warnings")
                    addOption("--no-check-certificates")
                    addOption("--geo-bypass")
                    addOption("--skip-download")
                    addOption("--no-call-home")
                    addOption("--socket-timeout", "8")
                    if (isYouTube) {
                        addOption(
                            "--extractor-args",
                            "youtube:skip=translated_subs"
                        )
                    }
                }

                var videoInfo = runCatching {
                    YoutubeDL.getInstance().getInfo(request)
                }.onFailure {
                    Log.w(TAG, "Primary YoutubeDL extraction failed for $cleanUrl", it)
                }.getOrNull()

                // Fallback attempt: If primary was challenged or failed, pass cookies from browser session if present
                if ((videoInfo == null || videoInfo.formats.isNullOrEmpty()) && isYouTube) {
                    val cookies = runCatching {
                        android.webkit.CookieManager.getInstance().getCookie("https://www.youtube.com")
                    }.getOrNull()

                    if (!cookies.isNullOrBlank()) {
                        val fallbackRequest = YoutubeDLRequest(cleanUrl).apply {
                            addOption("--no-playlist")
                            addOption("--no-warnings")
                            addOption("--no-check-certificates")
                            addOption("--geo-bypass")
                            addOption("--skip-download")
                            addOption("--no-call-home")
                            addOption("--socket-timeout", "8")
                            addOption(
                                "--extractor-args",
                                "youtube:player_client=android_vr,ios,android;player_skip=webpage,configs,js;skip=translated_subs,hls,dash"
                            )
                            addOption("--add-header", "Cookie: $cookies")
                        }

                        videoInfo = runCatching {
                            YoutubeDL.getInstance().getInfo(fallbackRequest)
                        }.onFailure {
                            Log.e(TAG, "Cookie-backed YoutubeDL extraction failed for $cleanUrl", it)
                        }.getOrNull()
                    }
                }

                Log.d(TAG, "Extraction result for $cleanUrl: videoInfo != null: ${videoInfo != null}, formats=${videoInfo?.formats?.size ?: 0}")

                if (videoInfo != null && !videoInfo.formats.isNullOrEmpty()) {
                    val title = videoInfo.title ?: videoInfo.fulltitle ?: fallbackTitle ?: "Video Media"
                    val thumbnail = videoInfo.thumbnail
                    val duration = videoInfo.duration.toLong().takeIf { it > 0 }

                    val audioVariants = mutableListOf<StreamVariant>()
                    val videoVariants = mutableListOf<StreamVariant>()

                    videoInfo.formats?.forEachIndexed { index, format ->
                        val vcodec = format.vcodec?.lowercase(Locale.ROOT)
                        val acodec = format.acodec?.lowercase(Locale.ROOT)
                        val ext = format.ext ?: "mp4"

                        // Ignore storyboard thumbnail frame spritesheets (sb0-sb3, ext=mhtml, or tracks with no audio and no video)
                        val isStoryboard = format.formatId?.startsWith("sb") == true || ext == "mhtml" ||
                            ((vcodec == null || vcodec == "none") && (acodec == null || acodec == "none"))
                        if (isStoryboard) return@forEachIndexed

                        val formatUrl = format.url ?: format.manifestUrl ?: cleanUrl

                        val parsedHeight = if (format.height > 0) {
                            format.height
                        } else {
                            HEIGHT_NOTE_REGEX.find(format.formatNote ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        }

                        val isAudioOnly = (vcodec == null || vcodec == "none" || parsedHeight <= 0) &&
                            (acodec != null && acodec != "none")
                        val isVideo = (vcodec != null && vcodec != "none") || parsedHeight > 0

                        if (index < 10) {
                            Log.d(TAG, "Format[$index]: id=${format.formatId}, urlNull=${format.url == null}, vcodec=$vcodec, acodec=$acodec, height=$parsedHeight, isVideo=$isVideo, isAudio=$isAudioOnly")
                        }

                        val mappedVideoCodec = when {
                            vcodec?.startsWith("av01") == true -> VideoCodec.AV1
                            vcodec?.startsWith("avc1") == true || vcodec?.startsWith("h264") == true -> VideoCodec.H264
                            vcodec?.startsWith("vp9") == true || vcodec?.startsWith("vp09") == true || vcodec?.startsWith("vp8") == true -> VideoCodec.VP9
                            isVideo -> VideoCodec.OTHER
                            else -> null
                        }

                        val mappedAudioCodec = when {
                            acodec?.startsWith("mp4a") == true || acodec?.startsWith("aac") == true -> AudioCodec.M4A
                            acodec?.startsWith("opus") == true -> AudioCodec.OPUS
                            acodec?.startsWith("mp3") == true -> AudioCodec.MP3
                            else -> null
                        }

                        // Calculate accurate base stream size from explicit header, approximate size, or mathematical bitrate
                        var fileSize = when {
                            format.fileSize > 0 -> format.fileSize
                            format.fileSizeApproximate > 0 -> format.fileSizeApproximate
                            format.tbr > 0 && duration != null && duration > 0 ->
                                ((format.tbr.toDouble() * 1000.0 * duration) / 8.0).roundToLong()
                            format.abr > 0 && duration != null && duration > 0 ->
                                ((format.abr.toDouble() * 1000.0 * duration) / 8.0).roundToLong()
                            else -> 0L
                        }

                        if (isAudioOnly) {
                            val rateKbps = if (format.abr > 0) format.abr else if (format.tbr > 0) format.tbr else 128
                            if (fileSize <= 0) {
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
                                    bitrateKbps = rateKbps,
                                    videoCodec = null,
                                    audioCodec = mappedAudioCodec ?: AudioCodec.M4A,
                                    containerExt = ext,
                                    fileSizeBytes = fileSize,
                                    requiresAudioMerge = false
                                )
                            )
                        } else if (isVideo) {
                            val requiresAudio = acodec == null || acodec == "none"
                            val qualityLabel = (if (parsedHeight > 0) "${parsedHeight}p" else "Video") +
                                (if (format.fps > 30) " ${format.fps}fps" else "")

                            videoVariants.add(
                                StreamVariant(
                                    formatId = format.formatId ?: "video",
                                    url = formatUrl,
                                    mediaType = MediaType.VIDEO,
                                    qualityLabel = qualityLabel,
                                    resolutionHeight = parsedHeight,
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

                    // Deduplicate audio variants by (audioCodec, bitrateKbps), preserving largest known file size
                    val dedupedAudioVariants = audioVariants
                        .groupBy { Pair(it.audioCodec, it.bitrateKbps) }
                        .values
                        .map { group -> group.maxByOrNull { it.fileSizeBytes } ?: group.first() }
                        .sortedByDescending { it.bitrateKbps }

                    // Deduplicate video variants by (resolutionHeight, videoCodec, qualityLabel), preferring higher bitrate/size
                    val dedupedVideoVariants = videoVariants
                        .groupBy { Triple(it.resolutionHeight, it.videoCodec, it.qualityLabel) }
                        .values
                        .map { group ->
                            group.maxWithOrNull(
                                compareBy<StreamVariant> { it.bitrateKbps }
                                    .thenBy { it.fileSizeBytes }
                                    .thenBy { !it.requiresAudioMerge }
                            ) ?: group.first()
                        }

                    val finalVideoVariants = finalizeVideoVariants(dedupedVideoVariants, dedupedAudioVariants, duration)

                    Log.d(TAG, "Processed variants for $url: video=${finalVideoVariants.size}, audio=${dedupedAudioVariants.size}")

                    if (finalVideoVariants.isNotEmpty() || dedupedAudioVariants.isNotEmpty()) {
                        val media = DetectedPageMedia(
                            pageTitle = title,
                            pageUrl = url,
                            thumbnailUrl = thumbnail,
                            durationSeconds = duration,
                            videoVariants = finalVideoVariants,
                            audioVariants = dedupedAudioVariants
                        )
                        mediaCache.put(cleanUrl, media)
                        if (cleanUrl != url) mediaCache.put(url, media)
                        return@withContext media
                    }
                }

                // 2. Fallback: Direct link inspection via HTTP HEAD
                extractDirectFileMedia(url, fallbackTitle)
            } catch (e: Exception) {
                Log.e(TAG, "Media extraction failed for $url", e)
                extractDirectFileMedia(url, fallbackTitle)
            }
        }

    private data class InnerTubeClientProfile(
        val name: String,
        val version: String,
        val userAgent: String,
        val clientId: String,
        val buildContext: (String) -> JSONObject
    )

    private fun extractYoutubeViaInnerTube(
        videoId: String,
        pageUrl: String,
        fallbackTitle: String?
    ): DetectedPageMedia? {
        val fastClient = okHttpClient.newBuilder()
            .callTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val profiles = listOf(
            InnerTubeClientProfile(
                name = "ANDROID_VR",
                version = "1.60.19",
                userAgent = ANDROID_VR_USER_AGENT,
                clientId = "28",
                buildContext = { id ->
                    JSONObject().apply {
                        put("videoId", id)
                        put("contentCheckOk", true)
                        put("racyCheckOk", true)
                        put(
                            "context",
                            JSONObject().put(
                                "client",
                                JSONObject().apply {
                                    put("clientName", "ANDROID_VR")
                                    put("clientVersion", "1.60.19")
                                    put("deviceMake", "Oculus")
                                    put("deviceModel", "Quest 3")
                                    put("osName", "Android")
                                    put("osVersion", "12L")
                                    put("androidSdkVersion", 32)
                                    put("hl", "en")
                                    put("gl", "US")
                                }
                            )
                        )
                    }
                }
            ),
            InnerTubeClientProfile(
                name = "IOS",
                version = "20.10.4",
                userAgent = IOS_USER_AGENT,
                clientId = "5",
                buildContext = { id ->
                    JSONObject().apply {
                        put("videoId", id)
                        put("contentCheckOk", true)
                        put("racyCheckOk", true)
                        put(
                            "context",
                            JSONObject().put(
                                "client",
                                JSONObject().apply {
                                    put("clientName", "IOS")
                                    put("clientVersion", "20.10.4")
                                    put("deviceMake", "Apple")
                                    put("deviceModel", "iPhone16,2")
                                    put("osName", "iPhone")
                                    put("osVersion", "18.3.2.22D82")
                                    put("hl", "en")
                                    put("gl", "US")
                                }
                            )
                        )
                    }
                }
            )
        )

        for (profile in profiles) {
            val media = runCatching {
                val body = profile.buildContext(videoId).toString().toRequestBody(JSON_MEDIA_TYPE)
                val req = Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                    .post(body)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", profile.userAgent)
                    .header("X-YouTube-Client-Name", profile.clientId)
                    .header("X-YouTube-Client-Version", profile.version)
                    .build()

                fastClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val rawJson = resp.body?.string() ?: return@use null
                    parseInnerTubePlayerResponse(rawJson, pageUrl, fallbackTitle)
                }
            }.onFailure {
                Log.d(TAG, "InnerTube ${profile.name} request failed for $videoId: ${it.message}")
            }.getOrNull()

            if (media != null) {
                return media
            }
        }
        return null
    }

    internal fun parseInnerTubePlayerResponse(
        rawJson: String,
        pageUrl: String,
        fallbackTitle: String?
    ): DetectedPageMedia? {
        val root = runCatching { JSONObject(rawJson) }.getOrNull() ?: return null
        val playabilityStatus = root.optJSONObject("playabilityStatus")?.optString("status")
        if (playabilityStatus != null && playabilityStatus != "OK") {
            return null
        }

        val streamingData = root.optJSONObject("streamingData") ?: return null
        val videoDetails = root.optJSONObject("videoDetails")

        val title = videoDetails?.optString("title")?.takeIf { it.isNotBlank() }
            ?: fallbackTitle
            ?: "YouTube Video"

        var duration = videoDetails?.optString("lengthSeconds")?.toLongOrNull()?.takeIf { it > 0 }
        val thumbnails = videoDetails?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val thumbnailUrl = thumbnails?.let { arr ->
            (arr.length() - 1 downTo 0)
                .mapNotNull { idx -> arr.optJSONObject(idx)?.optString("url")?.takeIf { it.isNotBlank() } }
                .firstOrNull()
        }

        val rawAudioVariants = mutableListOf<StreamVariant>()
        val rawVideoVariants = mutableListOf<StreamVariant>()

        fun processFormatsArray(array: JSONArray?, isAdaptive: Boolean) {
            if (array == null) return
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val directUrl = item.optString("url").takeIf { it.isNotBlank() } ?: continue
                val mimeType = item.optString("mimeType").lowercase(Locale.ROOT)
                if (mimeType.isBlank()) continue

                if (duration == null) {
                    val approxMs = item.optString("approxDurationMs").toLongOrNull() ?: item.optLong("approxDurationMs", 0L)
                    if (approxMs > 0L) duration = (approxMs / 1000L).takeIf { it > 0 }
                }

                val mimeBase = mimeType.substringBefore(';').trim()
                val codecsStr = CODECS_REGEX.find(mimeType)?.groupValues?.getOrNull(1)?.lowercase(Locale.ROOT).orEmpty()
                val isAudioOnly = mimeBase.startsWith("audio/")
                val isVideo = mimeBase.startsWith("video/")
                if (!isAudioOnly && !isVideo) continue

                val containerExt = when (mimeBase) {
                    "video/mp4" -> "mp4"
                    "video/webm" -> "webm"
                    "audio/mp4" -> "m4a"
                    "audio/webm" -> "webm"
                    else -> mimeBase.substringAfter('/', "mp4")
                }

                val itag = item.optInt("itag", 0)
                val formatId = if (itag > 0) itag.toString() else if (isAudioOnly) "audio" else "video"

                val avgBitrateBps = item.optInt("averageBitrate", 0)
                val peakBitrateBps = item.optInt("bitrate", 0)
                val bitrateKbps = ((if (avgBitrateBps > 0) avgBitrateBps else peakBitrateBps) / 1000).coerceAtLeast(0)

                var fileSize = item.optString("contentLength").toLongOrNull()
                    ?: item.optLong("contentLength", 0L)
                if (fileSize <= 0L && bitrateKbps > 0 && duration != null && duration!! > 0L) {
                    fileSize = ((bitrateKbps.toDouble() * 1000.0 * duration!!) / 8.0).roundToLong()
                }

                val mappedVideoCodec = when {
                    codecsStr.contains("av01") -> VideoCodec.AV1
                    codecsStr.contains("avc1") || codecsStr.contains("h264") -> VideoCodec.H264
                    codecsStr.contains("vp9") || codecsStr.contains("vp09") || codecsStr.contains("vp8") -> VideoCodec.VP9
                    isVideo -> VideoCodec.OTHER
                    else -> null
                }

                val mappedAudioCodec = when {
                    codecsStr.contains("mp4a") || codecsStr.contains("aac") -> AudioCodec.M4A
                    codecsStr.contains("opus") -> AudioCodec.OPUS
                    codecsStr.contains("mp3") -> AudioCodec.MP3
                    isAudioOnly && containerExt == "m4a" -> AudioCodec.M4A
                    isAudioOnly && containerExt == "webm" -> AudioCodec.OPUS
                    else -> null
                }

                if (isAudioOnly) {
                    val effectiveBitrate = bitrateKbps.takeIf { it > 0 } ?: 128
                    if (fileSize <= 0L) {
                        fileSize = MediaSizeCalculator.calculateAudioTotalBytes(
                            targetBitrateKbps = effectiveBitrate,
                            codec = mappedAudioCodec,
                            durationSeconds = duration
                        )
                    }
                    rawAudioVariants.add(
                        StreamVariant(
                            formatId = formatId,
                            url = directUrl,
                            mediaType = MediaType.AUDIO,
                            qualityLabel = "$effectiveBitrate kbps",
                            resolutionHeight = 0,
                            bitrateKbps = effectiveBitrate,
                            videoCodec = null,
                            audioCodec = mappedAudioCodec ?: AudioCodec.M4A,
                            containerExt = containerExt,
                            fileSizeBytes = fileSize,
                            requiresAudioMerge = false
                        )
                    )
                } else {
                    val parsedHeight = item.optInt("height", 0).takeIf { it > 0 }
                        ?: HEIGHT_NOTE_REGEX.find(item.optString("qualityLabel"))?.groupValues?.getOrNull(1)?.toIntOrNull()
                        ?: 0
                    if (parsedHeight <= 0) continue

                    val fps = item.optInt("fps", 30)
                    val qualityLabel = "${parsedHeight}p" + if (fps > 30) " ${fps}fps" else ""
                    val requiresAudio = isAdaptive || (!codecsStr.contains("mp4a") && !codecsStr.contains("opus"))

                    rawVideoVariants.add(
                        StreamVariant(
                            formatId = formatId,
                            url = directUrl,
                            mediaType = MediaType.VIDEO,
                            qualityLabel = qualityLabel,
                            resolutionHeight = parsedHeight,
                            bitrateKbps = bitrateKbps,
                            videoCodec = mappedVideoCodec,
                            audioCodec = if (requiresAudio) null else mappedAudioCodec,
                            containerExt = containerExt,
                            fileSizeBytes = fileSize,
                            requiresAudioMerge = requiresAudio
                        )
                    )
                }
            }
        }

        processFormatsArray(streamingData.optJSONArray("formats"), isAdaptive = false)
        processFormatsArray(streamingData.optJSONArray("adaptiveFormats"), isAdaptive = true)

        if (rawVideoVariants.isEmpty() && rawAudioVariants.isEmpty()) {
            return null
        }

        val dedupedAudio = rawAudioVariants
            .groupBy { Pair(it.audioCodec, it.bitrateKbps) }
            .values
            .map { group -> group.maxByOrNull { it.fileSizeBytes } ?: group.first() }
            .sortedByDescending { it.bitrateKbps }

        val dedupedVideo = rawVideoVariants
            .groupBy { Triple(it.resolutionHeight, it.videoCodec, it.qualityLabel) }
            .values
            .map { group ->
                group.maxWithOrNull(
                    compareBy<StreamVariant> { it.bitrateKbps }
                        .thenBy { it.fileSizeBytes }
                        .thenBy { !it.requiresAudioMerge }
                ) ?: group.first()
            }

        val finalVideo = finalizeVideoVariants(dedupedVideo, dedupedAudio, duration)
        if (finalVideo.isEmpty() && dedupedAudio.isEmpty()) return null

        return DetectedPageMedia(
            pageTitle = title,
            pageUrl = pageUrl,
            thumbnailUrl = thumbnailUrl,
            durationSeconds = duration,
            videoVariants = finalVideo,
            audioVariants = dedupedAudio
        )
    }

    private fun finalizeVideoVariants(
        dedupedVideoVariants: List<StreamVariant>,
        dedupedAudioVariants: List<StreamVariant>,
        duration: Long?
    ): List<StreamVariant> {
        val bestM4aAudio = dedupedAudioVariants
            .filter { it.audioCodec == AudioCodec.M4A }
            .maxByOrNull { it.bitrateKbps }
        val bestAnyAudio = dedupedAudioVariants.maxByOrNull { it.bitrateKbps }

        return dedupedVideoVariants.map { variant ->
            val pairedAudio = if (variant.requiresAudioMerge) {
                when (variant.videoCodec) {
                    VideoCodec.AV1, VideoCodec.H264 -> bestM4aAudio ?: bestAnyAudio
                    else -> bestAnyAudio ?: bestM4aAudio
                }
            } else null

            val pairedAudioSize = if (variant.requiresAudioMerge && pairedAudio != null) {
                if (pairedAudio.fileSizeBytes > 0) {
                    pairedAudio.fileSizeBytes
                } else {
                    MediaSizeCalculator.calculateAudioTotalBytes(
                        targetBitrateKbps = pairedAudio.bitrateKbps.coerceAtLeast(128),
                        codec = pairedAudio.audioCodec,
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

            if (variant.requiresAudioMerge && pairedAudio != null) {
                variant.copy(
                    pairedAudioStreamUrl = pairedAudio.url,
                    pairedAudioFormatId = pairedAudio.formatId,
                    fileSizeBytes = totalSize
                )
            } else {
                variant.copy(fileSizeBytes = totalSize)
            }
        }.sortedWith(
            compareByDescending<StreamVariant> { it.resolutionHeight }
                .thenByDescending { it.bitrateKbps }
                .thenByDescending { it.fileSizeBytes }
        )
    }

    private fun extractDirectFileMedia(url: String, fallbackTitle: String?): DetectedPageMedia? {
        val cleanUrl = cleanMediaUrl(url)
        getCachedMedia(cleanUrl)?.let { return it }

        val headReq = Request.Builder()
            .url(url)
            .head()
            .build()

        return runCatching {
            val headCall = okHttpClient.newCall(headReq)
            var activeCall = headCall
            var response = headCall.execute()

            if (!response.isSuccessful || response.code == 405) {
                response.close()
                val getReq = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=0-0")
                    .build()
                activeCall = okHttpClient.newCall(getReq)
                response = activeCall.execute()
            }

            response.use { resp ->
                if (!resp.isSuccessful && resp.code != 206) {
                    return null
                }

                val contentRange = resp.header("Content-Range")
                val rangeTotal = if (contentRange != null && contentRange.contains("/")) {
                    contentRange.substringAfterLast("/").trim().toLongOrNull() ?: 0L
                } else 0L

                // On HTTP 206 Range bytes=0-0, Content-Length is 1; total file size comes from Content-Range!
                val contentLength = if (resp.code == 206 && rangeTotal > 0L) {
                    rangeTotal
                } else {
                    (resp.header("Content-Length")?.toLongOrNull() ?: 0L).takeIf { it > 0L } ?: rangeTotal
                }

                val contentType = resp.header("Content-Type")?.lowercase(Locale.ROOT) ?: ""
                val contentDisposition = resp.header("Content-Disposition") ?: ""

                // If Range was ignored (200 OK on GET fallback), cancel call immediately so body isn't drained over network
                if (resp.code == 200 && resp.request.method == "GET") {
                    activeCall.cancel()
                }

                // Extract filename cleanly
                val rawFilename = if (contentDisposition.contains("filename*=", ignoreCase = true)) {
                    contentDisposition.substringAfter("filename*=").substringAfter("''").substringBefore(";").trim()
                } else if (contentDisposition.contains("filename=", ignoreCase = true)) {
                    contentDisposition.substringAfter("filename=").substringBefore(";").replace("\"", "").trim()
                } else {
                    val pathName = parseUrlParts(url)?.path?.substringAfterLast('/') ?: ""
                    pathName.ifEmpty { fallbackTitle ?: "downloaded_file" }
                }

                val decodedFilename = runCatching { URLDecoder.decode(rawFilename, "UTF-8") }.getOrDefault(rawFilename)
                val cleanFilename = decodedFilename
                    .replace("..", "_")
                    .replace(SANITIZE_FILENAME_REGEX, "_")
                    .trim()
                    .trimStart('.')

                var ext = cleanFilename.substringAfterLast('.', "").lowercase(Locale.ROOT)
                if (ext.isEmpty() || ext.length > 5 || ext == cleanFilename.lowercase(Locale.ROOT)) {
                    val mimeWithoutParams = contentType.substringBefore(';').trim()
                    val guessedExt = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeWithoutParams)
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

                val isVideo = isHlsOrDash || contentType.startsWith("video/") ||
                    ext in listOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "flv", "3gp")
                val isAudio = !isVideo && (contentType.startsWith("audio/") ||
                    ext in listOf("mp3", "m4a", "opus", "wav", "flac", "aac", "ogg"))
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
                    ext in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") -> "${ext.uppercase(Locale.ROOT)} Archive"
                    ext == "pdf" -> "PDF Document"
                    ext in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt") -> "Office Document"
                    ext in listOf("iso", "dmg", "exe", "msi", "bin", "deb", "rpm") -> "Binary / Installer"
                    ext.isNotEmpty() -> "${ext.uppercase(Locale.ROOT)} File"
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

                val directMedia = DetectedPageMedia(
                    pageTitle = displayTitle,
                    pageUrl = url,
                    thumbnailUrl = null,
                    durationSeconds = null,
                    videoVariants = if (isVideo) listOf(singleVariant) else emptyList(),
                    audioVariants = if (isAudio) listOf(singleVariant) else emptyList(),
                    directFileUrl = if (isDirectFile) url else null,
                    directFileVariant = if (isDirectFile) singleVariant else null
                )
                mediaCache.put(cleanUrl, directMedia)
                if (cleanUrl != url) mediaCache.put(url, directMedia)
                directMedia
            }
        }.getOrNull()
    }

    companion object {
        private const val TAG = "MediaSniffer"
        const val ANDROID_VR_USER_AGENT =
            "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
        const val IOS_USER_AGENT =
            "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"
        const val ANDROID_USER_AGENT =
            "com.google.android.youtube/19.29.37 (Linux; U; Android 11) gzip"

        fun resolveYoutubeCdnUserAgent(url: String): String? {
            if (!url.contains("googlevideo.com", ignoreCase = true)) return null
            return when {
                url.contains("c=IOS", ignoreCase = true) -> IOS_USER_AGENT
                url.contains("c=ANDROID_VR", ignoreCase = true) -> ANDROID_VR_USER_AGENT
                url.contains("c=ANDROID", ignoreCase = true) -> ANDROID_USER_AGENT
                else -> ANDROID_VR_USER_AGENT
            }
        }

        private val HEIGHT_NOTE_REGEX = Regex("(\\d{3,4})p?")
        private val CODECS_REGEX = Regex("codecs=\"([^\"]+)\"")
        private val SANITIZE_FILENAME_REGEX = Regex("[^a-zA-Z0-9._\\- ]")
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val supportedPlatformDomains = listOf(
            "youtube.com", "youtu.be", "vimeo.com", "dailymotion.com",
            "tiktok.com", "instagram.com", "twitter.com", "x.com",
            "facebook.com", "reddit.com", "bilibili.com", "twitch.tv",
            "soundcloud.com"
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

                    var origPlay = HTMLMediaElement.prototype.play;
                    HTMLMediaElement.prototype.play = function() {
                        var mediaSrc = this.currentSrc || this.src;
                        if (mediaSrc && mediaSrc.indexOf('http') === 0 && window.UdmMediaBridge && window.UdmMediaBridge.onMediaFound) {
                            window.UdmMediaBridge.onMediaFound(mediaSrc);
                        }
                        return origPlay.apply(this, arguments);
                    };
                }
                if (window.UdmMediaBridge && window.UdmMediaBridge.onMediaFound) {
                    var mediaElements = document.querySelectorAll('video, audio, source');
                    for (var i = 0; i < mediaElements.length; i++) {
                        var src = mediaElements[i].currentSrc || mediaElements[i].src;
                        if (src && src.indexOf('http') === 0) {
                            window.UdmMediaBridge.onMediaFound(src);
                        }
                    }
                }
            })();
        """.trimIndent()
    }
}
