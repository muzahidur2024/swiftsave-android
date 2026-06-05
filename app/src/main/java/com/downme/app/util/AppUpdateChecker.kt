package com.downme.app.util

import android.content.Context
import com.downme.app.BuildConfig
import com.downme.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateChecker {

    suspend fun checkForUpdate(context: Context): Result<AppReleaseInfo?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val manifestUrl = context.getString(R.string.update_manifest_url).trim()
                val apkBase = context.getString(R.string.update_apk_base_url).trim().trimEnd('/')
                if (manifestUrl.isEmpty() || apkBase.isEmpty()) {
                    error("Update manifest URL not configured")
                }
                val json = fetchText(manifestUrl)
                val root = JSONObject(json)
                val remoteCode =
                    if (root.has("versionCode")) {
                        root.getInt("versionCode")
                    } else {
                        parseVersionNameCode(root.optString("versionName", ""))
                    }
                val remoteName = root.optString("versionName", "").ifBlank { remoteCode.toString() }
                if (remoteCode <= BuildConfig.VERSION_CODE) {
                    return@runCatching null
                }
                val downloadPath = root.optString("downloadPath", "").trim()
                val apkFile = root.optString("apkFile", "").trim()
                val apkUrl =
                    when {
                        downloadPath.startsWith("http") -> downloadPath
                        downloadPath.isNotEmpty() -> "$apkBase$downloadPath"
                        apkFile.isNotEmpty() -> "$apkBase/downloads/$apkFile"
                        else -> error("Update manifest missing APK path")
                    }
                val fileName = apkFile.ifBlank { apkUrl.substringAfterLast('/') }
                val sizeMb =
                    if (root.has("sizeMb")) {
                        root.getDouble("sizeMb")
                    } else {
                        null
                    }
                AppReleaseInfo(
                    versionCode = remoteCode,
                    versionName = remoteName,
                    apkUrl = apkUrl,
                    apkFileName = fileName,
                    sizeMb = sizeMb,
                )
            }
        }

    private fun parseVersionNameCode(name: String): Int {
        val parts = name.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size >= 3) {
            return parts[0] * 10_000 + parts[1] * 100 + parts[2]
        }
        return 0
    }

    private fun fetchText(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val stream =
                if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                error("HTTP $code")
            }
            if (body.isBlank()) {
                error("Empty update manifest")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }
}
