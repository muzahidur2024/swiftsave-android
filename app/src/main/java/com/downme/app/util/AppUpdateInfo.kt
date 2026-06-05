package com.downme.app.util

data class AppReleaseInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val apkFileName: String,
    val sizeMb: Double?,
)
