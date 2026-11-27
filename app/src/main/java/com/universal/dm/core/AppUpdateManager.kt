package com.universal.dm.core

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class AppReleaseInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val publishedAt: String?,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val apkSizeBytes: Long
)

sealed interface AppUpdateStatus {
    data object Idle : AppUpdateStatus
    data object Checking : AppUpdateStatus
    data class UpToDate(
        val currentVersion: String,
        val latestRelease: AppReleaseInfo?
    ) : AppUpdateStatus

    data class UpdateAvailable(
        val currentVersion: String,
        val release: AppReleaseInfo
    ) : AppUpdateStatus

    data class Downloading(
        val release: AppReleaseInfo,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val progressPercent: Int
    ) : AppUpdateStatus

    data class ReadyToInstall(
        val release: AppReleaseInfo,
        val apkFile: File
    ) : AppUpdateStatus

    data class Error(
        val message: String,
        val release: AppReleaseInfo? = null
    ) : AppUpdateStatus
}

data class AppUpdateUiState(
    val status: AppUpdateStatus = AppUpdateStatus.Idle,
    val showAutoPrompt: Boolean = false,
    val lastCheckedMs: Long = 0L
)

/**
 * GitHub-backed In-App Update Manager for UDM.
 *
 * Checks `https://api.github.com/repos/bijoykumarmistri/UDM/releases/latest`,
 * compares semantic versions against the installed APK, downloads the latest release
 * APK directly into `cacheDir/updates/`, and launches Android's package installer.
 */
object AppUpdateManager {

    private const val TAG = "AppUpdateManager"
    private const val PREFS_NAME = "udm_app_updater"
    private const val KEY_LAST_CHECK_MS = "key_last_app_update_check_ms"
    private const val CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L
    private const val UPDATES_SUBDIR = "updates"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeDownloadJob: Job? = null

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    suspend fun checkForUpdateIfDue(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_MS, 0L)
        val now = System.currentTimeMillis()
        if (now - lastCheck < CHECK_INTERVAL_MS) return

