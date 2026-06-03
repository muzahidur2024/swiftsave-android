package com.swiftsave.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DownloadEntity)

    @Query("UPDATE downloads SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, error: String?)

    @Query(
        "UPDATE downloads SET status = :newStatus, progress = 0, errorMessage = :error " +
            "WHERE status = :oldStatus",
    )
    suspend fun updateAllWithStatus(oldStatus: String, newStatus: String, error: String?)

    @Query(
        "UPDATE downloads SET status = :status, filePath = :path, fileSize = :size, progress = 100, errorMessage = null WHERE id = :id",
    )
    suspend fun markComplete(id: String, status: String, path: String, size: Long)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)
}
