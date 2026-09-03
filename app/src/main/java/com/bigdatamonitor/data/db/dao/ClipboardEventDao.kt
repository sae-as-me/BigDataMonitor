package com.bigdatamonitor.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bigdatamonitor.data.db.entity.ClipboardEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ClipboardEventEntity): Long

    @Query("SELECT * FROM clipboard_events WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getByTimeRange(start: Long, end: Long): Flow<List<ClipboardEventEntity>>

    @Query("SELECT * FROM clipboard_events WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getByTimeRangeList(start: Long, end: Long): List<ClipboardEventEntity>

    @Query("SELECT COUNT(*) FROM clipboard_events WHERE timestamp >= :since")
    fun countSince(since: Long): Flow<Int>

    @Query("SELECT * FROM clipboard_events ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getPage(limit: Int, offset: Int): Flow<List<ClipboardEventEntity>>

    @Query("SELECT * FROM clipboard_events ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<ClipboardEventEntity>>

    @Query("DELETE FROM clipboard_events WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM clipboard_events")
    suspend fun deleteAll()
}
