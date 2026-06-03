package com.downme.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

data class LibraryMedia(
    val uri: Uri,
    val mimeType: String,
    val exists: Boolean,
    val isAudio: Boolean,
)

object MediaLibraryActions {

    fun resolve(context: Context, filePath: String?): LibraryMedia? {
        val path = filePath?.trim().orEmpty()
        if (path.isBlank()) return null
        return if (path.startsWith("content://")) {
            resolveContentUri(context, Uri.parse(path))
        } else {
            resolveFilePath(context, path)
        }
    }

    fun playIntent(context: Context, media: LibraryMedia): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(media.uri, media.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun shareIntent(context: Context, media: LibraryMedia): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = media.mimeType
            putExtra(Intent.EXTRA_STREAM, media.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    suspend fun deleteFile(context: Context, filePath: String?): Boolean {
        val path = filePath?.trim().orEmpty()
        if (path.isBlank()) return false
        return if (path.startsWith("content://")) {
            try {
                context.contentResolver.delete(Uri.parse(path), null, null) > 0
            } catch (_: Throwable) {
                false
            }
        } else {
            val file = File(path)
            if (!isShareableDownloadFile(context, file)) return false
            file.delete()
        }
    }

    /** Only expose files the app created (staging or published DownMe / gallery paths). */
    private fun isShareableDownloadFile(context: Context, file: File): Boolean {
        val canonical =
            runCatching { file.canonicalFile }.getOrNull() ?: return false
        if (!canonical.isFile) return false
        val staging = runCatching { SavedMediaPublisher.stagingDir(context).canonicalFile }.getOrNull()
        if (staging != null && canonical.path.startsWith(staging.path)) return true
        val publicRoots =
            listOf(
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "DownMe"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "DownMe"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DownMe"),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            )
        return publicRoots.any { root ->
            val base = runCatching { root.canonicalFile }.getOrNull() ?: return@any false
            canonical.path == base.path || canonical.path.startsWith(base.path + File.separator)
        }
    }

    private fun resolveContentUri(context: Context, uri: Uri): LibraryMedia? {
        val mime = context.contentResolver.getType(uri) ?: guessMimeFromUri(uri)
        val exists =
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
            } catch (_: Throwable) {
                false
            }
        return LibraryMedia(uri, mime, exists, mime.startsWith("audio/"))
    }

    private fun resolveFilePath(context: Context, path: String): LibraryMedia? {
        val file = File(path)
        if (!file.isFile || !isShareableDownloadFile(context, file)) return null
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        val mime = guessMimeFromPath(path)
        return LibraryMedia(uri, mime, file.length() > 0, mime.startsWith("audio/"))
    }

    private fun guessMimeFromPath(path: String): String =
        when {
            path.endsWith(".mp3", true) -> "audio/mpeg"
            path.endsWith(".m4a", true) -> "audio/mp4"
            path.endsWith(".opus", true) -> "audio/opus"
            path.endsWith(".flac", true) -> "audio/flac"
            path.endsWith(".webm", true) -> "video/webm"
            path.endsWith(".mkv", true) -> "video/x-matroska"
            else -> "video/mp4"
        }

    private fun guessMimeFromUri(uri: Uri): String {
        val seg = uri.lastPathSegment.orEmpty()
        return guessMimeFromPath(seg)
    }
}
