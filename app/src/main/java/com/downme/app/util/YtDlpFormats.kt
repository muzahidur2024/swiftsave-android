package com.downme.app.util

import android.app.ActivityManager
import android.content.Context
import com.yausername.youtubedl_android.YoutubeDLRequest

object YtDlpFormats {

    private const val LOW_RAM_THRESHOLD_BYTES = 4L * 1024 * 1024 * 1024
    private const val LONG_VIDEO_SEC = 25 * 60

    private val validKeys = setOf("1080", "720", "480", "360", "mp3")
    private val videoQualityOrder = listOf("1080", "720", "480", "360")

    fun normalizeQuality(qualityRaw: String?): String {
        val q = qualityRaw?.lowercase()?.trim().orEmpty()
        return when {
            q in validKeys -> q
            q == "1440" || q == "2160" || q == "best" -> "1080"
            else -> "1080"
        }
    }

    fun qualityLabel(quality: String): String =
        when (quality) {
            "mp3" -> "MP3"
            else -> "${quality}p"
        }

    /** Qualities to try, highest first, ending at 360p for video. */
    fun fallbackChain(requested: String): List<String> {
        val q = normalizeQuality(requested)
        if (q == "mp3") return listOf("mp3")
        val startIdx = videoQualityOrder.indexOf(q).coerceAtLeast(0)
        return videoQualityOrder.drop(startIdx)
    }

    fun planQualityAttempts(requested: String, availableHeights: Set<Int>): List<String> {
        val q = normalizeQuality(requested)
        if (q == "mp3") return listOf("mp3")
        val chain = fallbackChain(requested)
        if (availableHeights.isEmpty()) {
            // Probe failed: try requested quality only (avoid 1080→720→480 triple downloads).
            return listOf(q)
        }
        val maxHeight = availableHeights.maxOrNull() ?: 0
        val viable = chain.filter { satisfiesQuality(it, maxHeight) }
        return viable.ifEmpty { listOf("360") }
    }

    fun satisfiesQuality(quality: String, maxAvailableHeight: Int): Boolean =
        when (normalizeQuality(quality)) {
            "1080" -> maxAvailableHeight >= 900
            "720" -> maxAvailableHeight >= 600
            "480" -> maxAvailableHeight >= 360
            "360" -> maxAvailableHeight >= 240
            "mp3" -> true
            else -> maxAvailableHeight >= 240
        }

    fun isLowRamDevice(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem in 1..LOW_RAM_THRESHOLD_BYTES
    }

    fun isLongVideo(durationSec: Double?): Boolean =
        (durationSec ?: 0.0) >= LONG_VIDEO_SEC.toDouble()

    fun applyPerformanceOptions(
        request: YoutubeDLRequest,
        context: Context,
        durationSec: Double? = null,
    ) {
        request.addOption("--no-playlist")
        request.addOption("--no-warnings")
        request.addOption("--no-abort-on-error")
        val longVideo = isLongVideo(durationSec)
        val fragments =
            when {
                isLowRamDevice(context) && longVideo -> "1"
                longVideo -> "2"
                else -> "6"
            }
        request.addOption("--concurrent-fragments", fragments)
        request.addOption("--retries", if (longVideo) "10" else "5")
        request.addOption("--fragment-retries", if (longVideo) "15" else "5")
        request.addOption("--socket-timeout", if (longVideo) "120" else "30")
        if (longVideo) {
            request.addOption("--continue")
        }
    }

    /**
     * Prefer clients that still expose adaptive formats without cookies.
     * Avoid web/mweb-only paths that often return SABR-only streams in 2026.
     */
    fun applyYoutubeOptions(request: YoutubeDLRequest) {
        request.addOption("--extractor-args", "youtube:player_client=android_vr,web_safari,android")
        request.addOption("--extractor-retries", "2")
        request.addOption("--geo-bypass")
    }

    fun applyForUrl(
        context: Context,
        request: YoutubeDLRequest,
        url: String,
        quality: String,
        durationSec: Double? = null,
    ) {
        if (UrlUtils.isYoutubeUrl(url)) {
            applyYoutubeOptions(request)
        }
        applyPerformanceOptions(request, context, durationSec)
        applyQuality(request, normalizeQuality(quality))
    }

    fun applyQuality(request: YoutubeDLRequest, quality: String) {
        when (quality) {
            "best" -> {
                request.addOption("-f", "bv*+ba/bv+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
            "mp3" -> {
                request.addOption("-f", "ba/b")
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "0")
            }
            "1080", "720", "480", "360" -> {
                val height = quality
                request.addOption(
                    "-f",
                    "bv*[height<=$height]+ba/bv*[height<=$height]+ba/b[height<=$height]/bv*+ba/b",
                )
                request.addOption("--merge-output-format", "mp4")
            }
            else -> {
                request.addOption("-f", "bv*+ba/bv+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
        }
    }

    fun shouldRetryWithLowerQuality(
        stderr: String,
        cause: Throwable?,
        attemptedQuality: String,
    ): Boolean {
        if (normalizeQuality(attemptedQuality) == "360") return false
        if (normalizeQuality(attemptedQuality) == "mp3") return false
        return isFormatUnavailableError(stderr) ||
            isMergeOrFinalizeError(stderr, cause) ||
            cause is IllegalStateException
    }

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
            lower.contains("format is not available") ||
            lower.contains("only images are available")
    }

    fun isMergeOrFinalizeError(stderr: String, cause: Throwable? = null): Boolean {
        val text = buildString {
            append(stderr.lowercase())
            cause?.message?.let { append(' ').append(it.lowercase()) }
        }
        return text.contains("ffmpeg") ||
            text.contains("merging") ||
            text.contains("merge") ||
            text.contains("postprocess") ||
            text.contains("unable to download") ||
            text.contains("no space") ||
            text.contains("enospc") ||
            text.contains("cannot allocate") ||
            text.contains("out of memory")
    }
}
