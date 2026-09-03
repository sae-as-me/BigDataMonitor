package com.bigdatamonitor.domain

import com.bigdatamonitor.data.repository.ClipboardRepository
import com.bigdatamonitor.data.repository.NetworkRepository
import com.bigdatamonitor.data.repository.NotificationRepository
import com.bigdatamonitor.data.repository.PermissionRepository
import com.bigdatamonitor.data.repository.UsageRepository
import com.bigdatamonitor.domain.model.AppRiskScore
import com.bigdatamonitor.domain.model.RiskLevel
import com.bigdatamonitor.domain.model.SensitivePermission
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 风险评分引擎。
 * 为每个 App 计算隐私风险评分（0-100）。
 *
 * 评分模型（加权打分）：
 * - 敏感权限数量 30%
 * - 剪贴板读取频率 20%
 * - 通知关联命中率 20%
 * - 后台活跃度 15%
 * - 网络追踪域名 15%
 */
@Singleton
class RiskScorer @Inject constructor(
    private val permissionRepository: PermissionRepository,
    private val clipboardRepository: ClipboardRepository,
    private val notificationRepository: NotificationRepository,
    private val usageRepository: UsageRepository,
    private val networkRepository: NetworkRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val STAT_WINDOW_MS = 24 * 60 * 60 * 1000L // 24 小时窗口
        private const val MAX_PERMISSION_SCORE = 30
        private const val MAX_CLIPBOARD_SCORE = 20
        private const val MAX_NOTIFICATION_SCORE = 20
        private const val MAX_USAGE_SCORE = 15
        private const val MAX_NETWORK_SCORE = 15
    }

    /**
     * 计算指定 App 的隐私风险评分。
     */
    suspend fun scoreApp(packageName: String, appName: String): AppRiskScore {
        val now = System.currentTimeMillis()
        val since = now - STAT_WINDOW_MS

        // 1. 敏感权限数量 (30%)
        val snapshot = permissionRepository.getLatestPermissionSnapshot(packageName)
        val permissionScore = if (snapshot != null) {
            val perms = parsePermissionsJson(snapshot.permissionsJson)
            val sensitiveCount = perms.count {
                SensitivePermission.fromPermission(it.name) != null && it.isGranted
            }
            //敏感权限数 / 10 * 30, 上限30
            minOf(sensitiveCount * 3, MAX_PERMISSION_SCORE)
        } else {
            0
        }

        // 2. 剪贴板读取频率 (20%)
        val clipboardAccessCount = clipboardRepository.countClipboardAccessByPackage(packageName, since)
        val clipboardScore = minOf(clipboardAccessCount * 4, MAX_CLIPBOARD_SCORE)

        // 3. 通知关联命中率 (20%)
        val matchedCount = notificationRepository.countMatchedByPackageSince(packageName, since)
        val notificationScore = minOf(matchedCount * 5, MAX_NOTIFICATION_SCORE)

        // 4. 后台活跃度 (15%)
        val backgroundCount = usageRepository.countBackgroundByPackageSince(packageName, since)
        val usageScore = minOf(backgroundCount * 2, MAX_USAGE_SCORE)

        // 5. 网络追踪域名 (15%)
        val trackerCount = networkRepository.countTrackerDomainsByPackageSince(packageName, since)
        val networkScore = minOf(trackerCount * 3, MAX_NETWORK_SCORE)

        val totalScore = permissionScore + clipboardScore + notificationScore + usageScore + networkScore
        val level = RiskLevel.fromScore(totalScore)

        val breakdown = mapOf(
            "敏感权限" to permissionScore,
            "剪贴板读取" to clipboardScore,
            "通知关联" to notificationScore,
            "后台活跃" to usageScore,
            "网络追踪" to networkScore
        )

        // 更新数据库
        permissionRepository.updateRiskScore(packageName, totalScore, level.key, now)

        return AppRiskScore(
            packageName = packageName,
            appName = appName,
            totalScore = totalScore,
            level = level.key,
            breakdown = breakdown,
            lastUpdated = now
        )
    }

    private data class ParsedPermission(val name: String, val isGranted: Boolean)

    private fun parsePermissionsJson(jsonStr: String): List<ParsedPermission> {
        return try {
            val jsonArray = json.parseToJsonElement(jsonStr).jsonArray
            jsonArray.map { element ->
                val obj = element.jsonObject
                ParsedPermission(
                    name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "",
                    isGranted = obj["isGranted"]?.jsonPrimitive?.contentOrNull == "true" ||
                            obj["isGranted"]?.jsonPrimitive?.intOrNull == 1
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
