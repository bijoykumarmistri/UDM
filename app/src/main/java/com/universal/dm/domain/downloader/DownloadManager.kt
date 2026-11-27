package com.universal.dm.domain.downloader

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import com.universal.dm.UdmApplication
import com.universal.dm.data.local.DownloadDao
import com.universal.dm.data.local.DownloadTaskEntity
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.data.model.MediaType
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DownloadManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val database = UdmApplication.getDatabase(appContext)
    private val downloadDao: DownloadDao = database.downloadDao()
    private val workManager: WorkManager = WorkManager.getInstance(appContext)
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    init {
        registerNetworkCallback()
        reconcileOrphanedTasks()
    }

    private fun reconcileOrphanedTasks() {
        UdmApplication.appScope.launch {
            runCatching {
                val queuedTasks = downloadDao.getTasksByStatus(DownloadStatus.QUEUED)
                queuedTasks.forEach { task ->
                    val workInfos = workManager.getWorkInfosForUniqueWork("download_${task.id}").get()
                    val hasActiveWork = workInfos.any { !it.state.isFinished }
                    if (!hasActiveWork) {
                        scheduleWork(task.id)
                    }
                }

                val activeTasks = downloadDao.getTasksByStatus(DownloadStatus.DOWNLOADING) +
                    downloadDao.getTasksByStatus(DownloadStatus.MERGING_MUXING)
                activeTasks.forEach { task ->
                    val workInfos = workManager.getWorkInfosForUniqueWork("download_${task.id}").get()
                    val hasActiveWork = workInfos.any { !it.state.isFinished }
                    if (!hasActiveWork) {
                        downloadDao.updateTaskStatus(
                            task.id,
                            DownloadStatus.PAUSED,
                            "Interrupted — tap to resume"
                        )
                    }
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            connectivityManager?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val prefs = appContext.getSharedPreferences("udm_app_settings", Context.MODE_PRIVATE)
                    val autoResume = prefs.getBoolean("key_auto_resume_wifi", true)
                    if (autoResume) {
                        UdmApplication.appScope.launch {
                            val pausedTasks = downloadDao.getTasksByStatus(DownloadStatus.PAUSED)
                            pausedTasks.forEach { task ->
                                val msg = task.errorMessage.orEmpty()
                                if (msg.contains("wifi", ignoreCase = true) ||
                                    msg.contains("network", ignoreCase = true)
                                ) {
                                    resumeDownload(task.id)
                                }
                            }
                            val failedTasks = downloadDao.getTasksByStatus(DownloadStatus.FAILED)
                            failedTasks.forEach { task ->
                                val msg = task.errorMessage.orEmpty()
                                if (msg.contains("timeout", ignoreCase = true) ||
                                    msg.contains("unable to resolve", ignoreCase = true) ||
                                    msg.contains("failed to connect", ignoreCase = true) ||
                                    msg.contains("network", ignoreCase = true) ||
                                    msg.contains("socket", ignoreCase = true)
                                ) {
                                    resumeDownload(task.id)
                                }
                            }
                        }
                    }
                }
            })
        } catch (_: Exception) {}
    }

    suspend fun enqueueDownload(
        title: String,
        sourcePageUrl: String,
        downloadUrl: String,
        pairedAudioUrl: String? = null,
        fileName: String,
        destinationFolder: String,
        mediaType: MediaType = MediaType.DIRECT_FILE,
        qualityLabel: String = "1080p",
        codecLabel: String = "AV1",
        formatId: String? = null,
        threadCount: Int = 8,
        totalBytes: Long = 0L,
        thumbnailUrl: String? = null
    ): String = withContext(Dispatchers.IO) {
        val taskId = UUID.randomUUID().toString()

        val task = DownloadTaskEntity(
            id = taskId,
            title = title,
            sourcePageUrl = sourcePageUrl,
            downloadUrl = downloadUrl,
            pairedAudioUrl = pairedAudioUrl,
            fileName = fileName,
            filePath = destinationFolder,
            mediaType = mediaType,
            qualityLabel = qualityLabel,
            codecLabel = codecLabel,
            formatId = formatId,
            totalBytes = totalBytes,
            downloadedBytes = 0L,
            status = DownloadStatus.QUEUED,
            speedBytesPerSec = 0L,
            threadCount = threadCount.coerceIn(1, 16),
            createdAt = System.currentTimeMillis(),
            thumbnailUrl = thumbnailUrl
        )

        downloadDao.insertTask(task)
        scheduleWork(taskId)
        taskId
    }

    suspend fun pauseDownload(taskId: String) = withContext(Dispatchers.IO) {
        workManager.cancelAllWorkByTag(taskId)
        runCatching {
            YoutubeDL.getInstance().destroyProcessById(taskId)
        }
        downloadDao.updateTaskStatus(taskId, DownloadStatus.PAUSED, "Paused by user")
    }

    suspend fun resumeDownload(taskId: String) = withContext(Dispatchers.IO) {
        downloadDao.updateTaskStatus(taskId, DownloadStatus.QUEUED, null)
        scheduleWork(taskId)
    }

    suspend fun cancelDownload(taskId: String) = withContext(Dispatchers.IO) {
        workManager.cancelAllWorkByTag(taskId)
        runCatching {
            YoutubeDL.getInstance().destroyProcessById(taskId)
        }
        cleanupTaskCache(taskId)
        downloadDao.deleteChunksForTask(taskId)
        downloadDao.deleteTaskById(taskId)
    }

    suspend fun cleanupTaskCache(taskId: String) = withContext(Dispatchers.IO) {
        val task = downloadDao.getTaskById(taskId)
        val downloadsCacheDir = File(appContext.cacheDir, "downloads")
        val taskDir = File(downloadsCacheDir, "task_$taskId")
        runCatching {
            if (taskDir.exists()) taskDir.deleteRecursively()
        }
        if (task != null) {
            val legacyTempFile = File(downloadsCacheDir, task.fileName)
            runCatching {
                if (legacyTempFile.exists()) legacyTempFile.delete()
            }
        }
    }

    suspend fun updateNetworkConstraints() = withContext(Dispatchers.IO) {
        val queuedTasks = downloadDao.getTasksByStatus(DownloadStatus.QUEUED)
        val downloadingTasks = downloadDao.getTasksByStatus(DownloadStatus.DOWNLOADING)
        (queuedTasks + downloadingTasks).forEach { task ->
            scheduleWork(task.id)
        }
    }

    private fun scheduleWork(taskId: String) {
        val prefs = appContext.getSharedPreferences("udm_app_settings", Context.MODE_PRIVATE)
        val isWifiOnly = prefs.getBoolean("key_wifi_only", false)
        val networkType = if (isWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<UdmDownloadWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag(taskId)
            .setInputData(workDataOf(UdmDownloadWorker.KEY_TASK_ID to taskId))
            .build()

        workManager.enqueueUniqueWork(
            "download_$taskId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    suspend fun getReclaimableCacheBytes(): Long = withContext(Dispatchers.IO) {
        val activeTaskIds = getProtectedTaskIds()
        var totalBytes = 0L

        val downloadsCacheDir = File(appContext.cacheDir, "downloads")
        if (downloadsCacheDir.exists()) {
            downloadsCacheDir.listFiles()?.forEach { child ->
                val isProtected = child.isDirectory &&
                    child.name.startsWith("task_") &&
                    activeTaskIds.contains(child.name.removePrefix("task_"))
                if (!isProtected) {
                    totalBytes += calculateFileSize(child)
                }
            }
        }

        val logsCacheDir = File(appContext.cacheDir, "logs")
        if (logsCacheDir.exists()) {
            totalBytes += calculateFileSize(logsCacheDir)
        }

        totalBytes
    }

    suspend fun clearReclaimableCache(): Long = withContext(Dispatchers.IO) {
        val activeTaskIds = getProtectedTaskIds()
        var freedBytes = 0L

        val downloadsCacheDir = File(appContext.cacheDir, "downloads")
        if (downloadsCacheDir.exists()) {
            downloadsCacheDir.listFiles()?.forEach { child ->
                val isProtected = child.isDirectory &&
                    child.name.startsWith("task_") &&
                    activeTaskIds.contains(child.name.removePrefix("task_"))
                if (!isProtected) {
                    freedBytes += calculateFileSize(child)
                    runCatching { child.deleteRecursively() }
                }
            }
        }

        val logsCacheDir = File(appContext.cacheDir, "logs")
        if (logsCacheDir.exists()) {
            freedBytes += calculateFileSize(logsCacheDir)
            runCatching { logsCacheDir.deleteRecursively() }
        }

        freedBytes
    }

    private suspend fun getProtectedTaskIds(): Set<String> {
        val protectedStatuses = listOf(
            DownloadStatus.QUEUED,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.MERGING_MUXING,
            DownloadStatus.PAUSED
        )
        return protectedStatuses
            .flatMap { status -> downloadDao.getTasksByStatus(status) }
            .map { it.id }
            .toSet()
    }

    private fun calculateFileSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { calculateFileSize(it) } ?: 0L
    }

    companion object {
        @Volatile
        private var instance: DownloadManager? = null

        fun getInstance(context: Context): DownloadManager {
            return instance ?: synchronized(this) {
                instance ?: DownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
