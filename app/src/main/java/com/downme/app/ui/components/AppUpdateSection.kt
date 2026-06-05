package com.downme.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downme.app.BuildConfig
import com.downme.app.DownMeApplication
import com.downme.app.R
import com.downme.app.util.AppUpdateError
import com.downme.app.util.AppUpdateInstaller
import com.downme.app.util.AppUpdateState

@Composable
fun AppUpdateSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as DownMeApplication
    val state by app.appUpdateController.state.collectAsStateWithLifecycle()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.update_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.update_current_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (val s = state) {
                AppUpdateState.Idle -> {
                    Button(
                        onClick = { app.appUpdateController.checkForUpdate() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.update_check_button))
                    }
                }
                AppUpdateState.Checking -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        stringResource(R.string.update_checking),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AppUpdateState.UpToDate -> {
                    Text(
                        stringResource(R.string.update_up_to_date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedButton(
                        onClick = { app.appUpdateController.checkForUpdate() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.update_check_again))
                    }
                }
                is AppUpdateState.Available -> {
                    val sizeSuffix =
                        s.info.sizeMb?.let { mb ->
                            stringResource(R.string.update_size_mb, mb)
                        }.orEmpty()
                    Text(
                        stringResource(
                            R.string.update_available,
                            s.info.versionName,
                            sizeSuffix,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Button(
                        onClick = { app.appUpdateController.downloadUpdate(s.info) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.update_download_button))
                    }
                }
                is AppUpdateState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { s.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(R.string.update_downloading, s.progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is AppUpdateState.ReadyToInstall -> {
                    Text(
                        stringResource(R.string.update_ready_install, s.info.versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Button(
                        onClick = { AppUpdateInstaller.promptInstall(context, s.file) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.update_install_button))
                    }
                    if (!AppUpdateInstaller.canInstallPackages(context)) {
                        Text(
                            stringResource(R.string.update_allow_install_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilledTonalButton(
                            onClick = { AppUpdateInstaller.openInstallPermissionSettings(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.update_allow_install_button))
                        }
                    }
                }
                is AppUpdateState.Error -> {
                    val message =
                        when (s.kind) {
                            AppUpdateError.CHECK_FAILED -> stringResource(R.string.update_error)
                            AppUpdateError.DOWNLOAD_FAILED -> stringResource(R.string.update_download_error)
                        }
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(
                        onClick = { app.appUpdateController.checkForUpdate() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.update_check_button))
                    }
                }
            }
        }
    }
}
