package com.downme.app.util

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

object ShareIntentParser {

    fun urlFromSendIntent(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val mime = intent.type
        if (mime != null && mime != "text/plain") return null
        // Only accept shared link text, not file/stream attachments.
        val stream =
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        if (stream != null) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (text.isEmpty()) return null
        return UrlUtils.normalizeDownloadUrl(text)
    }
}
