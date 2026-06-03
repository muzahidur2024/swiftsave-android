package com.downme.app.util

import android.net.Uri
import java.net.IDN

object UrlUtils {

    private val urlRegex = Regex("https://[^\\s]+")

    private val youtubeHosts =
        setOf(
            "youtube.com",
            "www.youtube.com",
            "m.youtube.com",
            "music.youtube.com",
            "youtu.be",
            "www.youtu.be",
        )

    fun extractUrl(text: String?): String? {
        val raw = text?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val match = urlRegex.find(raw) ?: return null
        return match.value.trimEnd(',', '.', ')', ']', '>')
    }

    fun isSafeUrl(url: String): Boolean {
        val u = url.trim()
        if ('\n' in u || '\r' in u || ' ' in u) return false
        val parsed =
            try {
                Uri.parse(u)
            } catch (_: Throwable) {
                return false
            }
        if (parsed.scheme != "https") return false
        val host = parsed.host?.trim('.')?.lowercase()?.takeIf { it.isNotBlank() } ?: return false
        return runCatching { IDN.toASCII(host) }.isSuccess
    }

    fun isYoutubeUrl(url: String): Boolean {
        val host =
            try {
                Uri.parse(url).host?.trim('.')?.lowercase()
            } catch (_: Throwable) {
                null
            }
        return host != null && host in youtubeHosts
    }

    /**
     * Normalizes share/paste text to a single https URL. Expands youtu.be and Shorts links
     * to a standard watch URL that yt-dlp handles reliably.
     */
    fun normalizeDownloadUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val firstLine = trimmed.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        val candidate = extractUrl(trimmed) ?: extractUrl(firstLine) ?: firstLine
        val single = candidate.substringBefore('\n').substringBefore('\r').trim()
        if (!isSafeUrl(single)) return null
        return canonicalizeYoutubeUrl(single) ?: single
    }

    private fun canonicalizeYoutubeUrl(url: String): String? {
        val uri = Uri.parse(url)
        val host = uri.host?.trim('.')?.lowercase() ?: return null
        if (host !in youtubeHosts) return null

        if (host == "youtu.be" || host == "www.youtu.be") {
            val id = uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
            return "https://www.youtube.com/watch?v=$id"
        }

        val path = uri.path?.trim('/') ?: ""
        if (path.startsWith("shorts/")) {
            val id = path.removePrefix("shorts/").substringBefore('/')
            if (id.isNotBlank()) return "https://www.youtube.com/watch?v=$id"
        }
        if (path.startsWith("live/")) {
            val id = path.removePrefix("live/").substringBefore('/')
            if (id.isNotBlank()) return "https://www.youtube.com/watch?v=$id"
        }

        val videoId = uri.getQueryParameter("v")
        if (!videoId.isNullOrBlank()) {
            return "https://www.youtube.com/watch?v=$videoId"
        }

        return url
    }
}
