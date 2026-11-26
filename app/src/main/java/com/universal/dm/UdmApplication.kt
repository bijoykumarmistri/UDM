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
        // Pre-warm YoutubeDL and FFmpeg asynchronously on appScope so media extraction
        // executes instantly with 0 cold-start initialization latency when a video is loaded.
        appScope.launch {
            ensureYoutubeDlInitialized(this@UdmApplication)
        }
    }

    companion object {
        private const val TAG = "UdmApplication"
        private val isYoutubeDlInitialized = AtomicBoolean(false)
        private val youtubeDlMutex = Mutex()

        /**
         * Application-scoped supervisor coroutine scope for background initialization,
         * settings propagation, and non-UI async work without leaking per-call scopes.
         */
        val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
                            Log.d(
                                TAG,
                                "YoutubeDL and FFmpeg initialized successfully, version: ${
                                    runCatching { YoutubeDL.getInstance().version(appContext) }.getOrNull()
                                }"
                            )
                            appScope.launch {
                                runCatching {
                                    val status = YoutubeDL.getInstance().updateYoutubeDL(
                                        appContext,
                                        YoutubeDL.UpdateChannel.NIGHTLY
                                    )
                                    Log.d(
                                        TAG,
                                        "Background yt-dlp update status: $status, version: ${
                                            runCatching { YoutubeDL.getInstance().version(appContext) }.getOrNull()
                                        }"
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to initialize YoutubeDL or FFmpeg", e)
                        }
                    }
                }
            }
        }

        suspend fun updateYoutubeDl(
            context: Context,
            channel: YoutubeDL.UpdateChannel = YoutubeDL.UpdateChannel.NIGHTLY
        ): YoutubeDL.UpdateStatus? {
            return withContext(Dispatchers.IO) {
                try {
                    val appContext = context.applicationContext
                    val status = YoutubeDL.getInstance().updateYoutubeDL(appContext, channel)
                    Log.d(
                        TAG,
                        "YoutubeDL update status: $status, current version: ${
                            runCatching { YoutubeDL.getInstance().version(appContext) }.getOrNull()
                        }"
                    )
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
