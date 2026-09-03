package com.bigdatamonitor.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.bigdatamonitor.data.db.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Upsert
    suspend fun upsert(app: AppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppEntity>)

    @Query("SELECT * FROM apps ORDER BY riskScore DESC")
    fun getAllFlow(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): AppEntity?

    @Query("SELECT * FROM apps ORDER BY riskScore DESC LIMIT :limit")
    fun getTopRiskApps(limit: Int): Flow<List<AppEntity>>

    @Query("SELECT COUNT(*) FROM apps WHERE riskLevel = :level")
    fun countByRiskLevel(level: String): Flow<Int>

    @Query("UPDATE apps SET riskScore = :score, riskLevel = :level, lastAudited = :timestamp WHERE packageName = :pkg")
    suspend fun updateRiskScore(pkg: String, score: Int, level: String, timestamp: Long)

    @Query("DELETE FROM apps WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("DELETE FROM apps")
    suspend fun deleteAll()
}