        checkForUpdate(appContext, isManual = false)
    }

    suspend fun checkForUpdate(context: Context, isManual: Boolean = true) {
        val appContext = context.applicationContext
        val currentStatus = _uiState.value.status
        if (currentStatus is AppUpdateStatus.Checking || currentStatus is AppUpdateStatus.Downloading) {
            return
        }

        _uiState.update { it.copy(status = AppUpdateStatus.Checking) }

        val installedVersion = AppMetadata.getInstalledVersionName(appContext)
        val result = withContext(Dispatchers.IO) {
            fetchLatestRelease()
        }

        result.onSuccess { release ->
            val now = System.currentTimeMillis()
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putLong(KEY_LAST_CHECK_MS, now)
            }

            if (release == null) {
                _uiState.update {
                    it.copy(
                        status = AppUpdateStatus.UpToDate(installedVersion, null),
                        lastCheckedMs = now,
                        showAutoPrompt = false
                    )
                }
                return@onSuccess
            }

            if (isNewerVersion(release.versionName, installedVersion)) {
                val cachedApk = findValidCachedApk(appContext, release)
                val nextStatus = if (cachedApk != null) {
                    AppUpdateStatus.ReadyToInstall(release, cachedApk)
                } else {
                    AppUpdateStatus.UpdateAvailable(installedVersion, release)
                }
                _uiState.update {
                    it.copy(
                        status = nextStatus,
                        lastCheckedMs = now,
                        showAutoPrompt = !isManual || it.showAutoPrompt
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        status = AppUpdateStatus.UpToDate(installedVersion, release),
                        lastCheckedMs = now,
                        showAutoPrompt = false
                    )
                }
            }
        }.onFailure { err ->
            Log.w(TAG, "Update check failed", err)
            if (isManual) {
                _uiState.update {
                    it.copy(
                        status = AppUpdateStatus.Error(
                            message = err.localizedMessage?.takeIf { m -> m.isNotBlank() }
                                ?: "Couldn't reach GitHub releases"
                        )
                    )
                }
            } else {
                _uiState.update { it.copy(status = AppUpdateStatus.Idle) }
            }
        }
    }

    fun startDownloadAndInstall(context: Context, release: AppReleaseInfo) {
        val appContext = context.applicationContext
        val downloadUrl = release.apkDownloadUrl
        if (downloadUrl.isNullOrBlank()) {
            openGitHubUrl(appContext, release.htmlUrl)
            return
        }

        val cachedApk = findValidCachedApk(appContext, release)
        if (cachedApk != null) {
            _uiState.update { it.copy(status = AppUpdateStatus.ReadyToInstall(release, cachedApk)) }
            installApk(appContext, cachedApk)
            return
        }

        if (activeDownloadJob?.isActive == true) return

        activeDownloadJob = scope.launch {
            val updatesDir = File(appContext.cacheDir, UPDATES_SUBDIR).apply { mkdirs() }
            // Remove stale APKs from older update attempts
            updatesDir.listFiles()?.forEach { old ->
                runCatching { old.delete() }
            }

            val targetFileName = sanitizeFileName(
                release.apkFileName ?: "UDM-${release.tagName}.apk"
            )
            val partFile = File(updatesDir, "$targetFileName.part")
            val finalFile = File(updatesDir, targetFileName)

            _uiState.update {
                it.copy(
                    status = AppUpdateStatus.Downloading(
                        release = release,
                        downloadedBytes = 0L,
                        totalBytes = release.apkSizeBytes,
                        progressPercent = 0
                    )
                )
            }

            val downloadResult = runCatching {
                val request = Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "UDM-Android-Updater/${AppMetadata.APP_VERSION}")
                    .build()

                NetworkClient.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("GitHub returned HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IllegalStateException("Empty APK response")
                    val contentLength = body.contentLength().takeIf { it > 0L } ?: release.apkSizeBytes

                    body.byteStream().use { input ->
                        FileOutputStream(partFile).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var totalRead = 0L
                            var lastEmitMs = 0L

                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                totalRead += read

                                val now = System.currentTimeMillis()
                                if (now - lastEmitMs >= 200L) {
                                    lastEmitMs = now
                                    val percent = if (contentLength > 0L) {
                                        ((totalRead * 100L) / contentLength).toInt().coerceIn(0, 100)
                                    } else 0
                                    _uiState.update { state ->
                                        state.copy(
                                            status = AppUpdateStatus.Downloading(
                                                release = release,
                                                downloadedBytes = totalRead,
                                                totalBytes = contentLength,
                                                progressPercent = percent
                                            )
                                        )
                                    }
                                }
                            }
                            output.flush()
                        }
                    }
                }

                if (!partFile.exists() || partFile.length() == 0L) {
                    throw IllegalStateException("Downloaded APK file is empty")
                }
                if (finalFile.exists()) finalFile.delete()
                if (!partFile.renameTo(finalFile)) {
                    partFile.copyTo(finalFile, overwrite = true)
                    partFile.delete()
                }
                finalFile
            }

            downloadResult.onSuccess { apkFile ->
                _uiState.update {
                    it.copy(status = AppUpdateStatus.ReadyToInstall(release, apkFile))
                }
                withContext(Dispatchers.Main) {
                    installApk(appContext, apkFile)
                }
            }.onFailure { err ->
                Log.e(TAG, "Failed to download update APK", err)
                runCatching { partFile.delete() }
                _uiState.update {
                    it.copy(
                        status = AppUpdateStatus.Error(
                            message = err.localizedMessage?.takeIf { m -> m.isNotBlank() }
                                ?: "Failed to download APK",
                            release = release
                        )
                    )
                }
            }
        }
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            Toast.makeText(context, "Update file missing, please re-download", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!context.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(
                context,
                "Allow UDM to install updates, then tap Install",
                Toast.LENGTH_LONG
            ).show()
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${context.packageName}".toUri()
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(settingsIntent) }
            return false
        }

        return runCatching {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        }.getOrElse { err ->
            Log.e(TAG, "Failed to launch APK installer", err)
            Toast.makeText(
                context,
                "Unable to open installer — opening GitHub release",
                Toast.LENGTH_SHORT
            ).show()
            openGitHubUrl(context, AppMetadata.GITHUB_LATEST_RELEASE_URL)
            false
        }
    }

    fun openGitHubUrl(context: Context, url: String = AppMetadata.GITHUB_RELEASES_URL) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            Toast.makeText(context, "No browser app found", Toast.LENGTH_SHORT).show()
        }
    }

    fun dismissAutoPrompt() {
        _uiState.update { it.copy(showAutoPrompt = false) }
    }

    private fun findValidCachedApk(context: Context, release: AppReleaseInfo): File? {
        val updatesDir = File(context.cacheDir, UPDATES_SUBDIR)
        val fileName = sanitizeFileName(release.apkFileName ?: "UDM-${release.tagName}.apk")
        val candidate = File(updatesDir, fileName)
        if (!candidate.exists() || candidate.length() <= 0L) return null
        if (release.apkSizeBytes > 0L && candidate.length() != release.apkSizeBytes) return null
        return candidate
    }

    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return if (cleaned.endsWith(".apk", ignoreCase = true)) cleaned else "$cleaned.apk"
    }

    private fun fetchLatestRelease(): Result<AppReleaseInfo?> = runCatching {
        val latestRequest = Request.Builder()
            .url(AppMetadata.GITHUB_LATEST_RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "UDM-Android-Updater/${AppMetadata.APP_VERSION}")
            .build()

        NetworkClient.client.newCall(latestRequest).execute().use { response ->
            if (response.isSuccessful) {
                val bodyStr = response.body?.string().orEmpty()
                return@runCatching parseReleaseJson(bodyStr)
            }
            if (response.code != 404) {
                throw IllegalStateException("GitHub API error (HTTP ${response.code})")
            }
        }

        // Fallback to list of releases if `/releases/latest` is not published
        val listRequest = Request.Builder()
            .url(AppMetadata.GITHUB_RELEASES_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "UDM-Android-Updater/${AppMetadata.APP_VERSION}")
            .build()

        NetworkClient.client.newCall(listRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("GitHub API error (HTTP ${response.code})")
            }
            val bodyStr = response.body?.string().orEmpty()
            parseReleaseListJson(bodyStr)
        }
    }

    internal fun parseReleaseJson(jsonText: String): AppReleaseInfo? {
        if (jsonText.isBlank()) return null
        val obj = runCatching { MiniJsonParser(jsonText).parseTopObject() }.getOrNull() ?: return null
        return parseReleaseMap(obj)
    }

    internal fun parseReleaseListJson(jsonText: String): AppReleaseInfo? {
        if (jsonText.isBlank()) return null
        val array = runCatching { MiniJsonParser(jsonText).parseTopArray() }.getOrNull() ?: return null
        var best: AppReleaseInfo? = null
        for (item in array) {
            @Suppress("UNCHECKED_CAST")
            val map = item as? Map<String, Any?> ?: continue
            if ((map["draft"] as? Boolean) == true || (map["prerelease"] as? Boolean) == true) continue
            val parsed = parseReleaseMap(map) ?: continue
            if (best == null || isNewerVersion(parsed.versionName, best.versionName)) {
                best = parsed
            }
        }
        return best
    }

    private fun parseReleaseMap(obj: Map<String, Any?>): AppReleaseInfo? {
        if ((obj["draft"] as? Boolean) == true) return null

        val tagName = (obj["tag_name"] as? String)?.trim().orEmpty()
        if (tagName.isBlank()) return null

        val versionName = normalizeVersion(tagName)
        val title = (obj["name"] as? String)?.trim()?.ifBlank { "UDM $tagName" } ?: "UDM $tagName"
        val body = (obj["body"] as? String)?.trim().orEmpty()
        val htmlUrl = (obj["html_url"] as? String)?.trim()
            ?.ifBlank { AppMetadata.GITHUB_RELEASES_URL }
            ?: AppMetadata.GITHUB_RELEASES_URL
        val publishedAt = (obj["published_at"] as? String)?.trim()?.takeIf { it.isNotBlank() }

        var apkUrl: String? = null
        var apkName: String? = null
        var apkSize = 0L

        val assets = obj["assets"] as? List<*>
        if (assets != null) {
            for (rawAsset in assets) {
                @Suppress("UNCHECKED_CAST")
                val asset = rawAsset as? Map<String, Any?> ?: continue
                val name = (asset["name"] as? String)?.trim().orEmpty()
                val contentType = (asset["content_type"] as? String)?.trim().orEmpty()
                val downloadUrl = (asset["browser_download_url"] as? String)?.trim().orEmpty()
                val isApk = name.endsWith(".apk", ignoreCase = true) ||
                    contentType.equals("application/vnd.android.package-archive", ignoreCase = true)
                if (isApk && downloadUrl.isNotBlank()) {
                    apkUrl = downloadUrl
                    apkName = name.ifBlank { "UDM-$tagName.apk" }
                    apkSize = (asset["size"] as? Number)?.toLong() ?: 0L
                    break
                }
            }
        }

        return AppReleaseInfo(
            tagName = tagName,
            versionName = versionName,
            title = title,
            releaseNotes = body,
            htmlUrl = htmlUrl,
            publishedAt = publishedAt,
            apkDownloadUrl = apkUrl,
            apkFileName = apkName,
            apkSizeBytes = apkSize
        )
    }

    private class MiniJsonParser(private val src: String) {
        private var pos = 0

        fun parseTopObject(): Map<String, Any?>? {
            skipWhitespace()
            if (pos >= src.length || src[pos] != '{') return null
            return parseObject()
        }

        fun parseTopArray(): List<Any?>? {
            skipWhitespace()
            if (pos >= src.length || src[pos] != '[') return null
            return parseArray()
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= src.length) error("Unexpected EOF")
            return when (val c = src[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                '-', in '0'..'9' -> parseNumber()
                else -> error("Unexpected char '$c' at $pos")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            pos++ // '{'
            val map = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return map
            }
            while (pos < src.length) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                require(pos < src.length && src[pos] == ':') { "Expected ':' at $pos" }
                pos++
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else {
                    require(pos < src.length && src[pos] == '}') { "Expected '}' at $pos" }
                    pos++
                    break
                }
            }
            return map
        }

        private fun parseArray(): List<Any?> {
            pos++ // '['
            val list = ArrayList<Any?>()
            skipWhitespace()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return list
            }
            while (pos < src.length) {
                list.add(parseValue())
                skipWhitespace()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else {
                    require(pos < src.length && src[pos] == ']') { "Expected ']' at $pos" }
                    pos++
                    break
                }
            }
            return list
        }

        private fun parseString(): String {
            require(pos < src.length && src[pos] == '"') { "Expected '\"' at $pos" }
            pos++
            val sb = StringBuilder()
            while (pos < src.length) {
                val c = src[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= src.length) error("Unterminated escape")
                        when (val esc = src[pos++]) {
                            '"', '\\', '/' -> sb.append(esc)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                require(pos + 4 <= src.length) { "Invalid unicode escape" }
                                val hex = src.substring(pos, pos + 4)
                                pos += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                            else -> sb.append(esc)
                        }
                    }
                    else -> sb.append(c)
                }
            }
            error("Unterminated string")
        }

        private fun parseBoolean(): Boolean {
            return if (src.startsWith("true", pos)) {
                pos += 4
                true
            } else if (src.startsWith("false", pos)) {
                pos += 5
                false
            } else {
                error("Invalid boolean at $pos")
            }
        }

        private fun parseNull(): Any? {
            require(src.startsWith("null", pos)) { "Invalid null at $pos" }
            pos += 4
            return null
        }

        private fun parseNumber(): Number {
            val start = pos
            if (src[pos] == '-') pos++
            while (pos < src.length && src[pos] in '0'..'9') pos++
            var isFloat = false
            if (pos < src.length && src[pos] == '.') {
                isFloat = true
                pos++
                while (pos < src.length && src[pos] in '0'..'9') pos++
            }
            if (pos < src.length && (src[pos] == 'e' || src[pos] == 'E')) {
                isFloat = true
                pos++
                if (pos < src.length && (src[pos] == '+' || src[pos] == '-')) pos++
                while (pos < src.length && src[pos] in '0'..'9') pos++
            }
            val token = src.substring(start, pos)
            return if (isFloat) token.toDouble() else token.toLong()
        }

        private fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) {
                pos++
            }
        }
    }

    internal fun normalizeVersion(raw: String): String {
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')
            .trim()
    }

    internal fun compareVersions(v1: String, v2: String): Int {
        val parts1 = normalizeVersion(v1)
            .split('.')
            .map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val parts2 = normalizeVersion(v2)
            .split('.')
            .map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }

        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val a = parts1.getOrElse(i) { 0 }
            val b = parts2.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    internal fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        return compareVersions(remoteVersion, currentVersion) > 0
    }
}
