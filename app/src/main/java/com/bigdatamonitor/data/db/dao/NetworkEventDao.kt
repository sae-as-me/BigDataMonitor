package com.bigdatamonitor.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bigdatamonitor.data.db.entity.NetworkEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: NetworkEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<NetworkEventEntity>)

    @Query("SELECT * FROM network_events WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getByTimeRange(start: Long, end: Long): List<NetworkEventEntity>

    @Query("SELECT * FROM network_events WHERE packageName = :pkg ORDER BY timestamp DESC")
    fun getByPackage(pkg: String): Flow<List<NetworkEventEntity>>

    @Query("SELECT COUNT(*) FROM network_events WHERE packageName = :pkg AND isTrackerDomain = 1 AND timestamp >= :since")
    suspend fun countTrackerDomainsByPackageSince(pkg: String, since: Long): Int

    @Query("SELECT COUNT(*) FROM network_events WHERE isTrackerDomain = 1 AND timestamp >= :since")
    fun countTrackerDomainsSince(since: Long): Flow<Int>

    @Query("SELECT * FROM network_events ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getPage(limit: Int, offset: Int): Flow<List<NetworkEventEntity>>

    @Query("SELECT COUNT(*) FROM network_events WHERE timestamp >= :since")
    fun countSince(since: Long): Flow<Int>

    @Query("DELETE FROM network_events WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM network_events")
    suspend fun deleteAll()
}
