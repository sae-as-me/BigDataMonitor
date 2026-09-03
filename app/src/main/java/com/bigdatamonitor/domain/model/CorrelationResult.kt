package com.bigdatamonitor.domain.model

import kotlinx.serialization.Serializable

/** 关联分析结果中的单个关联事件 */
@Serializable
data class RelatedEvent(
    val eventType: String,
    val timestamp: Long,
    val packageName: String,
    val matchedContent: String,
    val matchReason: String
)

/** 完整关联分析结果 */
@Serializable
data class CorrelationResult(
    val sensitiveEventId: Long,
    val relatedEvents: List<RelatedEvent> = emptyList(),
    val confidenceScore: Float = 0f,
    val summary: String = ""
)

/** 风险评分明细 */
data class AppRiskScore(
    val packageName: String,
    val appName: String,
    val totalScore: Int = 0,
    val level: String = "unknown",
    val breakdown: Map<String, Int> = emptyMap(),
    val lastUpdated: Long = System.currentTimeMillis()
)
