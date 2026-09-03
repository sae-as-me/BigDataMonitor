package com.bigdatamonitor.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 剪贴板被读取事件（由无障碍服务捕获 Android 12+ Toast）。
 */
@Entity(tableName = "clipboard_access_events", indices = [Index("timestamp"), Index("packageName")])
data class ClipboardAccessEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val clipboardContentHash: String?
)
