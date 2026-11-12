package com.universal.dm.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "download_chunks",
    foreignKeys = [
        ForeignKey(
            entity = DownloadTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class DownloadChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val chunkId: Long = 0L,
    val taskId: String,
    val threadIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val currentByte: Long,
    val isCompleted: Boolean = false
)
