package com.bigdatamonitor.domain

import com.bigdatamonitor.data.db.entity.SensitiveEventEntity
import com.bigdatamonitor.data.repository.ClipboardRepository
import com.bigdatamonitor.data.repository.CorrelationRepository
import com.bigdatamonitor.data.repository.NetworkRepository
import com.bigdatamonitor.data.repository.NotificationRepository
import com.bigdatamonitor.data.repository.UsageRepository
import com.bigdatamonitor.data.db.entity.CorrelationResultEntity
import com.bigdatamonitor.domain.model.CorrelationResult
import com.bigdatamonitor.domain.model.RelatedEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 关联分析引擎。
 * 将分散的监控信号串联成有意义的隐私洞察。
 *
 * 核心算法：给定一个敏感事件 S（时间 T，关键词集 K），系统在以下 4 条线索中搜索匹配项：
 * 1. 剪贴板关联：T 前后 30 分钟内、内容包含 K 中关键词的剪贴板事件
 * 2. 通知关联：T 后 24 小时内、title 或 text 包含 K 中关键词的通知
 * 3. App 使用关联：T 前后 1 小时内异常活跃的 App
 * 4. 网络关联：T 前后连接广告/追踪域名的 App
 */
@Singleton
class CorrelationEngine @Inject constructor(
    private val clipboardRepository: ClipboardRepository,
    private val notificationRepository: NotificationRepository,
    private val usageRepository: UsageRepository,
    private val networkRepository: NetworkRepository,
    private val correlationRepository: CorrelationRepository
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    companion object {
        private const val CLIPBOARD_WINDOW_MS = 30 * 60 * 1000L  // 30 分钟
        private const val NOTIFICATION_WINDOW_MS = 24 * 60 * 60 * 1000L  // 24 小时
        private const val USAGE_WINDOW_MS = 60 * 60 * 1000L  // 1 小时
        private const val NETWORK_WINDOW_MS = 60 * 60 * 1000L  // 1 小时
    }

    /**
     * 对给定敏感事件执行关联分析，返回并持久化关联结果。
     */
    suspend fun analyze(sensitiveEvent: SensitiveEventEntity): CorrelationResultEntity {
        val keywords = sensitiveEvent.keywords
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        if (keywords.isEmpty()) {
            val emptyResult = CorrelationResult(
                sensitiveEventId = sensitiveEvent.id,
                relatedEvents = emptyList(),
                confidenceScore = 0f,
                summary = "无关键词可匹配"
            )
            return persist(sensitiveEvent.id, emptyResult)
        }

        val relatedEvents = mutableListOf<RelatedEvent>()

        // 1. 剪贴板关联
        val clipboardStart = sensitiveEvent.timestamp - CLIPBOARD_WINDOW_MS
        val clipboardEnd = sensitiveEvent.timestamp + CLIPBOARD_WINDOW_MS
        val clipboardEvents = clipboardRepository.getClipboardEventsList(clipboardStart, clipboardEnd)
        for (event in clipboardEvents) {
            val contentLower = event.contentPreview.lowercase()
            val matched = keywords.firstOrNull { contentLower.contains(it) }
            if (matched != null) {
                relatedEvents.add(
                    RelatedEvent(
                        eventType = "clipboard",
                        timestamp = event.timestamp,
                        packageName = event.sourceApp ?: "未知",
                        matchedContent = event.contentPreview,
                        matchReason = "剪贴板内容包含关键词「$matched」"
                    )
                )
            }
        }

        // 2. 通知关联
        val notifStart = sensitiveEvent.timestamp
        val notifEnd = sensitiveEvent.timestamp + NOTIFICATION_WINDOW_MS
        val notificationEvents = notificationRepository.getByTimeRange(notifStart, notifEnd)
        for (event in notificationEvents) {
            val titleLower = event.title.lowercase()
            val textLower = event.text.lowercase()
            val matched = keywords.firstOrNull {
                titleLower.contains(it) || textLower.contains(it)
            }
            if (matched != null) {
                relatedEvents.add(
                    RelatedEvent(
                        eventType = "notification",
                        timestamp = event.timestamp,
                        packageName = event.packageName,
                        matchedContent = "${event.title} ${event.text}".take(100),
                        matchReason = "通知内容匹配关键词「$matched」(${event.packageName})"
                    )
                )
            }
        }

        // 3. App 使用关联
        val usageStart = sensitiveEvent.timestamp - USAGE_WINDOW_MS
        val usageEnd = sensitiveEvent.timestamp + USAGE_WINDOW_MS
        val usageEvents = usageRepository.getByTimeRange(usageStart, usageEnd)
        // 统计每个包名的活跃次数，找出异常活跃的 App
        val usageByPackage = usageEvents.groupBy { it.packageName }
        val totalUsageEvents = usageEvents.size
        if (totalUsageEvents > 0) {
            val avgCount = totalUsageEvents.toDouble() / usageByPackage.size
            for ((pkg, events) in usageByPackage) {
                if (events.size > avgCount * 1.5 && events.size >= 3) {
                    relatedEvents.add(
                        RelatedEvent(
                            eventType = "app_usage",
                            timestamp = events.first().timestamp,
                            packageName = pkg,
                            matchedContent = "活跃 ${events.size} 次",
                            matchReason = "App 在敏感事件前后异常活跃（共 ${events.size} 次切换）"
                        )
                    )
                }
            }
        }

        // 4. 网络关联
        val networkStart = sensitiveEvent.timestamp - NETWORK_WINDOW_MS
        val networkEnd = sensitiveEvent.timestamp + NETWORK_WINDOW_MS
        val networkEvents = networkRepository.getByTimeRange(networkStart, networkEnd)
        val trackerEvents = networkEvents.filter { it.isTrackerDomain }
        // 按包名去重，每个包名只记录一次
        val seenPackages = mutableSetOf<String>()
        for (event in trackerEvents) {
            if (seenPackages.add(event.packageName)) {
                relatedEvents.add(
                    RelatedEvent(
                        eventType = "network",
                        timestamp = event.timestamp,
                        packageName = event.packageName,
                        matchedContent = event.targetHost,
                        matchReason = "连接了追踪/广告域名: ${event.targetHost}"
                    )
                )
            }
        }

        // 计算综合可信度
        var score = 0f
        val notificationHits = relatedEvents.count { it.eventType == "notification" }
        val clipboardHits = relatedEvents.count { it.eventType == "clipboard" }
        val networkHits = relatedEvents.count { it.eventType == "network" }
        val usageHits = relatedEvents.count { it.eventType == "app_usage" }

        score += minOf(notificationHits * 0.35f, 0.7f)
        score += if (clipboardHits > 0) 0.2f else 0f
        score += if (networkHits > 0) 0.15f else 0f
        score += if (usageHits > 0) 0.1f else 0f
        score = minOf(score, 1.0f)

        // 生成摘要
        val summary = buildString {
            append("发现 ${relatedEvents.size} 条疑似关联事件。")
            if (notificationHits > 0) append(" 其中 $notificationHits 条通知匹配了关键词。")
            if (clipboardHits > 0) append(" 剪贴板内容变化也包含相关关键词。")
            if (networkHits > 0) append(" 检测到 $networkHits 个 App 连接了追踪域名。")
            append(" 注意：关联为推断分析，非因果证明。")
        }

        val result = CorrelationResult(
            sensitiveEventId = sensitiveEvent.id,
            relatedEvents = relatedEvents,
            confidenceScore = score,
            summary = summary
        )

        return persist(sensitiveEvent.id, result)
    }

    private suspend fun persist(
        sensitiveEventId: Long,
        result: CorrelationResult
    ): CorrelationResultEntity {
        // 删除旧结果再插入新结果
        correlationRepository.deleteBySensitiveEvent(sensitiveEventId)
        val entity = CorrelationResultEntity(
            sensitiveEventId = sensitiveEventId,
            resultJson = json.encodeToString(result),
            confidenceScore = result.confidenceScore,
            createdAt = System.currentTimeMillis()
        )
        val id = correlationRepository.insert(entity)
        return entity.copy(id = id)
    }
}
