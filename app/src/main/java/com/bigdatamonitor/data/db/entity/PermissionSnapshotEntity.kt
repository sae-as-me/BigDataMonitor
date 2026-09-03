package com.bigdatamonitor.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 应用权限快照。
 */
@Entity(tableName = "permission_snapshots", indices = [Index("timestamp"), Index("packageName")])
data class PermissionSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val permissionsJson: String
)
