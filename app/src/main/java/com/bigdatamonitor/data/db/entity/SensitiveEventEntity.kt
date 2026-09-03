package com.bigdatamonitor.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户手动标注的敏感事件（如"与朋友谈论爬山"）。
 */
@Entity(tableName = "sensitive_events", indices = [Index("timestamp")])
data class SensitiveEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val title: String,
    val description: String,
    val keywords: String,
    val scope: String
)
