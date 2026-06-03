package com.downme.app.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.downme.app.R
import com.downme.app.DownMeApplication
import com.downme.app.data.DownloadEntity
import com.downme.app.util.MediaLibraryActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    jobId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as DownMeApplication
    var item by remember { mutableStateOf<DownloadEntity?>(null) }

    LaunchedEffect(jobId) {
        item = app.database.downloadDao().getById(jobId)
    }

    val media = remember(item?.filePath) { MediaLibraryActions.resolve(context, item?.filePath) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        item?.title ?: stringResource(R.string.play),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                item == null -> CircularProgressIndicator()
                media == null || !media.exists ->
                    Text(
                        stringResource(R.string.library_file_missing),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                else -> InAppPlayer(uri = media.uri, isAudio = media.isAudio)
            }
        }
    }
}

@Composable
private fun InAppPlayer(
    uri: android.net.Uri,
    isAudio: Boolean,
) {
    val context = LocalContext.current
    val player =
        remember(uri) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
        }

    DisposableEffect(uri) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                this.player = player
                useController = true
                controllerShowTimeoutMs = 4000
                controllerHideOnTouch = true
                if (isAudio) {
                    // Audio-only: show controls without needing a video surface.
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            }
        },
        update = { view ->
            view.player = player
        },
    )
}
