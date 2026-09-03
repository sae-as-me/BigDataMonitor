package com.bigdatamonitor.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 应用使用事件（前后台切换）。
 */
@Entity(tableName = "app_usage_events", indices = [Index("timestamp"), Index("packageName")])
data class AppUsageEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val eventType: String,
    val durationMs: Long? = null
)
