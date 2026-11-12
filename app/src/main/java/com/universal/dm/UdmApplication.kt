package com.universal.dm

import android.app.Application
import android.content.Context
import android.util.Log
import com.universal.dm.data.local.DownloadDatabase
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UdmApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: DownloadDatabase by lazy {
        DownloadDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        applicationScope.launch {
            try {
                YoutubeDL.getInstance().init(this@UdmApplication)
                FFmpeg.getInstance().init(this@UdmApplication)
                Log.d(TAG, "YoutubeDL and FFmpeg initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize YoutubeDL or FFmpeg", e)
            }
        }
    }

    companion object {
        private const val TAG = "UdmApplication"

        lateinit var instance: UdmApplication
            private set

        fun getDatabase(context: Context): DownloadDatabase {
            return (context.applicationContext as? UdmApplication)?.database
                ?: DownloadDatabase.getInstance(context)
        }
    }
}
