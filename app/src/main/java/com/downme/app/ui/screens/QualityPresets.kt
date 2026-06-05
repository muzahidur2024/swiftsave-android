package com.downme.app.ui.screens

internal data class QualityPreset(val key: String, val label: String)

internal val QUALITY_PRESETS: List<QualityPreset> =
    listOf(
        QualityPreset("1080", "1080p"),
        QualityPreset("720", "720p"),
        QualityPreset("480", "480p"),
        QualityPreset("360", "360p"),
        QualityPreset("mp3", "MP3"),
    )

internal val VIDEO_QUALITY_PRESETS: List<QualityPreset> =
    QUALITY_PRESETS.filter { it.key != "mp3" }

internal val MUSIC_QUALITY_PRESETS: List<QualityPreset> =
    QUALITY_PRESETS.filter { it.key == "mp3" }
