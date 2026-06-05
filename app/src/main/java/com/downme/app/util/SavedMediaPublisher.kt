package com.downme.app.util

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Stages downloads in app-private storage, then copies finished files into the user's chosen folder.
 */
object SavedMediaPublisher {

    private const val APP_STAGING_FOLDER = "DownMe"

    fun publishDownload(
        context: Context,
        source: File,
        title: String,
        jobId: String,
        customFolderUri: String?,
    ): String? {
        if (!source.isFile || source.length() <= 0L) return null
        val base = sanitizeTitle(title).ifBlank { "DownMe" }
        val ext = source.extension.lowercase().let { if (it.isNotEmpty()) ".$it" else "" }
        val displayName = "${base}_${jobId.takeLast(8)}$ext"
        val mime = mimeForExtension(source.extension)

        if (!customFolderUri.isNullOrBlank() && CustomDownloadFolder.isAccessible(context, customFolderUri)) {
            return CustomDownloadFolder.publish(context, source, displayName, mime, customFolderUri)
        }
        return source.absolutePath
    }

    fun stagingDir(context: Context): File {
        val parent =
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: context.filesDir
        return File(parent, APP_STAGING_FOLDER).apply { mkdirs() }
    }

    private fun sanitizeTitle(title: String): String =
        title
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(80)

    private fun mimeForExtension(ext: String): String =
        when (ext.lowercase()) {
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "opus" -> "audio/opus"
            "flac" -> "audio/flac"
            else -> "video/mp4"
        }
}
