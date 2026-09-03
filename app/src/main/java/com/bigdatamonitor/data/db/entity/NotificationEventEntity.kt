package com.bigdatamonitor.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 通知事件，记录推送通知的来源、标题和正文。
 */
@Entity(tableName = "notification_events", indices = [Index("timestamp"), Index("packageName")])
data class NotificationEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val title: String,
    val text: String,
    val category: String? = null,
    val matchedTopicIds: String? = null
)
