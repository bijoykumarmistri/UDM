package com.universal.dm.ui.finished

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.universal.dm.UdmApplication
import com.universal.dm.data.local.DownloadTaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class FinishedViewModel(application: Application) : AndroidViewModel(application) {

    private val database = UdmApplication.getDatabase(application)
    private val downloadDao = database.downloadDao()

    val completedTasks: StateFlow<List<DownloadTaskEntity>> = downloadDao.getCompletedTasksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _previewTask = MutableStateFlow<DownloadTaskEntity?>(null)
    val previewTask: StateFlow<DownloadTaskEntity?> = _previewTask.asStateFlow()

    fun selectTaskForPreview(task: DownloadTaskEntity?) {
        _previewTask.value = task
    }

    /**
     * Deletes the task entry from the database.
     * If deleteFileFromStorage is true, also deletes the physical file from disk/storage.
     */
    fun deleteTask(task: DownloadTaskEntity, deleteFileFromStorage: Boolean) {
        viewModelScope.launch {
            if (deleteFileFromStorage && !task.filePath.startsWith("content://")) {
                val file = File(task.filePath)
                if (file.exists()) {
                    runCatching { file.delete() }
                }
            }
            downloadDao.deleteTask(task)
        }
    }

    /**
     * Deletes a collection of tasks from the database and optionally removes physical files.
     */
    fun deleteMultipleTasks(tasks: List<DownloadTaskEntity>, deleteFromStorage: Boolean) {
        viewModelScope.launch {
            tasks.forEach { task ->
                if (deleteFromStorage && !task.filePath.startsWith("content://")) {
                    val file = File(task.filePath)
                    if (file.exists()) {
                        runCatching { file.delete() }
                    }
                }
                downloadDao.deleteTask(task)
            }
        }
    }

    /**
     * Clears all completed downloads history.
     */
    fun clearAllHistory(deleteFromStorage: Boolean) {
        viewModelScope.launch {
            completedTasks.value.forEach { task ->
                if (deleteFromStorage && !task.filePath.startsWith("content://")) {
                    val file = File(task.filePath)
                    if (file.exists()) {
                        runCatching { file.delete() }
                    }
                }
                downloadDao.deleteTask(task)
            }
        }
    }
}
