package com.downme.app.util

import android.content.Context
import android.util.Log
import com.downme.app.BuildConfig
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.R as YtdlpLibR
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import java.io.File
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
                val appContext = context.applicationContext
                try {
                    // Touch the same @raw/ytdlp resource the library reads during init().
                    appContext.resources.openRawResource(YtdlpLibR.raw.ytdlp).use { }
                    val youtubeDl = YoutubeDL.getInstance()
                    youtubeDl.init(appContext)
                    FFmpeg.getInstance().init(appContext)
                    verifyEngineFiles(appContext)
                    initialized = true
                    if (BuildConfig.DEBUG) Log.i(TAG, "YouTube-DL / FFmpeg ready")
                } catch (e: YoutubeDLException) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Failed to initialize YouTube-DL / FFmpeg", e)
                    throw e
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Failed to initialize download engine", e)
                    throw YoutubeDLException("Download engine failed to start", e)
                }
            }
        }
    }

    private fun verifyEngineFiles(context: Context) {
        val baseDir = File(context.noBackupFilesDir, YoutubeDL.baseName)
        val ytdlpScript = File(File(baseDir, YoutubeDL.ytdlpDirName), YoutubeDL.ytdlpBin)
        if (!ytdlpScript.exists() || ytdlpScript.length() < 1024) {
            throw YoutubeDLException("yt-dlp binary missing after init")
        }
        val pythonZip = File(context.applicationInfo.nativeLibraryDir, "libpython.zip.so")
        if (!pythonZip.exists()) {
            throw YoutubeDLException("Python runtime missing from APK")
        }
    }
}
