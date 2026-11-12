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

    fun deleteCompletedTask(task: DownloadTaskEntity) {
        viewModelScope.launch {
            if (!task.filePath.startsWith("content://")) {
                val file = File(task.filePath)
                if (file.exists()) file.delete()
            }
            downloadDao.deleteTask(task)
        }
    }
}
