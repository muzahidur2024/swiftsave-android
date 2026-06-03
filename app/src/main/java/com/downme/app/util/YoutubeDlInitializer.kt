package com.downme.app.util

import android.content.Context
import android.util.Log
import com.downme.app.BuildConfig
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Lazy, thread-safe init for bundled Python / yt-dlp / FFmpeg.
 * Avoids blocking cold start; first download waits until ready.
 */
object YoutubeDlInitializer {

    private const val TAG = "YoutubeDlInit"

    private val mutex = Mutex()

    @Volatile
    private var initialized = false

    suspend fun ensureInitialized(context: Context) {
        if (initialized) return
        mutex.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) {
                try {
                    val appContext = context.applicationContext
                    val youtubeDl = YoutubeDL.getInstance()
                    youtubeDl.init(appContext)
                    FFmpeg.getInstance().init(appContext)
                    runCatching { youtubeDl.updateYoutubeDL(appContext) }
                    initialized = true
                    if (BuildConfig.DEBUG) Log.i(TAG, "YouTube-DL / FFmpeg ready")
                } catch (e: YoutubeDLException) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Failed to initialize YouTube-DL / FFmpeg", e)
                    throw e
                }
            }
        }
    }
}
