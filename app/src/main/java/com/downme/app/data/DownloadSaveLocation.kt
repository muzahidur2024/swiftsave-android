package com.downme.app.data

import com.downme.app.R

enum class DownloadSaveLocation(
    val id: String,
    val labelRes: Int,
    val pathHintRes: Int,
    val publishToGallery: Boolean,
    private val videoRelativePath: String,
    private val audioRelativePath: String,
    private val legacyVideoSubfolder: String,
    private val legacyAudioSubfolder: String,
) {
    MoviesDownMe(
        id = "movies_downme",
        labelRes = R.string.download_path_movies_downme,
        pathHintRes = R.string.download_path_hint_movies_downme,
        publishToGallery = true,
        videoRelativePath = "Movies/DownMe",
        audioRelativePath = "Music/DownMe",
        legacyVideoSubfolder = "DownMe",
        legacyAudioSubfolder = "DownMe",
    ),
    Movies(
        id = "movies",
        labelRes = R.string.download_path_movies,
        pathHintRes = R.string.download_path_hint_movies,
        publishToGallery = true,
        videoRelativePath = "Movies",
        audioRelativePath = "Music",
        legacyVideoSubfolder = "",
        legacyAudioSubfolder = "",
    ),
    Downloads(
        id = "downloads",
        labelRes = R.string.download_path_downloads,
        pathHintRes = R.string.download_path_hint_downloads,
        publishToGallery = true,
        videoRelativePath = "Download/DownMe",
        audioRelativePath = "Download/DownMe",
        legacyVideoSubfolder = "DownMe",
        legacyAudioSubfolder = "DownMe",
    ),
    AppOnly(
        id = "app_only",
        labelRes = R.string.download_path_app_only,
        pathHintRes = R.string.download_path_hint_app_only,
        publishToGallery = false,
        videoRelativePath = "",
        audioRelativePath = "",
        legacyVideoSubfolder = "DownMe",
        legacyAudioSubfolder = "DownMe",
    ),
    ;

    fun videoRelativePath(isAudio: Boolean): String =
        if (isAudio) audioRelativePath else videoRelativePath

    fun legacySubfolder(isAudio: Boolean): String =
        if (isAudio) legacyAudioSubfolder else legacyVideoSubfolder

    companion object {
        val DEFAULT = MoviesDownMe

        fun fromId(id: String?): DownloadSaveLocation =
            entries.firstOrNull { it.id == id } ?: DEFAULT

        val selectable = entries.toList()
    }
}

enum class AppThemeMode(val id: String, val labelRes: Int) {
    Dark("dark", R.string.theme_dark),
    Light("light", R.string.theme_light),
    Yellow("yellow", R.string.theme_yellow),
    ;

    companion object {
        fun fromId(id: String?): AppThemeMode =
            entries.firstOrNull { it.id == id } ?: Dark
    }
}
