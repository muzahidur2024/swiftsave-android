package com.swiftsave.app.share

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.swiftsave.app.BuildConfig
import com.swiftsave.app.R
import com.swiftsave.app.SwiftSaveApplication
import com.swiftsave.app.data.DownloadEntity
import com.swiftsave.app.data.DownloadStatus
import com.swiftsave.app.download.DownloadForegroundService
import com.swiftsave.app.util.ShareIntentParser
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Invisible handler for Share → SwiftSave. Starts the download in the background and finishes
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
        val app = application as SwiftSaveApplication
        lifecycleScope.launch {
            try {
                val quality = app.userPreferences.defaultQuality.first()
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
