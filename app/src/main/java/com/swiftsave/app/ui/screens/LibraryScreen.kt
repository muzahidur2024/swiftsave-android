package com.swiftsave.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.swiftsave.app.R
import com.swiftsave.app.SwiftSaveApplication
import com.swiftsave.app.data.DownloadEntity
import com.swiftsave.app.data.DownloadStatus
import com.swiftsave.app.download.DownloadForegroundService
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SwiftSaveApplication
    val dao = app.database.downloadDao()
    val items by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val downloading = items.filter { it.status == DownloadStatus.DOWNLOADING }
    val done = items.filter { it.status == DownloadStatus.DONE }
    val failed = items.filter { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }

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
                        onShare = null,
                        onRetry = null,
                        onDelete = {
                            scope.launch {
                                DownloadForegroundService.cancelDownload(context, row.id)
                                dao.deleteById(row.id)
                            }
                        },
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
                        onShare = {
                            val path = row.filePath ?: return@DownloadRow
                            val (uri, type) =
                                if (path.startsWith("content://")) {
                                    val u = Uri.parse(path)
                                    val t = context.contentResolver.getType(u) ?: "video/*"
                                    u to t
                                } else {
                                    val f = File(path)
                                    if (!f.exists()) return@DownloadRow
                                    val u =
                                        FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            f,
                                        )
                                    val t =
                                        context.contentResolver.getType(u)
                                            ?: if (path.endsWith(".mp3", true)) "audio/mpeg" else "video/*"
                                    u to t
                                }
                            val send = Intent(Intent.ACTION_SEND).apply {
                                this.type = type
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(send, context.getString(R.string.share)),
                            )
                        },
                        onDelete = {
                            scope.launch {
                                val path = row.filePath
                                if (!path.isNullOrBlank()) {
                                    if (path.startsWith("content://")) {
                                        try {
                                            context.contentResolver.delete(
                                                Uri.parse(path),
                                                null,
                                                null,
                                            )
                                        } catch (_: Throwable) {
                                        }
                                    } else {
                                        File(path).delete()
                                    }
                                }
                                dao.deleteById(row.id)
                            }
                        },
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
                        onShare = null,
                        onRetry = if (row.status == DownloadStatus.FAILED) {
                            { app.retryFailedDownload(row) }
                        } else {
                            null
                        },
                        onDelete = {
                            scope.launch { dao.deleteById(row.id) }
                        },
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
    onShare: (() -> Unit)?,
    onRetry: (() -> Unit)? = null,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!item.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(72.dp),
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
                            DownloadStatus.FAILED -> add(item.errorMessage ?: stringResource(R.string.library_error_generic))
                            DownloadStatus.CANCELLED -> add(item.errorMessage ?: cancelledText)
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
                showShare = onShare != null && item.status == DownloadStatus.DONE,
                showRetry = onRetry != null && item.status == DownloadStatus.FAILED,
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
    }
}

@Composable
private fun BoxWithMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpenMenu: () -> Unit,
    showShare: Boolean,
    showRetry: Boolean,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    Box {
        IconButton(onClick = onOpenMenu) {
            Icon(Icons.Default.MoreVert, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            if (showShare) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share)) },
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
