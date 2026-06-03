package com.swiftsave.app.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.swiftsave.app.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Copies downloads from app-private storage into locations indexed by the system MediaStore so
 * Gallery, Photos, and Files → Movies (or Music) can show them.
 */
object SavedMediaPublisher {

    private const val TAG = "SavedMediaPublisher"

    fun publishDownload(context: Context, source: File, title: String, jobId: String): String? {
        if (!source.isFile || source.length() <= 0L) return null
        val base = sanitizeTitle(title).ifBlank { "SwiftSave" }
        val ext = source.extension.lowercase().let { if (it.isNotEmpty()) ".$it" else "" }
        val displayName = "${base}_${jobId.takeLast(8)}$ext"
        val mime = mimeForExtension(source.extension)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertViaMediaStore(context.applicationContext, source, displayName, mime)
        } else {
            copyToPublicMoviesLegacy(context.applicationContext, source, displayName, mime)
        }
    }

    /** Visible in system gallery and under public Movies/SwiftSave or Music/SwiftSave. */
    private fun insertViaMediaStore(
        context: Context,
        source: File,
        displayName: String,
        mime: String,
    ): String? {
        val resolver = context.contentResolver
        val isAudio = mime.startsWith("audio/")
        val relativePath =
            if (isAudio) {
                Environment.DIRECTORY_MUSIC + "/SwiftSave"
            } else {
                Environment.DIRECTORY_MOVIES + "/SwiftSave"
            }
        // EXTERNAL_CONTENT_URI is more widely picked up by gallery OEM code than volume Uris.
        val collection =
            if (isAudio) {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        val uri = resolver.insert(collection, values)
        if (uri == null) {
            if (BuildConfig.DEBUG) Log.e(TAG, "MediaStore insert failed")
            return null
        }
        try {
            val out =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    resolver.openOutputStream(uri, "w")
                } else {
                    resolver.openOutputStream(uri)
                }
            out?.use { output ->
                FileInputStream(source).use { it.copyTo(output) }
            } ?: run {
                resolver.delete(uri, null, null)
                if (BuildConfig.DEBUG) Log.e(TAG, "openOutputStream failed")
                return null
            }
            val publishedSize = source.length()
            val finalize =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                    put(MediaStore.MediaColumns.SIZE, publishedSize)
                }
            resolver.update(uri, finalize, null, null)
            resolver.notifyChange(uri, null)
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) Log.e(TAG, "MediaStore write failed", e)
            resolver.delete(uri, null, null)
            return null
        }
        source.delete()
        triggerGalleryScan(context, displayName, mime, isAudio)
        return uri.toString()
    }

    /**
     * Many OEM galleries still index via [MediaScannerConnection]. After MediaStore finishes, the
     * file is typically on disk under public Movies/SwiftSave or Music/SwiftSave.
     */
    private fun triggerGalleryScan(
        context: Context,
        displayName: String,
        mime: String,
        isAudio: Boolean,
    ) {
        val dir =
            if (isAudio) {
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    "SwiftSave",
                )
            } else {
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "SwiftSave",
                )
            }
        val scanFile = File(dir, displayName)
        if (scanFile.isFile && scanFile.length() > 0L) {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(scanFile.absolutePath),
                arrayOf(mime),
                null,
            )
        }
    }

    private fun copyToPublicMoviesLegacy(
        context: Context,
        source: File,
        displayName: String,
        mime: String,
    ): String? {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val isAudio = mime.startsWith("audio/")
        val publicRoot =
            if (isAudio) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            }
        val destDir = File(publicRoot, "SwiftSave").apply { mkdirs() }
        val stem = displayName.substringBeforeLast('.')
        val extPart = displayName.substringAfterLast('.', "")
        var dest = File(destDir, displayName)
        var n = 1
        while (dest.exists()) {
            dest =
                File(
                    destDir,
                    if (extPart.isNotEmpty()) "${stem}_$n.$extPart" else "${stem}_$n",
                )
            n++
        }
        try {
            FileInputStream(source).use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
        } catch (_: Throwable) {
            dest.delete()
            return null
        }
        source.delete()
        MediaScannerConnection.scanFile(
            context,
            arrayOf(dest.absolutePath),
            arrayOf(mime),
            null,
        )
        return dest.absolutePath
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
