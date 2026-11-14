package com.universal.dm.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.data.model.MediaType
import java.util.UUID

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sourcePageUrl: String,
    val downloadUrl: String,
    val pairedAudioUrl: String? = null,
    val fileName: String,
    val filePath: String,
    val mediaType: MediaType = MediaType.DIRECT_FILE,
    val qualityLabel: String,
    val codecLabel: String,
    val formatId: String? = null,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val speedBytesPerSec: Long = 0L,
    val threadCount: Int = 4,
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
