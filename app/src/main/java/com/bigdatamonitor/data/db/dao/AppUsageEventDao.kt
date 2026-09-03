package com.bigdatamonitor.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bigdatamonitor.data.db.entity.AppUsageEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: AppUsageEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<AppUsageEventEntity>)

    @Query("SELECT * FROM app_usage_events WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getByTimeRange(start: Long, end: Long): List<AppUsageEventEntity>

    @Query("SELECT * FROM app_usage_events WHERE packageName = :pkg ORDER BY timestamp DESC")
    fun getByPackage(pkg: String): Flow<List<AppUsageEventEntity>>

    @Query("SELECT * FROM app_usage_events ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getPage(limit: Int, offset: Int): Flow<List<AppUsageEventEntity>>

    @Query("SELECT COUNT(*) FROM app_usage_events WHERE packageName = :pkg AND eventType = 'background' AND timestamp >= :since")
    suspend fun countBackgroundByPackageSince(pkg: String, since: Long): Int

    @Query("SELECT * FROM app_usage_events ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<AppUsageEventEntity>>

    @Query("DELETE FROM app_usage_events WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM app_usage_events")
    suspend fun deleteAll()
}
