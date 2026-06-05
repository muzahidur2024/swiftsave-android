package com.downme.app.util

import android.app.Application
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppUpdateError {
    CHECK_FAILED,
    DOWNLOAD_FAILED,
}

sealed class AppUpdateState {
    data object Idle : AppUpdateState()

    data object Checking : AppUpdateState()

    data object UpToDate : AppUpdateState()

    data class Available(val info: AppReleaseInfo) : AppUpdateState()

    data class Downloading(val progress: Int, val info: AppReleaseInfo) : AppUpdateState()

    data class ReadyToInstall(val file: File, val info: AppReleaseInfo) : AppUpdateState()

    data class Error(val kind: AppUpdateError) : AppUpdateState()
}

/**
 * Runs update check/download on the application scope so work continues when the user
 * leaves Settings or switches tabs.
 */
class AppUpdateController(
    private val app: Application,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    private var activeJob: Job? = null

    fun checkForUpdate() {
        activeJob?.cancel()
        activeJob =
            scope.launch {
                _state.value = AppUpdateState.Checking
                AppUpdateChecker.checkForUpdate(app)
                    .onSuccess { update ->
                        _state.value =
                            when (update) {
                                null -> AppUpdateState.UpToDate
                                else -> AppUpdateState.Available(update)
                            }
                    }
                    .onFailure { e ->
                        if (e is CancellationException) return@launch
                        _state.value = AppUpdateState.Error(AppUpdateError.CHECK_FAILED)
                    }
            }
    }

    fun downloadUpdate(info: AppReleaseInfo) {
        activeJob?.cancel()
        activeJob =
            scope.launch {
                _state.value = AppUpdateState.Downloading(0, info)
                AppUpdateInstaller.downloadApk(app, info) { progress ->
                    _state.value = AppUpdateState.Downloading(progress, info)
                }.onSuccess { file ->
                    _state.value = AppUpdateState.ReadyToInstall(file, info)
                }.onFailure { e ->
                    if (e is CancellationException) return@launch
                    _state.value = AppUpdateState.Error(AppUpdateError.DOWNLOAD_FAILED)
                }
            }
    }
}
