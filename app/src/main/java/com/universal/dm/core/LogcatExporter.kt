package com.universal.dm.core

import android.app.ActivityManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.core.content.FileProvider
import com.universal.dm.UdmApplication
import com.universal.dm.domain.sniffer.MediaSizeCalculator
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports a comprehensive diagnostic report containing device specs, app state,
 * settings, download queue snapshot, and full Logcat buffers for easy bug fixing.
 */
object LogcatExporter {

    private const val MAX_LOGCAT_LINES = 20_000

    suspend fun exportDiagnosticLog(context: Context): File = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val logsDir = File(appContext.cacheDir, "logs").apply { if (!exists()) mkdirs() }

        // Keep cache tidy by pruning older diagnostic exports
        logsDir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(2)
            ?.forEach { runCatching { it.delete() } }

        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val outFile = File(logsDir, "udm-logcat-$timestamp.txt")

        val report = buildDiagnosticReport(appContext)
        outFile.writeText(report, Charsets.UTF_8)
        outFile
    }

    fun shareLogFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val pkgInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        }.getOrNull()
        val versionName = pkgInfo?.versionName ?: "unknown"
        val versionCode = pkgInfo?.longVersionCode ?: 0L

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "UDM Diagnostic Logcat (v$versionName)")
            putExtra(
                Intent.EXTRA_TEXT,
                "UDM v$versionName ($versionCode) • Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) • ${Build.MANUFACTURER} ${Build.MODEL}"
            )
            clipData = ClipData.newRawUri("UDM Logcat", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Share diagnostic logs").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private suspend fun buildDiagnosticReport(context: Context): String {
        val sb = StringBuilder(64 * 1024)
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())

        val pkgInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        }.getOrNull()

        sb.appendLine("============================================================")
        sb.appendLine("UDM DIAGNOSTIC LOGCAT REPORT")
        sb.appendLine("============================================================")
        sb.appendLine("Generated At     : $nowFormatted")
        sb.appendLine("Package          : ${context.packageName}")
        sb.appendLine("App Version      : ${pkgInfo?.versionName ?: "unknown"} (${pkgInfo?.longVersionCode ?: 0})")
        sb.appendLine("Android OS       : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}, Patch ${Build.VERSION.SECURITY_PATCH})")
        sb.appendLine("Device           : ${Build.MANUFACTURER} ${Build.BRAND} ${Build.MODEL} (${Build.DEVICE} / ${Build.HARDWARE})")
        sb.appendLine("Build Finger     : ${Build.FINGERPRINT}")
        sb.appendLine("Supported ABIs   : ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        sb.appendLine("Locale / TZ      : ${Locale.getDefault()} / ${java.util.TimeZone.getDefault().id}")

        // Memory
        val runtime = Runtime.getRuntime()
        val usedHeapMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxHeapMb = runtime.maxMemory() / (1024 * 1024)
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
        sb.appendLine("JVM Heap         : ${usedHeapMb} MB used / ${maxHeapMb} MB max")
        if (memInfo.totalMem > 0) {
            sb.appendLine(
                "System RAM       : ${MediaSizeCalculator.formatFileSize(memInfo.availMem)} free / ${MediaSizeCalculator.formatFileSize(memInfo.totalMem)} total (lowMemory=${memInfo.lowMemory})"
            )
        }

        // Storage
        runCatching {
            val dataStat = StatFs(Environment.getDataDirectory().path)
            val freeData = dataStat.availableBlocksLong * dataStat.blockSizeLong
            val totalData = dataStat.blockCountLong * dataStat.blockSizeLong
            sb.appendLine("Internal Storage : ${MediaSizeCalculator.formatFileSize(freeData)} free / ${MediaSizeCalculator.formatFileSize(totalData)} total")
        }

        // Network
        sb.appendLine("Network          : ${describeNetwork(context)}")

        // Extractor version (non-blocking)
        val ytDlpVersion = runCatching { YoutubeDL.getInstance().version(context) }.getOrNull() ?: "uninitialized"
        sb.appendLine("yt-dlp Version   : $ytDlpVersion")

        // Settings Snapshot
        sb.appendLine()
        sb.appendLine("-------------------- APP SETTINGS --------------------------")
        val prefs = context.getSharedPreferences("udm_app_settings", Context.MODE_PRIVATE)
        prefs.all.toSortedMap().forEach { (k, v) ->
            if (k != "key_browser_favourites") {
                sb.appendLine("$k = $v")
            }
        }

        // Download Tasks Snapshot
        sb.appendLine()
        sb.appendLine("-------------------- DOWNLOAD TASKS ------------------------")
        val tasks = runCatching {
            UdmApplication.getDatabase(context).downloadDao().getAllTasksFlow().firstOrNull()
        }.getOrNull().orEmpty()
        if (tasks.isEmpty()) {
            sb.appendLine("(no tasks)")
        } else {
            tasks.forEach { task ->
                sb.appendLine(
                    "[${task.status}] id=${task.id} | file=${task.fileName} | type=${task.mediaType} | " +
                        "quality=${task.qualityLabel} | codec=${task.codecLabel} | formatId=${task.formatId} | " +
                        "progress=${task.downloadedBytes}/${task.totalBytes} | threads=${task.threadCount} | " +
                        "page=${task.sourcePageUrl} | error=${task.errorMessage ?: "none"}"
                )
            }
        }

        // Full Logcat Buffers
        sb.appendLine()
        sb.appendLine("-------------------- LOGCAT OUTPUT -------------------------")
        sb.appendLine(readLogcatLines())

        return sb.toString()
    }

    private fun describeNetwork(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "unavailable"
        val network = cm.activeNetwork ?: return "disconnected"
        val caps = cm.getNetworkCapabilities(network) ?: return "no capabilities"
        val transports = buildList {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("WIFI")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("CELLULAR")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("ETHERNET")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
        }.ifEmpty { listOf("OTHER") }
        val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val unmetered = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        return "${transports.joinToString("+")} (validated=$validated, unmetered=$unmetered)"
    }

    private fun readLogcatLines(): String {
        val commands = listOf(
            listOf("logcat", "-d", "-b", "main,system,crash", "-v", "threadtime", "*:V"),
            listOf("logcat", "-d", "-v", "threadtime", "*:V"),
            listOf("logcat", "-d", "-v", "time")
        )

        var lastError: String? = null
        for (cmd in commands) {
            val result = runCatching {
                val process = ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start()
                val lines = ArrayDeque<String>(MAX_LOGCAT_LINES.coerceAtMost(4096))
                var totalRead = 0
                BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        totalRead++
                        if (lines.size >= MAX_LOGCAT_LINES) {
                            lines.removeFirst()
                        }
                        lines.addLast(line)
                        line = reader.readLine()
                    }
                }
                val exit = process.waitFor()
                Pair(exit, lines to totalRead)
            }

            result.onSuccess { (exit, data) ->
                val (lines, totalRead) = data
                if (lines.isNotEmpty()) {
                    val header = if (totalRead > MAX_LOGCAT_LINES) {
                        "(Showing last ${lines.size} of $totalRead logcat lines; cmd=${cmd.joinToString(" ")})\n"
                    } else {
                        "(Captured ${lines.size} logcat lines; cmd=${cmd.joinToString(" ")})\n"
                    }
                    return header + lines.joinToString("\n")
                }
                lastError = "Command ${cmd.joinToString(" ")} exited with $exit and produced no lines"
            }.onFailure { err ->
                lastError = "${err.javaClass.simpleName}: ${err.message}"
            }
        }

        return "(Unable to read logcat: ${lastError ?: "unknown error"})"
    }
}
