package com.downme.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.downme.app.R
import com.downme.app.DownMeApplication
import com.downme.app.data.DownloadEntity
import com.downme.app.data.DownloadStatus
import com.downme.app.download.DownloadForegroundService
import com.downme.app.util.MediaLibraryActions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onPlay: (String) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as DownMeApplication
    val dao = app.database.downloadDao()
    val items by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val downloading = items.filter { it.status == DownloadStatus.DOWNLOADING }
    val done = items.filter { it.status == DownloadStatus.DONE }
    val failed = items.filter { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }

    fun playItem(row: DownloadEntity) {
        val media = MediaLibraryActions.resolve(context, row.filePath)
        if (media == null || !media.exists) {
            Toast.makeText(context, context.getString(R.string.library_action_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        onPlay(row.id)
    }

    fun shareItem(row: DownloadEntity) {
        val media = MediaLibraryActions.resolve(context, row.filePath)
        if (media == null || !media.exists) {
            Toast.makeText(context, context.getString(R.string.library_action_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        val send = MediaLibraryActions.shareIntent(context, media)
        context.startActivity(android.content.Intent.createChooser(send, context.getString(R.string.share)))
    }

    fun deleteItem(row: DownloadEntity) {
        scope.launch {
            if (row.status == DownloadStatus.DOWNLOADING) {
                DownloadForegroundService.cancelDownload(context, row.id)
            }
            MediaLibraryActions.deleteFile(context, row.filePath)
            dao.deleteById(row.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_library)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (downloading.isNotEmpty()) {
                item {
                    SectionTitle(stringResource(R.string.library_section_downloading, downloading.size))
                }
                items(downloading, key = { it.id }) { row ->
                    DownloadRow(
                        item = row,
                        onPlay = null,
                        onShare = null,
                        onRetry = null,
                        onDelete = { deleteItem(row) },
                    )
                }
            }
            if (done.isNotEmpty()) {
                item {
                    SectionTitle(stringResource(R.string.library_section_downloaded, done.size))
                }
                items(done, key = { it.id }) { row ->
                    DownloadRow(
                        item = row,
                        onPlay = { playItem(row) },
                        onShare = { shareItem(row) },
                        onDelete = { deleteItem(row) },
                        onRowClick = { playItem(row) },
                    )
                }
            }
            if (failed.isNotEmpty()) {
                item {
                    SectionTitle(stringResource(R.string.library_section_failed, failed.size))
                }
                items(failed, key = { it.id }) { row ->
                    DownloadRow(
                        item = row,
                        onPlay = null,
                        onShare = null,
                        onRetry =
                            if (row.status == DownloadStatus.FAILED) {
                                { app.retryFailedDownload(row) }
                            } else {
                                null
                            },
                        onDelete = { deleteItem(row) },
                    )
                }
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.library_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun DownloadRow(
    item: DownloadEntity,
    onPlay: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onRetry: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onRowClick: (() -> Unit)? = null,
) {
    var menu by remember { mutableStateOf(false) }
    val canPlay = onPlay != null && item.status == DownloadStatus.DONE
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (canPlay && onRowClick != null) {
                        Modifier.clickable(onClick = onRowClick)
                    } else {
                        Modifier
                    },
                ),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!item.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    if (item.status == DownloadStatus.DOWNLOADING) {
                        Text(
                            stringResource(R.string.library_progress_percent, item.progress),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        LinearProgressIndicator(
                            progress = { item.progress.coerceIn(0, 100) / 100f },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                        )
                    }
                    val cancelledText = stringResource(R.string.download_cancelled)
                    val meta =
                        buildList {
                            when (item.status) {
                                DownloadStatus.DOWNLOADING -> {}
                                DownloadStatus.DONE ->
                                    item.fileSize?.let {
                                        add(stringResource(R.string.library_size_mb, it / (1024.0 * 1024.0)))
                                    }
                                DownloadStatus.FAILED ->
                                    add(item.errorMessage ?: stringResource(R.string.library_error_generic))
                                DownloadStatus.CANCELLED ->
                                    add(item.errorMessage ?: cancelledText)
                                else -> {}
                            }
                            item.durationSec?.let { add(formatDuration(it)) }
                        }.joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(
                            meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        )
                    }
                    Text(
                        item.sourceUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        maxLines = 1,
                    )
                }
                if (item.status == DownloadStatus.FAILED && onRetry != null) {
                    FilledTonalButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.retry))
                    }
                }
                BoxWithMenu(
                    expanded = menu,
                    onDismiss = { menu = false },
                    onOpenMenu = { menu = true },
                    showPlay = canPlay,
                    showShare = onShare != null && item.status == DownloadStatus.DONE,
                    showRetry = onRetry != null && item.status == DownloadStatus.FAILED,
                    onPlay = {
                        menu = false
                        onPlay?.invoke()
                    },
                    onShare = {
                        menu = false
                        onShare?.invoke()
                    },
                    onRetry = {
                        menu = false
                        onRetry?.invoke()
                    },
                    onDelete = {
                        menu = false
                        onDelete()
                    },
                )
            }
            if (canPlay) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = { onPlay?.invoke() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.play))
                    }
                    if (onShare != null) {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxWithMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpenMenu: () -> Unit,
    showPlay: Boolean,
    showShare: Boolean,
    showRetry: Boolean,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    Box {
        IconButton(onClick = onOpenMenu) {
            Icon(Icons.Default.MoreVert, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            if (showPlay) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.play)) },
                    leadingIcon = {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    },
                    onClick = onPlay,
                )
            }
            if (showShare) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share)) },
                    leadingIcon = {
                        Icon(Icons.Default.Share, contentDescription = null)
                    },
                    onClick = onShare,
                )
            }
            if (showRetry) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.retry)) },
                    leadingIcon = {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    },
                    onClick = onRetry,
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null)
                },
                onClick = onDelete,
            )
        }
    }
}

private fun formatDuration(sec: Double): String {
    val s = sec.toLong()
    val m = s / 60
    val r = s % 60
    if (m >= 60) {
        val h = m / 60
        val mm = m % 60
        return String.format("%d:%02d:%02d", h, mm, r)
    }
    return String.format("%d:%02d", m, r)
}
