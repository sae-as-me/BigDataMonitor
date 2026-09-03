package com.bigdatamonitor.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bigdatamonitor.data.db.entity.SensitiveEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensitiveEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SensitiveEventEntity): Long

    @Delete
    suspend fun delete(event: SensitiveEventEntity)

    @Query("SELECT * FROM sensitive_events ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<SensitiveEventEntity>>

    @Query("SELECT * FROM sensitive_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SensitiveEventEntity?

    @Query("SELECT * FROM sensitive_events")
    suspend fun getAll(): List<SensitiveEventEntity>

    @Query("DELETE FROM sensitive_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sensitive_events")
    suspend fun deleteAll()
}
