package com.universal.dm.ui.downloads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.universal.dm.UdmApplication
import com.universal.dm.data.local.DownloadChunkEntity
import com.universal.dm.data.local.DownloadTaskEntity
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.data.model.MediaType
import com.universal.dm.domain.downloader.DownloadEngineHelper
import com.universal.dm.domain.downloader.DownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = UdmApplication.getDatabase(application)
    private val downloadDao = database.downloadDao()
    private val downloadManager = DownloadManager.getInstance(application)

    val allTasks: StateFlow<List<DownloadTaskEntity>> = downloadDao.getAllTasksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTasks: StateFlow<List<DownloadTaskEntity>> = downloadDao.getActiveTasksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Single reactive Room Flow eliminates N+1 per-task chunk queries on every progress tick
    val chunksMap: StateFlow<Map<String, List<DownloadChunkEntity>>> = downloadDao.getActiveChunksFlow()
        .map { chunks -> chunks.groupBy { it.taskId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _peakSpeed = MutableStateFlow(0L)
    val peakSpeed: StateFlow<Long> = _peakSpeed.asStateFlow()

    private val _previewTask = MutableStateFlow<DownloadTaskEntity?>(null)
    val previewTask: StateFlow<DownloadTaskEntity?> = _previewTask.asStateFlow()

    init {
        viewModelScope.launch {
            allTasks.collect { tasks ->
                val currentTotalSpeed = tasks
                    .filter { it.status == DownloadStatus.DOWNLOADING }
                    .sumOf { it.speedBytesPerSec }
                if (currentTotalSpeed > _peakSpeed.value) {
                    _peakSpeed.value = currentTotalSpeed
                }
            }
        }
    }

    fun selectTaskForPreview(task: DownloadTaskEntity?) {
        _previewTask.value = task
    }

    fun deleteTask(task: DownloadTaskEntity, deleteFileFromStorage: Boolean = true) {
        viewModelScope.launch {
            if (deleteFileFromStorage) {
                withContext(Dispatchers.IO) {
                    DownloadEngineHelper.deleteSavedFile(getApplication(), task.filePath)
                }
            }
            downloadManager.cancelDownload(task.id)
        }
    }

    fun pauseAll() {
        viewModelScope.launch {
            activeTasks.value.forEach { task ->
                if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.QUEUED) {
                    downloadManager.pauseDownload(task.id)
                }
            }
        }
    }

    fun resumeAll() {
        viewModelScope.launch {
            activeTasks.value.forEach { task ->
                if (task.status == DownloadStatus.PAUSED) {
                    downloadManager.resumeDownload(task.id)
                }
            }
        }
    }

    fun pauseTask(taskId: String) {
        viewModelScope.launch {
            downloadManager.pauseDownload(taskId)
        }
    }

    fun resumeTask(taskId: String) {
        viewModelScope.launch {
            downloadManager.resumeDownload(taskId)
        }
    }

    fun cancelTask(taskId: String) {
        viewModelScope.launch {
            downloadManager.cancelDownload(taskId)
        }
    }

    fun retryTask(taskId: String) {
        viewModelScope.launch {
            downloadManager.resumeDownload(taskId)
        }
    }

    fun clearFailedTasks() {
        viewModelScope.launch {
            activeTasks.value.filter { it.status == DownloadStatus.FAILED }.forEach { task ->
                downloadManager.cancelDownload(task.id)
            }
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            downloadDao.deleteCompletedTasks()
        }
    }

    fun cancelAll() {
        viewModelScope.launch {
            activeTasks.value.forEach { task ->
                downloadManager.cancelDownload(task.id)
            }
        }
    }

    fun enqueueManualDownload(url: String, fileName: String, threadCount: Int = 8) {
        viewModelScope.launch {
            val isStreamingUrl = url.contains("youtube.com", ignoreCase = true) ||
                url.contains("youtu.be", ignoreCase = true) ||
                url.contains("vimeo.com", ignoreCase = true) ||
                url.contains("soundcloud.com", ignoreCase = true) ||
                url.contains("dailymotion.com", ignoreCase = true) ||
                url.contains("facebook.com", ignoreCase = true) ||
                url.contains("instagram.com", ignoreCase = true) ||
                url.contains("tiktok.com", ignoreCase = true) ||
                url.contains("twitter.com", ignoreCase = true) ||
                url.contains("x.com", ignoreCase = true)

            val mediaType = if (isStreamingUrl) MediaType.VIDEO else MediaType.DIRECT_FILE
            val defaultExtension = if (isStreamingUrl) "mp4" else "bin"
            val inferredName = runCatching {
                val path = java.net.URI(url).path
                path?.substringAfterLast('/')?.takeIf { it.isNotBlank() && it.contains('.') }
            }.getOrNull()

            val sanitizedName = when {
                fileName.isNotBlank() -> if (fileName.contains('.')) fileName else "$fileName.$defaultExtension"
                !inferredName.isNullOrBlank() -> inferredName
                else -> "udm_download_${System.currentTimeMillis()}.$defaultExtension"
            }

            val appSettings = com.universal.dm.ui.settings.AppSettingsState(getApplication())
            val targetFolder = appSettings.defaultDownloadFolder.ifBlank { "/storage/emulated/0/Download/UDM" }
            val preferredQualityLabel = when (appSettings.preferredVideoQuality) {
                2160 -> "2160p 4K"
                720 -> "720p HD"
                480 -> "480p"
                else -> "1080p FHD"
            }

            downloadManager.enqueueDownload(
                title = fileName.ifEmpty { inferredName ?: if (isStreamingUrl) "Web Video Stream" else "Direct File Download" },
                sourcePageUrl = url,
                downloadUrl = url,
                fileName = sanitizedName,
                destinationFolder = targetFolder,
                mediaType = mediaType,
                qualityLabel = if (isStreamingUrl) preferredQualityLabel else "Standard",
                codecLabel = if (isStreamingUrl) "AV1/H.264" else "Direct",
                threadCount = threadCount
            )
        }
    }
}
