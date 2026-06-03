package com.downme.app.ui.screens



import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.widthIn

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.Button

import androidx.compose.material3.CenterAlignedTopAppBar

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.HorizontalDivider

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

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.downme.app.R

import com.downme.app.DownMeApplication

import com.downme.app.data.DownloadEntity

import com.downme.app.data.DownloadStatus

import com.downme.app.download.DownloadForegroundService

import com.downme.app.ui.components.HowToDownloadSection

import com.downme.app.ui.components.QualityPickerSheetContent

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



    fun startDownload(candidate: String, quality: String) {

        val jobId = UUID.randomUUID().toString().replace("-", "").take(32)

        val now = System.currentTimeMillis()

        scope.launch {

            app.userPreferences.setDefaultQuality(quality)

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



    LaunchedEffect(defaultQuality, pendingDownloadUrl) {

        if (pendingDownloadUrl == null) {

            sheetQuality = defaultQuality

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

                        style = MaterialTheme.typography.titleLarge,

                        color = MaterialTheme.colorScheme.primary,

                    )

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

        ) {

            Column(

                modifier =

                    Modifier

                        .widthIn(max = 400.dp)

                        .fillMaxWidth(),

                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement = Arrangement.spacedBy(14.dp),

            ) {

                Spacer(modifier = Modifier.height(16.dp))



                Text(

                    stringResource(R.string.home_hero_title),

                    style = MaterialTheme.typography.titleMedium,

                    color = MaterialTheme.colorScheme.onBackground,

                    textAlign = TextAlign.Center,

                    modifier = Modifier.fillMaxWidth(),

                )

                Text(

                    stringResource(R.string.home_hero_body),

                    style = MaterialTheme.typography.bodySmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                    textAlign = TextAlign.Center,

                    modifier = Modifier.fillMaxWidth(),

                )



                Spacer(modifier = Modifier.height(8.dp))



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

                    style = MaterialTheme.typography.labelSmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                    textAlign = TextAlign.Center,

                    modifier = Modifier.fillMaxWidth(),

                )



                HorizontalDivider(

                    modifier =

                        Modifier

                            .fillMaxWidth()

                            .padding(top = 20.dp, bottom = 4.dp),

                    color = MaterialTheme.colorScheme.outlineVariant,

                )



                HowToDownloadSection(onOpenTutorial = onOpenTutorial)



                Spacer(modifier = Modifier.height(32.dp))

            }

        }

    }



    pendingDownloadUrl?.let { candidate ->

        ModalBottomSheet(

            onDismissRequest = { pendingDownloadUrl = null },

        ) {

            QualityPickerSheetContent(

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


