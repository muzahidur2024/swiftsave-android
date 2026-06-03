package com.downme.app.share

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.downme.app.BuildConfig
import com.downme.app.DownMeApplication
import com.downme.app.R
import com.downme.app.data.AppThemeMode
import com.downme.app.data.DownloadEntity
import com.downme.app.data.DownloadStatus
import com.downme.app.download.DownloadForegroundService
import com.downme.app.ui.screens.QUALITY_PRESETS
import com.downme.app.ui.screens.QualityPreset
import com.downme.app.ui.theme.DownMeTheme
import com.downme.app.util.ShareIntentParser
import com.downme.app.util.YtDlpFormats
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handler for Share -> DownMe. Shows a transient quality picker over the source app when enabled,
 * then returns to that app after starting the foreground download.
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
                val defaultQuality = YtDlpFormats.normalizeQuality(app.userPreferences.defaultQuality.first())
                if (app.userPreferences.showQualityPrompt.first()) {
                    val themeMode = app.userPreferences.themeMode.first()
                    setContent {
                        DownMeTheme(themeMode = themeMode) {
                            SharedQualityPrompt(
                                defaultQuality = defaultQuality,
                                onDismiss = { finish() },
                                onDownload = { quality ->
                                    startSharedDownload(url, quality)
                                },
                            )
                        }
                    }
                    return@launch
                }
                startSharedDownload(url, defaultQuality)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Failed to start shared download", e)
                finish()
            }
        }
    }

    private fun startSharedDownload(url: String, qualityRaw: String) {
        lifecycleScope.launch {
            try {
                val quality = YtDlpFormats.normalizeQuality(qualityRaw)
                val app = application as DownMeApplication
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
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "ShareDownloadActivity"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharedQualityPrompt(
    defaultQuality: String,
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit,
) {
    var selectedQuality by remember(defaultQuality) { mutableStateOf(defaultQuality) }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
    ) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.quality_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                SharedQualityGrid(
                    title = stringResource(R.string.quality_sheet_video),
                    qualities = QUALITY_PRESETS.filterNot { it.key == "mp3" },
                    selectedQuality = selectedQuality,
                    onQualityChange = { selectedQuality = it },
                )
                SharedQualityGrid(
                    title = stringResource(R.string.quality_sheet_music),
                    qualities = QUALITY_PRESETS.filter { it.key == "mp3" },
                    selectedQuality = selectedQuality,
                    onQualityChange = { selectedQuality = it },
                    columns = 1,
                )
                Button(
                    onClick = { onDownload(selectedQuality) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                ) {
                    Text(stringResource(R.string.start_download))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SharedQualityGrid(
    title: String,
    qualities: List<QualityPreset>,
    selectedQuality: String,
    onQualityChange: (String) -> Unit,
    columns: Int = 2,
) {
    if (qualities.isEmpty()) return
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    qualities.chunked(columns).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            rowItems.forEach { quality ->
                SharedQualityOptionCard(
                    quality = quality,
                    selected = selectedQuality == quality.key,
                    onClick = { onQualityChange(quality.key) },
                    modifier = Modifier.weight(1f),
                )
            }
            repeat(columns - rowItems.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SharedQualityOptionCard(
    quality: QualityPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                quality.label,
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}
