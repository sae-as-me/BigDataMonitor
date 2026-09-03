package com.bigdatamonitor.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bigdatamonitor.data.db.entity.CorrelationResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CorrelationResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: CorrelationResultEntity): Long

    @Query("SELECT * FROM correlation_results WHERE sensitiveEventId = :eventId ORDER BY createdAt DESC")
    fun getBySensitiveEvent(eventId: Long): Flow<List<CorrelationResultEntity>>

    @Query("SELECT * FROM correlation_results WHERE sensitiveEventId = :eventId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestBySensitiveEvent(eventId: Long): CorrelationResultEntity?

    @Query("DELETE FROM correlation_results WHERE sensitiveEventId = :eventId")
    suspend fun deleteBySensitiveEvent(eventId: Long)

    @Query("DELETE FROM correlation_results")
    suspend fun deleteAll()
}
