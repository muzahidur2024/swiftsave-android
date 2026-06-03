package com.downme.app.data

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
    /** Quality preset key (best, 1080, 720, mp3, …) used for this job. */
    val quality: String? = null,
)

object DownloadStatus {
    const val QUEUED = "QUEUED"
    const val DOWNLOADING = "DOWNLOADING"
    const val DONE = "DONE"
    const val CANCELLED = "CANCELLED"
    const val FAILED = "FAILED"
}
