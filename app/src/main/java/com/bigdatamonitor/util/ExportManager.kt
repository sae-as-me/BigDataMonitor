package com.bigdatamonitor.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.bigdatamonitor.data.db.entity.*
import com.bigdatamonitor.data.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** 数据导出管理器 */
@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clipboardRepository: ClipboardRepository,
    private val notificationRepository: NotificationRepository,
    private val usageRepository: UsageRepository,
    private val permissionRepository: PermissionRepository,
    private val networkRepository: NetworkRepository,
    private val sensitiveEventRepository: SensitiveEventRepository,
    private val correlationRepository: CorrelationRepository
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Serializable
    data class ExportData(
        val exportTime: Long,
        val rangeStart: Long,
        val rangeEnd: Long,
        val clipboardEvents: List<ClipboardEventExport> = emptyList(),
        val clipboardAccessEvents: List<ClipboardAccessExport> = emptyList(),
        val notificationEvents: List<NotificationExport> = emptyList(),
        val usageEvents: List<UsageExport> = emptyList(),
        val networkEvents: List<NetworkExport> = emptyList(),
        val sensitiveEvents: List<SensitiveExport> = emptyList(),
        val correlationResults: List<CorrelationExport> = emptyList()
    )

    @Serializable data class ClipboardEventExport(val timestamp: Long, val contentHash: String, val contentPreview: String, val sourceApp: String?)
    @Serializable data class ClipboardAccessExport(val timestamp: Long, val packageName: String)
    @Serializable data class NotificationExport(val timestamp: Long, val packageName: String, val title: String, val text: String, val matchedTopicIds: String?)
    @Serializable data class UsageExport(val timestamp: Long, val packageName: String, val eventType: String, val durationMs: Long?)
    @Serializable data class NetworkExport(val timestamp: Long, val packageName: String, val protocol: String, val targetHost: String, val isTrackerDomain: Boolean)
    @Serializable data class SensitiveExport(val id: Long, val timestamp: Long, val title: String, val keywords: String, val scope: String)
    @Serializable data class CorrelationExport(val sensitiveEventId: Long, val resultJson: String, val confidenceScore: Float, val createdAt: Long)

    /** 导出为 JSON 文件并返回 Uri */
    suspend fun exportJson(start: Long, end: Long): Uri {
        val data = collectData(start, end)
        val jsonStr = json.encodeToString(data)
        val file = File(context.cacheDir, "export_${System.currentTimeMillis()}.json")
        file.writeText(jsonStr, Charsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** 导出为 CSV 文件并返回 Uri */
    suspend fun exportCsv(start: Long, end: Long): Uri {
        val sb = StringBuilder()
        sb.append("type,timestamp,packageName,title/text,matchedContent\n")

        // 剪贴板事件
        clipboardRepository.getClipboardEventsList(start, end).forEach { e ->
            sb.append("clipboard_change,${e.timestamp},${e.sourceApp ?: ""},\"${escapeCsv(e.contentPreview)}\",\n")
        }

        // 剪贴板读取
        clipboardRepository.getClipboardAccessEvents(start, end).forEach { e ->
            sb.append("clipboard_access,${e.timestamp},${e.packageName},,\n")
        }

        // 通知事件
        notificationRepository.getByTimeRange(start, end).forEach { e ->
            sb.append("notification,${e.timestamp},${e.packageName},\"${escapeCsv(e.title)}: ${escapeCsv(e.text)}\",${e.matchedTopicIds ?: ""}\n")
        }

        // 使用事件
        usageRepository.getByTimeRange(start, end).forEach { e ->
            sb.append("${e.eventType},${e.timestamp},${e.packageName},,\n")
        }

        // 网络事件
        networkRepository.getByTimeRange(start, end).forEach { e ->
            sb.append("network,${e.timestamp},${e.packageName},\"${e.protocol} ${e.targetHost}\",${if (e.isTrackerDomain) "tracker" else ""}\n")
        }

        val file = File(context.cacheDir, "export_${System.currentTimeMillis()}.csv")
        file.writeText(sb.toString(), Charsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private suspend fun collectData(start: Long, end: Long): ExportData {
        return ExportData(
            exportTime = System.currentTimeMillis(),
            rangeStart = start,
            rangeEnd = end,
            clipboardEvents = clipboardRepository.getClipboardEventsList(start, end).map {
                ClipboardEventExport(it.timestamp, it.contentHash, it.contentPreview, it.sourceApp)
            },
            clipboardAccessEvents = clipboardRepository.getClipboardAccessEvents(start, end).map {
                ClipboardAccessExport(it.timestamp, it.packageName)
            },
            notificationEvents = notificationRepository.getByTimeRange(start, end).map {
                NotificationExport(it.timestamp, it.packageName, it.title, it.text, it.matchedTopicIds)
            },
            usageEvents = usageRepository.getByTimeRange(start, end).map {
                UsageExport(it.timestamp, it.packageName, it.eventType, it.durationMs)
            },
            networkEvents = networkRepository.getByTimeRange(start, end).map {
                NetworkExport(it.timestamp, it.packageName, it.protocol, it.targetHost, it.isTrackerDomain)
            },
            sensitiveEvents = sensitiveEventRepository.getAll().map {
                SensitiveExport(it.id, it.timestamp, it.title, it.keywords, it.scope)
            },
            correlationResults = sensitiveEventRepository.getAll().mapNotNull { se ->
                correlationRepository.getLatestBySensitiveEvent(se.id)?.let { cr ->
                    CorrelationExport(cr.sensitiveEventId, cr.resultJson, cr.confidenceScore, cr.createdAt)
                }
            }
        )
    }

    private fun escapeCsv(text: String): String {
        return text.replace("\"", "\"\"").replace("\n", " ").replace("\r", "")
    }
}
