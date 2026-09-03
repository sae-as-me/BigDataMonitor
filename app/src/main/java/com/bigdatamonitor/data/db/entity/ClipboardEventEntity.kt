package com.bigdatamonitor.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 剪贴板内容变化事件。
 * 记录剪贴板内容被写入（复制/剪切）的时间。
 */
@Entity(tableName = "clipboard_events", indices = [Index("timestamp"), Index("contentHash")])
data class ClipboardEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val contentHash: String,
    val contentPreview: String,
    val contentType: String,
    val sourceApp: String?
)
