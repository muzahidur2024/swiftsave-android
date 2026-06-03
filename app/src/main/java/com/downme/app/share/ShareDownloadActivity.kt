package com.downme.app.share

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.downme.app.BuildConfig
import com.downme.app.R
import com.downme.app.DownMeApplication
import com.downme.app.data.DownloadEntity
import com.downme.app.data.DownloadStatus
import com.downme.app.download.DownloadForegroundService
import com.downme.app.util.ShareIntentParser
import com.downme.app.util.YtDlpFormats
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Invisible handler for Share → DownMe. Starts the download in the background and finishes
 * immediately so the user stays in the app they shared from (Facebook, browser, etc.).
 */
class ShareDownloadActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = ShareIntentParser.urlFromSendIntent(intent)
        if (url == null) {
            finish()
            return
        }
        val app = application as DownMeApplication
        lifecycleScope.launch {
            try {
                val quality = YtDlpFormats.normalizeQuality(app.userPreferences.defaultQuality.first())
                val jobId = UUID.randomUUID().toString().replace("-", "").take(32)
                val now = System.currentTimeMillis()
                withContext(Dispatchers.IO) {
                    app.database.downloadDao().upsert(
                        DownloadEntity(
                            id = jobId,
                            title = getString(R.string.preparing_download),
                            sourceUrl = url,
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
                }
                DownloadForegroundService.startDownload(this@ShareDownloadActivity, jobId, url, quality)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Failed to start shared download", e)
            } finally {
                moveTaskToBack(true)
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "ShareDownloadActivity"
    }
}
