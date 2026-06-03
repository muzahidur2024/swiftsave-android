package com.swiftsave.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceUrl: String,
    val filePath: String?,
    val fileSize: Long?,
    val durationSec: Double?,
    val thumbnailUrl: String?,
    val createdAt: Long,
    val status: String,
    val progress: Int,
    val errorMessage: String?,
)

object DownloadStatus {
    const val QUEUED = "QUEUED"
    const val DOWNLOADING = "DOWNLOADING"
    const val DONE = "DONE"
    const val CANCELLED = "CANCELLED"
    const val FAILED = "FAILED"
}
