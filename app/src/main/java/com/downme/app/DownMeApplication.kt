package com.downme.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.downme.app.data.AppDatabase
import com.downme.app.data.DownloadEntity
import com.downme.app.data.DownloadStatus
import com.downme.app.data.UserPreferencesRepository
import com.downme.app.data.buildAppDatabase
import com.downme.app.download.DownloadForegroundService
import com.downme.app.util.UrlUtils
import com.downme.app.util.YoutubeDlInitializer
import com.downme.app.util.YtDlpFormats
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DownMeApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var userPreferences: UserPreferencesRepository
        private set

    private val applicationJob = SupervisorJob()
    private val applicationScope = CoroutineScope(applicationJob + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = buildAppDatabase(this)
        userPreferences = UserPreferencesRepository(this)
        ensureNotificationChannel()
        markInterruptedDownloadsFailed()
        warmUpYoutubeDlInBackground()
    }

    /** Prepares yt-dlp/FFmpeg after UI is up so first download is quicker. */
    private fun warmUpYoutubeDlInBackground() {
        applicationScope.launch {
            runCatching { YoutubeDlInitializer.ensureInitialized(this@DownMeApplication) }
                .onFailure { e ->
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Background yt-dlp warm-up failed", e)
                    }
                }
        }
    }

    private fun markInterruptedDownloadsFailed() {
        applicationScope.launch {
            database.downloadDao().updateAllWithStatus(
                DownloadStatus.DOWNLOADING,
                DownloadStatus.FAILED,
                getString(R.string.download_interrupted),
            )
        }
    }

    fun enqueueDownloadFromSharedLink(url: String) {
        val safe = UrlUtils.normalizeDownloadUrl(url) ?: return
        applicationScope.launch {
            try {
                val quality = YtDlpFormats.normalizeQuality(userPreferences.defaultQuality.first())
                val jobId = UUID.randomUUID().toString().replace("-", "").take(32)
                val now = System.currentTimeMillis()
                database.downloadDao().upsert(
                    DownloadEntity(
                        id = jobId,
                        title = getString(R.string.preparing_download),
                        sourceUrl = safe,
                        filePath = null,
                        fileSize = null,
                        durationSec = null,
                        thumbnailUrl = null,
                        createdAt = now,
                        status = DownloadStatus.DOWNLOADING,
                        progress = 0,
                        errorMessage = null,
                        quality = quality,
                    ),
                )
                DownloadForegroundService.startDownload(this@DownMeApplication, jobId, safe, quality)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "enqueueDownloadFromSharedLink failed", e)
            }
        }
    }

    fun retryFailedDownload(item: DownloadEntity) {
        if (item.status != DownloadStatus.FAILED) return
        val url = UrlUtils.normalizeDownloadUrl(item.sourceUrl) ?: return
        applicationScope.launch {
            try {
                val quality =
                    YtDlpFormats.normalizeQuality(
                        item.quality ?: userPreferences.defaultQuality.first(),
                    )
                database.downloadDao().upsert(
                    item.copy(
                        sourceUrl = url,
                        status = DownloadStatus.DOWNLOADING,
                        progress = 0,
                        errorMessage = null,
                        filePath = null,
                        fileSize = null,
                        quality = quality,
                    ),
                )
                DownloadForegroundService.startDownload(
                    this@DownMeApplication,
                    item.id,
                    url,
                    quality,
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "retryFailedDownload failed", e)
            }
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.download_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "down_me_downloads"
        private const val TAG = "DownMe"
    }
}
