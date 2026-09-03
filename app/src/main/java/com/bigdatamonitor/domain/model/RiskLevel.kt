package com.bigdatamonitor.domain.model

/** 风险等级 */
enum class RiskLevel(val key: String, val label: String, val scoreRange: IntRange) {
    UNKNOWN("unknown", "未知", 0..0),
    LOW("low", "低风险", 1..29),
    MEDIUM("medium", "中风险", 30..59),
    HIGH("high", "高风险", 60..84),
    CRITICAL("critical", "极高风险", 85..100);

    companion object {
        fun fromScore(score: Int): RiskLevel {
            return entries.find { score in it.scoreRange } ?: UNKNOWN
        }

        fun fromKey(key: String): RiskLevel {
            return entries.find { it.key == key } ?: UNKNOWN
        }
    }
}
