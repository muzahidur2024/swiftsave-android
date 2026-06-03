package com.downme.app.util

import com.yausername.youtubedl_android.YoutubeDLRequest

object YtDlpFormats {

    private val validKeys = setOf("best", "2160", "1440", "1080", "720", "480", "mp3")

    fun normalizeQuality(qualityRaw: String?): String {
        val q = qualityRaw?.lowercase()?.trim().orEmpty()
        return q.takeIf { it in validKeys } ?: "best"
    }

    /** Apply network/retry options shared by all hosts. */
    fun applyPerformanceOptions(request: YoutubeDLRequest) {
        request.addOption("--no-playlist")
        request.addOption("--no-warnings")
        request.addOption("--no-abort-on-error")
        request.addOption("--concurrent-fragments", "4")
        request.addOption("--retries", "5")
        request.addOption("--fragment-retries", "5")
        request.addOption("--socket-timeout", "30")
    }

    /** Extra reliability for YouTube (bot checks, player changes, SABR). */
    fun applyYoutubeOptions(request: YoutubeDLRequest) {
        request.addOption("--extractor-args", "youtube:player_client=android,web")
        request.addOption("--extractor-retries", "3")
        request.addOption("--geo-bypass")
    }

    fun applyForUrl(request: YoutubeDLRequest, url: String, qualityRaw: String) {
        if (UrlUtils.isYoutubeUrl(url)) {
            applyYoutubeOptions(request)
        }
        applyPerformanceOptions(request)
        applyQuality(request, normalizeQuality(qualityRaw))
    }

    fun applyQuality(request: YoutubeDLRequest, quality: String) {
        when (quality) {
            "best" -> {
                request.addOption(
                    "-f",
                    "bestvideo[ext=mp4]+bestaudio[ext=m4a]/bestvideo+bestaudio/best",
                )
                request.addOption("--merge-output-format", "mp4")
                request.addOption("--remux-video", "mp4")
            }
            "mp3" -> {
                request.addOption("-f", "ba/b")
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "0")
            }
            "2160", "1440", "1080", "720", "480" -> {
                val format =
                    "bestvideo[height<=$quality][ext=mp4]+bestaudio[ext=m4a]/" +
                        "bestvideo[height<=$quality]+bestaudio/" +
                        "b[height<=$quality]/" +
                        "bestvideo+bestaudio/best"
                request.addOption("-f", format)
                request.addOption("--merge-output-format", "mp4")
                request.addOption("--remux-video", "mp4")
            }
            else -> {
                request.addOption("-f", "bestvideo+bestaudio/best")
                request.addOption("--merge-output-format", "mp4")
            }
        }
    }

    /** Pull a human-friendly failure reason from yt-dlp stderr (stored in debug builds only). */
    fun summarizeError(stderr: String): String? {
        val line =
            stderr.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() }
        return line?.take(200)
    }

    fun isFormatUnavailableError(stderr: String): Boolean {
        val lower = stderr.lowercase()
        return lower.contains("requested format is not available") ||
            lower.contains("no video formats") ||
            lower.contains("format is not available")
    }
}
