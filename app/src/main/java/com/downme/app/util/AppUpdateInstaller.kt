package com.downme.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateInstaller {

    fun canInstallPackages(context: Context): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            true
        } else {
            context.packageManager.canRequestPackageInstalls()
        }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent =
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }

    suspend fun downloadApk(
        context: Context,
        info: AppReleaseInfo,
        onProgress: (Int) -> Unit,
    ): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val dest = File(dir, info.apkFileName)
                if (dest.exists()) dest.delete()

                val connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 120_000
                    requestMethod = "GET"
                }
                try {
                    val code = connection.responseCode
                    if (code !in 200..299) {
                        error("HTTP $code")
                    }
                    val total = connection.contentLengthLong.coerceAtLeast(0L)
                    connection.inputStream.use { input ->
                        dest.outputStream().buffered(256 * 1024).use { output ->
                            val buffer = ByteArray(256 * 1024)
                            var downloaded = 0L
                            var lastReported = -1
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (total > 0L) {
                                    val p = ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                                    if (p != lastReported) {
                                        lastReported = p
                                        onProgress(p)
                                    }
                                }
                            }
                        }
                    }
                    onProgress(100)
                    if (!dest.isFile || dest.length() <= 0L) {
                        dest.delete()
                        error("Downloaded file is empty")
                    }
                    dest
                } finally {
                    connection.disconnect()
                }
            }
        }

    fun promptInstall(context: Context, apkFile: File) {
        if (!apkFile.isFile || apkFile.length() <= 0L) return
        if (!canInstallPackages(context)) {
            openInstallPermissionSettings(context)
            return
        }
        val appContext = context.applicationContext
        val uri =
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                apkFile,
            )
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }
}
