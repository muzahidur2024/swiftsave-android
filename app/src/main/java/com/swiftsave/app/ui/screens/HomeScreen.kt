package com.swiftsave.app.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.swiftsave.app.R
import com.swiftsave.app.SwiftSaveApplication
import com.swiftsave.app.data.DownloadEntity
import com.swiftsave.app.data.DownloadStatus
import com.swiftsave.app.download.DownloadForegroundService
import com.swiftsave.app.ui.components.PremiumHarvestContactBlock
import com.swiftsave.app.util.UrlUtils
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onOpenTutorial: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as SwiftSaveApplication
    val defaultQuality by app.userPreferences.defaultQuality.collectAsStateWithLifecycle(initialValue = "1080")
    val scope = rememberCoroutineScope()
    var urlText by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf(defaultQuality) }

    LaunchedEffect(defaultQuality) {
        quality = defaultQuality
    }

    val qualities = remember { QUALITY_PRESETS }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SwiftSave",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenTutorial) {
                        Icon(Icons.Default.HelpOutline, contentDescription = stringResource(R.string.how_to_download))
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.hint_search_url),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://…") },
                singleLine = true,
            )
            Text(
                stringResource(R.string.quality_label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                qualities.forEach { q ->
                    val selected = quality == q.key
                    AssistChip(
                        onClick = { quality = q.key },
                        label = { Text(q.label) },
                        colors =
                            AssistChipDefaults.assistChipColors(
                                containerColor =
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                labelColor = MaterialTheme.colorScheme.onSurface,
                            ),
                    )
                }
            }
            Button(
                onClick = {
                    val candidate = UrlUtils.extractUrl(urlText) ?: urlText.trim()
                    if (!UrlUtils.isSafeUrl(candidate)) {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.paste_valid_url),
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                        return@Button
                    }
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
                            ),
                        )
                        DownloadForegroundService.startDownload(
                            context,
                            jobId,
                            candidate,
                            quality,
                        )
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
            ) {
                Text(stringResource(R.string.start_download))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Text(
                    stringResource(R.string.storage_path),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                )
            }

            HorizontalDivider(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            )
            PremiumHarvestAboutSection()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private val AboutLineHeight =
    LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = Trim.None,
    )

@Composable
private fun PremiumHarvestAboutSection() {
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
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        AboutCard(title = stringResource(R.string.about_app_title), titleStyle = titleStyle) {
            Text(text = stringResource(R.string.about_app_body), style = bodyStyle)
        }
        AboutCard(title = stringResource(R.string.about_leadership_title), titleStyle = titleStyle) {
            Text(text = stringResource(R.string.about_leadership_body), style = bodyStyle)
        }
        AboutCard(title = stringResource(R.string.about_purity_title), titleStyle = titleStyle) {
            Text(text = stringResource(R.string.about_purity_body), style = bodyStyle)
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
            PremiumHarvestContactBlock()
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
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = title, style = titleStyle)
            content()
        }
    }
}
