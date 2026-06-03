package com.downme.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downme.app.R
import com.downme.app.DownMeApplication
import com.downme.app.data.DownloadEntity
import com.downme.app.data.DownloadStatus
import com.downme.app.download.DownloadForegroundService
import com.downme.app.ui.components.SupportContactBlock
import com.downme.app.util.UrlUtils
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTutorial: () -> Unit,
    pendingSharedDownloadUrl: String? = null,
    onPendingSharedDownloadUrlConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as DownMeApplication
    val defaultQuality by app.userPreferences.defaultQuality.collectAsStateWithLifecycle(initialValue = "1080")
    val showQualityPrompt by app.userPreferences.showQualityPrompt.collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()
    var urlText by remember { mutableStateOf("") }
    var pendingDownloadUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetQuality by rememberSaveable { mutableStateOf(defaultQuality) }

    val qualities = remember { QUALITY_PRESETS }

    fun startDownload(candidate: String, quality: String) {
        val jobId = UUID.randomUUID().toString().replace("-", "").take(32)
        val now = System.currentTimeMillis()
        scope.launch {
            app.database.downloadDao().upsert(
                DownloadEntity(
                    id = jobId,
                    title = context.getString(R.string.preparing_download),
                    sourceUrl = candidate,
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
            DownloadForegroundService.startDownload(
                context,
                jobId,
                candidate,
                quality,
            )
        }
    }

    LaunchedEffect(pendingSharedDownloadUrl) {
        val raw = pendingSharedDownloadUrl ?: return@LaunchedEffect
        val candidate = UrlUtils.normalizeDownloadUrl(raw) ?: raw.trim()
        if (candidate.isBlank() || !UrlUtils.isSafeUrl(candidate)) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.paste_valid_url),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
            onPendingSharedDownloadUrlConsumed()
            return@LaunchedEffect
        }
        if (showQualityPrompt) {
            sheetQuality = defaultQuality
            pendingDownloadUrl = candidate
        } else {
            startDownload(candidate, defaultQuality)
        }
        onPendingSharedDownloadUrlConsumed()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenTutorial) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = stringResource(R.string.how_to_download))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(R.string.home_hero_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    stringResource(R.string.home_hero_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.home_link_label)) },
                placeholder = { Text("https://…") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            Button(
                onClick = {
                    val candidate = UrlUtils.normalizeDownloadUrl(urlText) ?: urlText.trim()
                    if (candidate.isBlank() || !UrlUtils.isSafeUrl(candidate)) {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.paste_valid_url),
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                        return@Button
                    }
                    if (showQualityPrompt) {
                        sheetQuality = defaultQuality
                        pendingDownloadUrl = candidate
                    } else {
                        startDownload(candidate, defaultQuality)
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.start_download))
            }
            Text(
                stringResource(R.string.home_download_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            HorizontalDivider(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            AboutSection()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    pendingDownloadUrl?.let { candidate ->
        ModalBottomSheet(
            onDismissRequest = { pendingDownloadUrl = null },
        ) {
            QualityDownloadSheet(
                qualities = qualities,
                selectedQuality = sheetQuality,
                onQualityChange = { sheetQuality = it },
                onDownload = {
                    startDownload(candidate, sheetQuality)
                    pendingDownloadUrl = null
                },
            )
        }
    }
}

@Composable
private fun QualityDownloadSheet(
    qualities: List<QualityPreset>,
    selectedQuality: String,
    onQualityChange: (String) -> Unit,
    onDownload: () -> Unit,
) {
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
        val music = qualities.filter { it.key == "mp3" }
        val video = qualities.filterNot { it.key == "mp3" }
        QualityGrid(
            title = stringResource(R.string.quality_sheet_video),
            qualities = video,
            selectedQuality = selectedQuality,
            onQualityChange = onQualityChange,
        )
        QualityGrid(
            title = stringResource(R.string.quality_sheet_music),
            qualities = music,
            selectedQuality = selectedQuality,
            onQualityChange = onQualityChange,
            columns = 1,
        )
        Button(
            onClick = onDownload,
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

@Composable
private fun QualityGrid(
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
                QualityOptionCard(
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
private fun QualityOptionCard(
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

private val AboutLineHeight =
    LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = Trim.None,
    )

@Composable
private fun AboutSection() {
    val bodyStyle =
        MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 24.sp,
            lineHeightStyle = AboutLineHeight,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
        )
    val titleStyle =
        MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.primary,
        )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.about_section_header),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )
        AboutCard(title = stringResource(R.string.about_app_title), titleStyle = titleStyle) {
            Text(text = stringResource(R.string.about_app_body), style = bodyStyle)
        }
        AboutCard(title = stringResource(R.string.about_disclaimer_title), titleStyle = titleStyle) {
            Text(
                text = stringResource(R.string.about_disclaimer_body),
                style =
                    bodyStyle.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        }
        AboutCard(title = stringResource(R.string.about_contact_title), titleStyle = titleStyle) {
            SupportContactBlock()
        }
    }
}

@Composable
private fun AboutCard(
    title: String,
    titleStyle: androidx.compose.ui.text.TextStyle,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = title, style = titleStyle)
            content()
        }
    }
}
