package com.universal.dm.domain.downloader

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.universal.dm.UdmApplication
import com.universal.dm.data.local.DownloadDao
import com.universal.dm.data.local.DownloadTaskEntity
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.data.model.MediaType
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DownloadManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val database = UdmApplication.getDatabase(appContext)
    private val downloadDao: DownloadDao = database.downloadDao()
    private val workManager: WorkManager = WorkManager.getInstance(appContext)

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
        totalBytes: Long = 0L
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
            threadCount = threadCount,
            createdAt = System.currentTimeMillis()
        )

        downloadDao.insertTask(task)

        // Enqueue WorkManager job
        scheduleWork(taskId)

        taskId
    }

    suspend fun pauseDownload(taskId: String) = withContext(Dispatchers.IO) {
        workManager.cancelAllWorkByTag(taskId)
        runCatching {
            YoutubeDL.getInstance().destroyProcessById(taskId)
        }
        downloadDao.updateTaskStatus(taskId, DownloadStatus.PAUSED)
    }

    suspend fun resumeDownload(taskId: String) = withContext(Dispatchers.IO) {
        downloadDao.updateTaskStatus(taskId, DownloadStatus.QUEUED)
        scheduleWork(taskId)
    }

    suspend fun cancelDownload(taskId: String) = withContext(Dispatchers.IO) {
        workManager.cancelAllWorkByTag(taskId)
        runCatching {
            YoutubeDL.getInstance().destroyProcessById(taskId)
        }
        val task = downloadDao.getTaskById(taskId)
        if (task != null) {
            val tempFile = File(appContext.cacheDir, "downloads/${task.fileName}")
            if (tempFile.exists()) tempFile.delete()
        }
        downloadDao.deleteChunksForTask(taskId)
        downloadDao.deleteTaskById(taskId)
    }

    private fun scheduleWork(taskId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<UdmDownloadWorker>()
            .setConstraints(constraints)
            .addTag(taskId)
            .setInputData(workDataOf(UdmDownloadWorker.KEY_TASK_ID to taskId))
            .build()

        workManager.enqueueUniqueWork(
            "download_$taskId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
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
