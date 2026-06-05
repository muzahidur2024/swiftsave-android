package com.downme.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream

object CustomDownloadFolder {

    fun displayLabel(context: Context, treeUri: String?): String? {
        if (treeUri.isNullOrBlank()) return null
        val tree = DocumentFile.fromTreeUri(context.applicationContext, Uri.parse(treeUri)) ?: return null
        return tree.name?.takeIf { it.isNotBlank() }
    }

    fun isAccessible(context: Context, treeUri: String?): Boolean {
        if (treeUri.isNullOrBlank()) return false
        val tree = DocumentFile.fromTreeUri(context.applicationContext, Uri.parse(treeUri)) ?: return false
        return tree.canWrite()
    }

    fun persistTreePermission(context: Context, treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.applicationContext.contentResolver.takePersistableUriPermission(treeUri, flags)
    }

    fun publish(
        context: Context,
        source: File,
        displayName: String,
        mime: String,
        treeUri: String,
    ): String? {
        if (!source.isFile || source.length() <= 0L) return null
        val appContext = context.applicationContext
        val tree = DocumentFile.fromTreeUri(appContext, Uri.parse(treeUri)) ?: return null
        if (!tree.canWrite()) return null

        val stem = displayName.substringBeforeLast('.')
        val extPart = displayName.substringAfterLast('.', "")
        var fileName = displayName
        var suffix = 1
        while (tree.findFile(fileName) != null) {
            fileName =
                if (extPart.isNotEmpty()) {
                    "${stem}_$suffix.$extPart"
                } else {
                    "${stem}_$suffix"
                }
            suffix++
        }

        val dest = tree.createFile(mime, fileName) ?: return null
        return try {
            appContext.contentResolver.openOutputStream(dest.uri)?.use { output ->
                FileInputStream(source).use { input -> input.copyTo(output) }
            } ?: run {
                dest.delete()
                return null
            }
            source.delete()
            dest.uri.toString()
        } catch (_: Throwable) {
            dest.delete()
            null
        }
    }
}
