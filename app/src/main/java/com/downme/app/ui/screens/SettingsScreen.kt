package com.downme.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downme.app.R
import com.downme.app.DownMeApplication
import com.downme.app.data.AppThemeMode
import com.downme.app.ui.components.AboutDownMeSection
import com.downme.app.ui.components.AppUpdateSection
import com.downme.app.ui.components.LegalSupportSection
import com.downme.app.util.CustomDownloadFolder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as DownMeApplication
    val quality by app.userPreferences.defaultQuality.collectAsStateWithLifecycle(initialValue = "1080")
    val customFolderUri by app.userPreferences.customDownloadFolderUri.collectAsStateWithLifecycle(
        initialValue = null,
    )
    val themeMode by app.userPreferences.themeMode.collectAsStateWithLifecycle(
        initialValue = AppThemeMode.Dark,
    )
    val showQualityPrompt by app.userPreferences.showQualityPrompt.collectAsStateWithLifecycle(
        initialValue = true,
    )
    val scope = rememberCoroutineScope()
    val qualities = remember { QUALITY_PRESETS }

    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                CustomDownloadFolder.persistTreePermission(context, uri)
                scope.launch {
                    app.userPreferences.setCustomDownloadFolderUri(uri.toString())
                }
            }
        }

    val folderLabel =
        remember(customFolderUri) {
            CustomDownloadFolder.displayLabel(context, customFolderUri)
        }
    val folderAccessible =
        remember(customFolderUri) {
            CustomDownloadFolder.isAccessible(context, customFolderUri)
        }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.tab_settings)) })
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            AppUpdateSection()
            SettingsDivider()
            Text(
                stringResource(R.string.settings_intro_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stringResource(R.string.settings_intro_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SettingsSectionTitle(stringResource(R.string.theme_title))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val selected = themeMode == mode
                    AssistChip(
                        onClick = { scope.launch { app.userPreferences.setThemeMode(mode) } },
                        label = { Text(stringResource(mode.labelRes)) },
                        colors = chipColors(selected),
                    )
                }
            }

            SettingsDivider()

            SettingsSectionTitle(stringResource(R.string.download_path_title))
            when {
                customFolderUri.isNullOrBlank() -> {
                    Text(
                        stringResource(R.string.download_path_not_set),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                folderAccessible && !folderLabel.isNullOrBlank() -> {
                    Text(
                        stringResource(R.string.download_path_selected, folderLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    Text(
                        stringResource(R.string.download_path_permission_lost),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Text(
                stringResource(R.string.download_path_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            if (customFolderUri.isNullOrBlank()) {
                Button(
                    onClick = { folderPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.download_path_choose))
                }
            } else {
                OutlinedButton(
                    onClick = { folderPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.download_path_change))
                }
            }
            Text(
                stringResource(R.string.download_path_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth(),
            )

            SettingsDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SettingsSectionTitle(stringResource(R.string.quality_prompt_title))
                    Text(
                        stringResource(R.string.quality_prompt_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = showQualityPrompt,
                    onCheckedChange = { enabled ->
                        scope.launch { app.userPreferences.setShowQualityPrompt(enabled) }
                    },
                )
            }

            SettingsDivider()

            SettingsSectionTitle(stringResource(R.string.default_quality_title))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                qualities.forEach { q ->
                    val selected = quality == q.key
                    AssistChip(
                        onClick = {
                            scope.launch { app.userPreferences.setDefaultQuality(q.key) }
                        },
                        label = { Text(q.label) },
                        colors = chipColors(selected),
                    )
                }
            }

            SettingsDivider()

            AboutDownMeSection()

            LegalSupportSection()

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun chipColors(selected: Boolean) =
    AssistChipDefaults.assistChipColors(
        containerColor =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        labelColor =
            if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
