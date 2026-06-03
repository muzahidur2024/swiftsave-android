package com.downme.app.download

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.downme.app.MainActivity
import com.downme.app.R
import com.downme.app.DownMeApplication
import com.downme.app.data.DownloadEntity
import com.downme.app.data.DownloadStatus
import com.downme.app.util.SavedMediaPublisher
import kotlinx.coroutines.flow.first
import com.downme.app.util.UrlUtils
import com.downme.app.util.YoutubeDlInitializer
import com.downme.app.util.YtDlpFormats
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DownloadForegroundService : Service() {

    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(supervisor + Dispatchers.IO)
    private val jobsInFlight = AtomicInteger(0)
    private val serialLock = Mutex()
    private val activeJobIds = ConcurrentHashMap.newKeySet<String>()
    private val cancelledJobIds = ConcurrentHashMap.newKeySet<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID)
                if (!jobId.isNullOrBlank() && isValidJobId(jobId)) {
                    cancelDownload(jobId)
                }
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID) ?: return START_NOT_STICKY
                if (!isValidJobId(jobId)) return START_NOT_STICKY
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val normalizedUrl = UrlUtils.normalizeDownloadUrl(url) ?: return START_NOT_STICKY
                val quality = intent.getStringExtra(EXTRA_QUALITY) ?: "1080"
                jobsInFlight.incrementAndGet()
                activeJobIds.add(jobId)
                cancelledJobIds.remove(jobId)
                scope.launch {
                    try {
                        serialLock.withLock {
                            runDownload(jobId, normalizedUrl, quality)
                        }
                    } finally {
                        activeJobIds.remove(jobId)
                        cancelledJobIds.remove(jobId)
                        if (jobsInFlight.decrementAndGet() == 0) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        activeJobIds.forEach { jobId ->
            cancelledJobIds.add(jobId)
            runCatching { YoutubeDL.getInstance().destroyProcessById(jobId) }
        }
        supervisor.cancel()
        super.onDestroy()
    }

    private fun cancelDownload(jobId: String) {
        cancelledJobIds.add(jobId)
        runCatching { YoutubeDL.getInstance().destroyProcessById(jobId) }
        scope.launch {
            (applicationContext as DownMeApplication).database.downloadDao().updateStatus(
                jobId,
                DownloadStatus.CANCELLED,
                getString(R.string.download_cancelled),
            )
        }
    }

    private suspend fun runDownload(jobId: String, url: String, qualityRaw: String) {
        val app = applicationContext as DownMeApplication
        val dao = app.database.downloadDao()
        val downloadUrl = UrlUtils.normalizeDownloadUrl(url) ?: return
        val quality = YtDlpFormats.normalizeQuality(qualityRaw)
        var displayTitle = getString(R.string.preparing_download)
        startForeground(
            NOTIFICATION_ID,
            buildProgressNotification(
                displayTitle,
                getString(R.string.preparing_download),
                0,
                jobId,
                indeterminate = true,
            ),
        )

        try {
            val prep = getString(R.string.preparing_download)
            YoutubeDlInitializer.ensureInitialized(applicationContext)
            YoutubeDL.getInstance().destroyProcessById(jobId)

            val hostLabel =
                try {
                    Uri.parse(downloadUrl).host?.removePrefix("www.")?.takeIf { it.isNotBlank() }
                } catch (_: Throwable) {
                    null
                }
            val existing = dao.getById(jobId)
            val now = existing?.createdAt ?: System.currentTimeMillis()
            val initialTitle =
                when {
                    existing?.title?.isNotBlank() == true && existing.title != prep -> existing.title
                    hostLabel != null -> "$prep ($hostLabel)"
                    else -> prep
                }
            dao.upsert(
                DownloadEntity(
                    id = jobId,
                    title = initialTitle,
                    sourceUrl = downloadUrl,
                    filePath = null,
                    fileSize = null,
                    durationSec = existing?.durationSec,
                    thumbnailUrl = existing?.thumbnailUrl,
                    createdAt = now,
                    status = DownloadStatus.DOWNLOADING,
                    progress = 0,
                    errorMessage = null,
                    quality = quality,
                ),
            )

            val streamInfo =
                try {
                    YoutubeDL.getInstance().getInfo(downloadUrl)
                } catch (_: Throwable) {
                    null
                }
            val title = streamInfo?.title?.takeIf { it.isNotBlank() } ?: hostLabel ?: "Video"
            displayTitle = title
            val thumb = streamInfo?.thumbnail
            val durationSec =
                streamInfo?.duration?.let { v -> (v as? Number)?.toDouble() }
            dao.upsert(
                DownloadEntity(
                    id = jobId,
                    title = title,
                    sourceUrl = downloadUrl,
                    filePath = null,
                    fileSize = null,
                    durationSec = durationSec,
                    thumbnailUrl = thumb,
                    createdAt = now,
                    status = DownloadStatus.DOWNLOADING,
                    progress = 0,
                    errorMessage = null,
                    quality = quality,
                ),
            )

            val saveLocation = app.userPreferences.downloadLocation.first()
            val outDir = SavedMediaPublisher.stagingDir(applicationContext)
            clearStaleOutputs(outDir, jobId)
            val outputTemplate = File(outDir, jobId).absolutePath + ".%(ext)s"
            val request = YoutubeDLRequest(downloadUrl)
            request.addOption("-o", outputTemplate)
            YtDlpFormats.applyForUrl(request, downloadUrl, quality)
            val response =
                YoutubeDL.getInstance().execute(
                    request,
                    jobId,
                    redirectErrorStream = true,
                ) { progress, _, _ ->
                    val p = normalizePercent(progress)
                    scope.launch {
                        if (!cancelledJobIds.contains(jobId)) {
                            dao.updateProgress(jobId, p)
                            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                            nm.notify(
                                NOTIFICATION_ID,
                                buildProgressNotification(
                                    displayTitle,
                                    getString(R.string.notification_download_progress, p),
                                    p,
                                    jobId,
                                ),
                            )
                        }
                    }
                }
            if (cancelledJobIds.contains(jobId)) {
                throw YoutubeDL.CanceledException()
            }
            if (response.exitCode != 0) {
                val err = response.err?.trim().orEmpty().ifBlank { "Exit code ${response.exitCode}" }
                throw DownloadException(err)
            }
            val file = resolveOutputFile(outDir, jobId)
                ?: throw IllegalStateException("Downloaded file not found")
            val sizeBytes = file.length()
            val publishedPath =
                SavedMediaPublisher.publishDownload(
                    applicationContext,
                    file,
                    title,
                    jobId,
                    saveLocation,
                )
            val storedPath = publishedPath ?: file.absolutePath
            dao.markComplete(
                id = jobId,
                status = DownloadStatus.DONE,
                path = storedPath,
                size = sizeBytes,
            )
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(
                NOTIFICATION_ID,
                buildProgressNotification(
                    title,
                    getString(R.string.download_complete),
                    100,
                    null,
                    indeterminate = false,
                ),
            )
        } catch (e: Throwable) {
            val wasCancelled =
                cancelledJobIds.contains(jobId) ||
                    e is YoutubeDL.CanceledException ||
                    e is CancellationException
            val status = if (wasCancelled) DownloadStatus.CANCELLED else DownloadStatus.FAILED
            val stderr = (e as? DownloadException)?.stderr.orEmpty()
            val msg =
                when {
                    wasCancelled -> getString(R.string.download_cancelled)
                    e is YoutubeDLException ->
                        e.message?.take(200) ?: getString(R.string.download_error_engine)
                    YtDlpFormats.isFormatUnavailableError(stderr) ->
                        getString(R.string.download_error_format)
                    stderr.isNotBlank() ->
                        YtDlpFormats.summarizeError(stderr) ?: getString(R.string.download_error_safe)
                    else -> getString(R.string.download_error_safe)
                }
            dao.updateStatus(
                jobId,
                status,
                msg,
            )
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            val line =
                if (wasCancelled) {
                    msg
                } else {
                    getString(R.string.download_failed, msg)
                }
            nm.notify(
                NOTIFICATION_ID,
                buildProgressNotification(
                    displayTitle,
                    line,
                    0,
                    null,
                    indeterminate = false,
                ),
            )
        }
    }

    private class DownloadException(val stderr: String) : IllegalStateException(stderr)

    private fun normalizePercent(raw: Float): Int {
        if (raw.isNaN() || raw < 0f) return 0
        if (raw > 1f) {
            return raw.roundToInt().coerceIn(0, 100)
        }
        return (raw * 100f).roundToInt().coerceIn(0, 100)
    }

    private fun clearStaleOutputs(outDir: File, jobId: String) {
        outDir.listFiles()?.forEach { f ->
            if (f.isFile && f.name.startsWith(jobId)) {
                f.delete()
            }
        }
    }

    private fun resolveOutputFile(dir: File, jobId: String): File? {
        val exts = listOf("mp4", "webm", "mkv", "m4a", "mp3", "opus", "flac")
        for (ext in exts) {
            val f = File(dir, "$jobId.$ext")
            if (f.isFile && f.length() > 0) return f
        }
        return dir.listFiles()
            ?.filter { f ->
                f.isFile &&
                    f.name.startsWith(jobId) &&
                    !f.name.endsWith(".part") &&
                    !f.name.endsWith(".ytdl")
            }
            ?.maxByOrNull { it.length() }
            ?.takeIf { it.length() > 0 }
    }

    private fun buildProgressNotification(
        title: String,
        line: String,
        progress: Int,
        jobId: String?,
        indeterminate: Boolean = false,
    ): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancel = PendingIntent.getService(
            this,
            1,
            Intent(this, DownloadForegroundService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_JOB_ID, jobId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val p = progress.coerceIn(0, 100)
        val b = NotificationCompat.Builder(this, DownMeApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(line)
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
        if (indeterminate) {
            b.setProgress(0, 0, true)
            b.setOngoing(true)
        } else {
            b.setProgress(100, p, false)
            b.setOngoing(p in 1..99)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            b.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
        if (!jobId.isNullOrBlank()) {
            b.addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.cancel_download), cancel)
        }
        return b.build()
    }

    companion object {
        private const val NOTIFICATION_ID = 7101
        const val ACTION_START = "com.downme.app.download.START"
        const val ACTION_CANCEL = "com.downme.app.download.CANCEL"
        const val EXTRA_JOB_ID = "job_id"
        const val EXTRA_URL = "url"
        const val EXTRA_QUALITY = "quality"

        fun startDownload(context: Context, jobId: String, url: String, quality: String) {
            val safeUrl = UrlUtils.normalizeDownloadUrl(url) ?: return
            if (!isValidJobId(jobId)) return
            val i = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_JOB_ID, jobId)
                putExtra(EXTRA_URL, safeUrl)
                putExtra(EXTRA_QUALITY, YtDlpFormats.normalizeQuality(quality))
            }
            ContextCompat.startForegroundService(context, i)
        }

        private fun isValidJobId(jobId: String): Boolean =
            jobId.length in 8..64 && jobId.all { it.isLetterOrDigit() }

        fun cancelDownload(context: Context, jobId: String) {
            if (!isValidJobId(jobId)) return
            val i = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_JOB_ID, jobId)
            }
            context.startService(i)
        }
    }
}
