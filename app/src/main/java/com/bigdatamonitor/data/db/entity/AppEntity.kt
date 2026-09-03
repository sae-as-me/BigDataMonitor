package com.bigdatamonitor.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 已安装应用信息实体。
 */
@Entity(tableName = "apps", indices = [Index("riskScore")])
data class AppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val installerPackage: String?,
    val riskScore: Int = 0,
    val riskLevel: String = "unknown",
    val lastAudited: Long? = null
)
