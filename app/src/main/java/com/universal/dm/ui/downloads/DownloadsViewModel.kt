package com.universal.dm.ui.downloads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.universal.dm.UdmApplication
import com.universal.dm.data.local.DownloadChunkEntity
import com.universal.dm.data.local.DownloadTaskEntity
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.data.model.MediaType
import com.universal.dm.domain.downloader.DownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = UdmApplication.getDatabase(application)
    private val downloadDao = database.downloadDao()
    private val downloadManager = DownloadManager.getInstance(application)

    val activeTasks: StateFlow<List<DownloadTaskEntity>> = downloadDao.getActiveTasksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _chunksMap = MutableStateFlow<Map<String, List<DownloadChunkEntity>>>(emptyMap())
    val chunksMap: StateFlow<Map<String, List<DownloadChunkEntity>>> = _chunksMap.asStateFlow()

    init {
        viewModelScope.launch {
            activeTasks.collect { tasks ->
                val newMap = mutableMapOf<String, List<DownloadChunkEntity>>()
                tasks.forEach { task ->
                    newMap[task.id] = downloadDao.getChunksForTask(task.id)
                }
                _chunksMap.value = newMap
            }
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
            val sanitizedName = if (fileName.isNotBlank()) {
                if (fileName.contains(".")) fileName else "$fileName.$defaultExtension"
            } else {
                "udm_download_${System.currentTimeMillis()}.$defaultExtension"
            }

            downloadManager.enqueueDownload(
                title = fileName.ifEmpty { if (isStreamingUrl) "Web Video Stream" else "Direct File Download" },
                sourcePageUrl = url,
                downloadUrl = url,
                fileName = sanitizedName,
                destinationFolder = "/storage/emulated/0/Download/UDM",
                mediaType = mediaType,
                qualityLabel = if (isStreamingUrl) "1080p FHD" else "Standard",
                codecLabel = if (isStreamingUrl) "AV1/H.264" else "Direct",
                threadCount = threadCount
            )
        }
    }
}
