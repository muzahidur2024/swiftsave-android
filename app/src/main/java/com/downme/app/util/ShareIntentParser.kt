package com.downme.app.util

import android.content.Intent

object ShareIntentParser {

    fun urlFromSendIntent(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        return UrlUtils.normalizeDownloadUrl(text)
    }
}
