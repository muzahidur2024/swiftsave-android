package com.swiftsave.app.util

import com.yausername.youtubedl_android.YoutubeDLRequest

object YtDlpFormats {

    /** yt-dlp options that speed up downloads without changing output quality presets. */
    fun applyPerformanceOptions(request: YoutubeDLRequest) {
        request.addOption("--no-playlist")
        request.addOption("--no-warnings")
        request.addOption("--concurrent-fragments", "4")
        request.addOption("--retries", "3")
        request.addOption("--fragment-retries", "3")
    }

    fun applyQuality(request: YoutubeDLRequest, qualityRaw: String) {
        val q = qualityRaw.lowercase().trim()
        when (q) {
            "best" -> {
                request.addOption("-f", "bv*+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
            "mp3" -> {
                request.addOption("-f", "ba/b")
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "0")
            }
            "2160", "1440", "1080", "720", "480" -> {
                request.addOption("-f", "bv*[height<=$q]+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
            else -> {
                request.addOption("-f", "bv*+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
        }
    }
}
