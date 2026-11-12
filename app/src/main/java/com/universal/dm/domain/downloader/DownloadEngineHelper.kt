package com.universal.dm.domain.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

object DownloadEngineHelper {

    const val CHANNEL_ID = "udm_download_channel"
    private const val CHANNEL_NAME = "UDM Active Downloads"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays live multi-threaded download progress and speed"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun createForegroundInfo(
        context: Context,
        notificationId: Int,
        title: String,
        progressPercent: Int,
        speedBytesPerSec: Long,
        isMuxing: Boolean = false,
        isIndeterminate: Boolean = false
    ): ForegroundInfo {
        ensureNotificationChannel(context)

        val speedText = formatSpeed(speedBytesPerSec)
        val contentText = when {
            isMuxing -> "Merging AV1 & M4A Streams with FFmpeg..."
            isIndeterminate -> "Initializing download streams..."
            else -> "$progressPercent% • $speedText"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, if (isIndeterminate || isMuxing) 0 else progressPercent, isIndeterminate || isMuxing)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", bytesPerSec.toFloat() / (1024 * 1024))
            bytesPerSec >= 1024 -> String.format(Locale.US, "%.1f KB/s", bytesPerSec.toFloat() / 1024)
            else -> "$bytesPerSec B/s"
        }
    }

    /**
     * Saves the completed downloaded file into the public Downloads/UDM directory using MediaStore on
     * Android 10+ (API 29+) or direct filesystem writes on Android 9 and lower.
     */
    fun saveFileToPublicDownloads(
        context: Context,
        sourceFile: File,
        fileName: String,
        mimeType: String
    ): String {
        if (!sourceFile.exists()) return sourceFile.absolutePath

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/UDM")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val collectionUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri: Uri? = resolver.insert(collectionUri, contentValues)

            if (itemUri != null) {
                try {
                    resolver.openOutputStream(itemUri)?.use { outStream ->
                        FileInputStream(sourceFile).use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)

                    // Clean up temp file
                    sourceFile.delete()
                    return itemUri.toString()
                } catch (_: Exception) {
                    // Fallback to local source path on error
                }
            }
        }

        // Android 9 and lower fallback or legacy directory
        val publicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "UDM"
        )
        if (!publicDir.exists()) publicDir.mkdirs()

        val destFile = File(publicDir, fileName)
        try {
            FileInputStream(sourceFile).use { inStream ->
                FileOutputStream(destFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
            sourceFile.delete()
            return destFile.absolutePath
        } catch (_: Exception) {
            return sourceFile.absolutePath
        }
    }
}
