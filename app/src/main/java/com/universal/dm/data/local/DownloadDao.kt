package com.universal.dm.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.universal.dm.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun getAllTasksFlow(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status IN ('QUEUED', 'DOWNLOADING', 'MERGING_MUXING', 'PAUSED') ORDER BY createdAt DESC")
    fun getActiveTasksFlow(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status = 'COMPLETED' ORDER BY createdAt DESC")
    fun getCompletedTasksFlow(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE id = :id")
    suspend fun getTaskById(id: String): DownloadTaskEntity?

    @Query("SELECT * FROM download_tasks WHERE id = :id")
    fun getTaskFlowById(id: String): Flow<DownloadTaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTaskEntity)

    @Update
    suspend fun updateTask(task: DownloadTaskEntity)

    @Delete
    suspend fun deleteTask(task: DownloadTaskEntity)

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("UPDATE download_tasks SET downloadedBytes = :downloadedBytes, totalBytes = CASE WHEN :totalBytes > 0 THEN :totalBytes ELSE totalBytes END, speedBytesPerSec = :speedBytesPerSec, status = :status WHERE id = :taskId")
    suspend fun updateTaskProgress(taskId: String, downloadedBytes: Long, totalBytes: Long, speedBytesPerSec: Long, status: DownloadStatus)

    @Query("UPDATE download_tasks SET downloadedBytes = :downloadedBytes, speedBytesPerSec = :speedBytesPerSec, status = :status WHERE id = :taskId")
    suspend fun updateTaskProgress(taskId: String, downloadedBytes: Long, speedBytesPerSec: Long, status: DownloadStatus)

    @Query("UPDATE download_tasks SET status = :status, errorMessage = :errorMessage WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: DownloadStatus, errorMessage: String? = null)

    @Query("SELECT * FROM download_chunks WHERE taskId = :taskId ORDER BY threadIndex ASC")
    suspend fun getChunksForTask(taskId: String): List<DownloadChunkEntity>

    @Query("SELECT * FROM download_chunks WHERE taskId = :taskId ORDER BY threadIndex ASC")
    fun getChunksFlowForTask(taskId: String): Flow<List<DownloadChunkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<DownloadChunkEntity>)

    @Update
    suspend fun updateChunk(chunk: DownloadChunkEntity)

    @Query("UPDATE download_chunks SET currentByte = :currentByte, isCompleted = :isCompleted WHERE chunkId = :chunkId")
    suspend fun updateChunkProgress(chunkId: Long, currentByte: Long, isCompleted: Boolean)

    @Query("DELETE FROM download_chunks WHERE taskId = :taskId")
    suspend fun deleteChunksForTask(taskId: String)
}
