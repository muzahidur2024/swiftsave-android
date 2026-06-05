package com.downme.app.data

import com.downme.app.R

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
