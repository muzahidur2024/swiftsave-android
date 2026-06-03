package com.swiftsave.app.util

import android.net.Uri
import java.net.IDN

object UrlUtils {

    private val urlRegex = Regex("https://[^\\s]+")

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
        val host = parsed.host?.trim('.')?.takeIf { it.isNotBlank() } ?: return false
        return runCatching { IDN.toASCII(host) }.isSuccess
    }

    /**
     * Single-line https URL suitable for yt-dlp (fixes retry when stored text has extra whitespace
     * or one line of share text plus URL).
     */
    fun normalizeDownloadUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val firstLine = trimmed.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        val candidate = extractUrl(trimmed) ?: extractUrl(firstLine) ?: firstLine
        val single = candidate.substringBefore('\n').substringBefore('\r').trim()
        return single.takeIf { isSafeUrl(it) }
    }
}
