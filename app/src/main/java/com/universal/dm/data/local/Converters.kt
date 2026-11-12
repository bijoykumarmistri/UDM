package com.universal.dm.data.local

import androidx.room.TypeConverter
import com.universal.dm.data.model.DownloadStatus
import com.universal.dm.data.model.MediaType

class Converters {
    @TypeConverter
    fun fromMediaType(value: MediaType?): String? = value?.name

    @TypeConverter
    fun toMediaType(value: String?): MediaType? = value?.let {
        runCatching { MediaType.valueOf(it) }.getOrDefault(MediaType.DIRECT_FILE)
    }

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus?): String? = value?.name

    @TypeConverter
    fun toDownloadStatus(value: String?): DownloadStatus? = value?.let {
        runCatching { DownloadStatus.valueOf(it) }.getOrDefault(DownloadStatus.QUEUED)
    }
}
