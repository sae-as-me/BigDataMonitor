package com.bigdatamonitor.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bigdatamonitor.data.db.entity.ClipboardAccessEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardAccessEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ClipboardAccessEventEntity): Long

    @Query("SELECT * FROM clipboard_access_events WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getByTimeRange(start: Long, end: Long): List<ClipboardAccessEventEntity>

    @Query("SELECT COUNT(*) FROM clipboard_access_events WHERE packageName = :pkg AND timestamp >= :since")
    suspend fun countByPackageSince(pkg: String, since: Long): Int

    @Query("SELECT COUNT(*) FROM clipboard_access_events WHERE timestamp >= :since")
    fun countSince(since: Long): Flow<Int>

    @Query("SELECT * FROM clipboard_access_events ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getPage(limit: Int, offset: Int): Flow<List<ClipboardAccessEventEntity>>

    @Query("DELETE FROM clipboard_access_events WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM clipboard_access_events")
    suspend fun deleteAll()
}
