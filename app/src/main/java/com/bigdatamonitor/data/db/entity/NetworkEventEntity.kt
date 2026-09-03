package com.bigdatamonitor.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 网络连接事件（由 VpnService 捕获）。
 */
@Entity(tableName = "network_events", indices = [Index("timestamp"), Index("packageName")])
data class NetworkEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val uid: Int,
    val packageName: String,
    val protocol: String,
    val targetHost: String,
    val targetPort: Int? = null,
    val bytesOut: Long = 0,
    val bytesIn: Long = 0,
    val isTrackerDomain: Boolean = false
)
