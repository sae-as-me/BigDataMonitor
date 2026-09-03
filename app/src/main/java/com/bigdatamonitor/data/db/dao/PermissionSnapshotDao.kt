package com.bigdatamonitor.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bigdatamonitor.data.db.entity.PermissionSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PermissionSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: PermissionSnapshotEntity): Long

    @Query("SELECT * FROM permission_snapshots WHERE packageName = :pkg ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestByPackage(pkg: String): PermissionSnapshotEntity?

    @Query("DELETE FROM permission_snapshots WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM permission_snapshots")
    suspend fun deleteAll()
}
