package com.bigdatamonitor.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bigdatamonitor.data.db.entity.NotificationEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: NotificationEventEntity): Long

    @Query("SELECT * FROM notification_events WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getByTimeRange(start: Long, end: Long): List<NotificationEventEntity>

    @Query("SELECT * FROM notification_events WHERE packageName = :pkg ORDER BY timestamp DESC")
    fun getByPackage(pkg: String): Flow<List<NotificationEventEntity>>

    @Query("SELECT * FROM notification_events WHERE matchedTopicIds IS NOT NULL AND matchedTopicIds != '' ORDER BY timestamp DESC")
    fun getMatchedFlow(): Flow<List<NotificationEventEntity>>

    @Query("SELECT COUNT(*) FROM notification_events WHERE matchedTopicIds IS NOT NULL AND matchedTopicIds != '' AND timestamp >= :since")
    fun countMatchedSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM notification_events WHERE packageName = :pkg AND matchedTopicIds IS NOT NULL AND matchedTopicIds != '' AND timestamp >= :since")
    suspend fun countMatchedByPackageSince(pkg: String, since: Long): Int

    @Query("SELECT * FROM notification_events ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getPage(limit: Int, offset: Int): Flow<List<NotificationEventEntity>>

    @Query("SELECT COUNT(*) FROM notification_events WHERE timestamp >= :since")
    fun countSince(since: Long): Flow<Int>

    @Query("DELETE FROM notification_events WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM notification_events")
    suspend fun deleteAll()
}
