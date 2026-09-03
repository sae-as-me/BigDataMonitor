package com.bigdatamonitor.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 关联分析结果。
 */
@Entity(tableName = "correlation_results", indices = [Index("sensitiveEventId")])
data class CorrelationResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sensitiveEventId: Long,
    val resultJson: String,
    val confidenceScore: Float,
    val createdAt: Long
)
