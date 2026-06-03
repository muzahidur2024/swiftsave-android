package com.downme.app.ui.screens

internal data class QualityPreset(val key: String, val label: String)

internal val QUALITY_PRESETS: List<QualityPreset> =
    listOf(
        QualityPreset("best", "Best"),
        QualityPreset("2160", "4K"),
        QualityPreset("1440", "1440p"),
        QualityPreset("1080", "1080p"),
        QualityPreset("720", "720p"),
        QualityPreset("480", "480p"),
        QualityPreset("mp3", "MP3"),
    )
