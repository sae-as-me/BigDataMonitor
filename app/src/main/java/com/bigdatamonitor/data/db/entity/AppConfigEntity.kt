package com.bigdatamonitor.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 应用配置键值对存储。
 */
@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
