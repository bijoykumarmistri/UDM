package com.universal.dm

import android.app.Application
import android.content.Context
import android.util.Log
import com.universal.dm.data.local.DownloadDatabase
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class UdmApplication : Application() {

    val database: DownloadDatabase by lazy {
        DownloadDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // YoutubeDL & FFmpeg initialization is completely on-demand via ensureYoutubeDlInitialized()
        // when a stream extraction or sniffer action is executed. This guarantees 100% CPU & I/O
        // bandwidth for cold start and initial app interactions.
    }

    companion object {
        private const val TAG = "UdmApplication"
        private val isYoutubeDlInitialized = AtomicBoolean(false)
        private val youtubeDlMutex = Mutex()

        lateinit var instance: UdmApplication
            private set

        suspend fun ensureYoutubeDlInitialized(context: Context) {
            if (isYoutubeDlInitialized.get()) return
            youtubeDlMutex.withLock {
                if (!isYoutubeDlInitialized.get()) {
                    withContext(Dispatchers.IO) {
                        try {
                            val appContext = context.applicationContext
                            YoutubeDL.getInstance().init(appContext)
                            FFmpeg.getInstance().init(appContext)
                            isYoutubeDlInitialized.set(true)
                            Log.d(TAG, "YoutubeDL and FFmpeg initialized successfully, version: ${runCatching { YoutubeDL.getInstance().version(appContext) }.getOrNull()}")
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                runCatching {
                                    val status = YoutubeDL.getInstance().updateYoutubeDL(appContext, YoutubeDL.UpdateChannel.NIGHTLY)
                                    Log.d(TAG, "Background yt-dlp update status: $status, version: ${YoutubeDL.getInstance().version(appContext)}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to initialize YoutubeDL or FFmpeg", e)
                        }
                    }
                }
            }
        }

        suspend fun updateYoutubeDl(context: Context, channel: YoutubeDL.UpdateChannel = YoutubeDL.UpdateChannel.NIGHTLY): YoutubeDL.UpdateStatus? {
            return withContext(Dispatchers.IO) {
                try {
                    val appContext = context.applicationContext
                    val status = YoutubeDL.getInstance().updateYoutubeDL(appContext, channel)
                    Log.d(TAG, "YoutubeDL update status: $status, current version: ${YoutubeDL.getInstance().version(appContext)}")
                    status
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update YoutubeDL", e)
                    null
                }
            }
        }

        fun getDatabase(context: Context): DownloadDatabase {
            return (context.applicationContext as? UdmApplication)?.database
                ?: DownloadDatabase.getInstance(context)
        }
    }
}
