package com.downme.app.util

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class YtDlpVideoProbe(
    val title: String?,
    val thumbnail: String?,
    val durationSec: Double?,
    val availableHeights: Set<Int>,
)

object YtDlpFormatProbe {

    suspend fun probeVideo(context: Context, url: String): YtDlpVideoProbe =
        withContext(Dispatchers.IO) {
            val fromDump = probeViaDumpJson(context, url)
            val needsTitle = !isUsableTitle(fromDump.title)
            val fromInfo =
                if (needsTitle) {
                    probeViaGetInfo(url)
                } else {
                    null
                }
            YtDlpVideoProbe(
                title = firstUsableTitle(fromDump.title, fromInfo?.title),
                thumbnail = fromDump.thumbnail ?: fromInfo?.thumbnail,
                durationSec = fromDump.durationSec ?: fromInfo?.durationSec,
                availableHeights = fromDump.availableHeights,
            )
        }

    fun isUsableTitle(title: String?): Boolean {
        val t = title?.trim().orEmpty()
        if (t.isBlank()) return false
        if (DOMAIN_LIKE_TITLE.matches(t)) return false
        return true
    }

    private fun firstUsableTitle(vararg candidates: String?): String? =
        candidates.firstOrNull { isUsableTitle(it) }?.trim()

    private suspend fun probeViaDumpJson(context: Context, url: String): YtDlpVideoProbe =
        runCatching {
            val request = YoutubeDLRequest(url)
            request.addOption("--dump-json")
            request.addOption("--skip-download")
            if (UrlUtils.isYoutubeUrl(url)) {
                YtDlpFormats.applyYoutubeOptions(request)
            }
            YtDlpFormats.applyPerformanceOptions(request, context, durationSec = null)
            val response =
                YoutubeDL.getInstance().execute(
                    request,
                    null,
                    redirectErrorStream = false,
                )
            val payload =
                extractJsonPayload(response.out)
                    ?: extractJsonPayload(response.err)
            if (payload == null) {
                return@runCatching YtDlpVideoProbe(null, null, null, emptySet())
            }
            parseProbe(payload)
        }.getOrDefault(YtDlpVideoProbe(null, null, null, emptySet()))

    private suspend fun probeViaGetInfo(url: String): YtDlpVideoProbe? =
        runCatching {
            val info = YoutubeDL.getInstance().getInfo(url) ?: return@runCatching null
            YtDlpVideoProbe(
                title = info.title?.takeIf { it.isNotBlank() },
                thumbnail = info.thumbnail?.takeIf { it.isNotBlank() },
                durationSec = (info.duration as? Number)?.toDouble()?.takeIf { it > 0.0 },
                availableHeights = emptySet(),
            )
        }.getOrNull()

    private fun extractJsonPayload(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        if (trimmed.startsWith("{")) return trimmed
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return trimmed.substring(start, end + 1)
    }

    private fun parseProbe(jsonText: String): YtDlpVideoProbe {
        val root = JSONObject(jsonText)
        val title = root.optString("title").takeIf { it.isNotBlank() }
        val thumbnail = root.optString("thumbnail").takeIf { it.isNotBlank() }
        val duration = root.optDouble("duration", 0.0).takeIf { it > 0.0 }
        return YtDlpVideoProbe(title, thumbnail, duration, parseHeights(root))
    }

    private fun parseHeights(root: JSONObject): Set<Int> {
        val formats = root.optJSONArray("formats") ?: return emptySet()
        val heights = mutableSetOf<Int>()
        for (i in 0 until formats.length()) {
            val format = formats.optJSONObject(i) ?: continue
            val height = format.optInt("height", 0)
            val vcodec = format.optString("vcodec", "none")
            if (height > 0 && vcodec != "none") {
                heights.add(height)
            }
        }
        return heights
    }

    private val DOMAIN_LIKE_TITLE =
        Regex("^[a-z0-9][a-z0-9.-]*\\.[a-z]{2,}$", RegexOption.IGNORE_CASE)
}
