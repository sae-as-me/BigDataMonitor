package com.bigdatamonitor.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bigdatamonitor.data.db.entity.AppConfigEntity

@Dao
interface AppConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(config: AppConfigEntity)

    @Query("SELECT value FROM app_config WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Query("DELETE FROM app_config")
    suspend fun deleteAll()
}
