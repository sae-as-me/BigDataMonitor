package com.bigdatamonitor.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.bigdatamonitor.data.db.entity.NotificationEventEntity
import com.bigdatamonitor.data.db.entity.SensitiveEventEntity
import com.bigdatamonitor.data.repository.NotificationRepository
import com.bigdatamonitor.data.repository.SensitiveEventRepository
import com.bigdatamonitor.data.datastore.SettingsDataStore
import com.bigdatamonitor.util.HashUtil
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 通知监听服务。
 * 捕获所有 App 推送的通知，记录通知内容、来源 App 和时间。
 * 与用户标注的敏感话题做关键词匹配，标记"疑似关联"通知。
 */
class NotificationMonitorService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotifMonitor"
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationEntryPoint {
        fun notificationRepository(): NotificationRepository
        fun sensitiveEventRepository(): SensitiveEventRepository
        fun settingsDataStore(): SettingsDataStore
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val notificationRepository: NotificationRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationEntryPoint::class.java
        ).notificationRepository()
    }

    private val sensitiveEventRepository: SensitiveEventRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationEntryPoint::class.java
        ).sensitiveEventRepository()
    }

    private val settingsDataStore: SettingsDataStore by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationEntryPoint::class.java
        ).settingsDataStore()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val notification = sbn.notification ?: return
        val packageName = sbn.packageName ?: return

        // 过滤自身通知
        if (packageName == applicationContext.packageName) return

        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        scope.launch {
            try {
                // 检查存储模式
                val storageMode = settingsDataStore.notificationStorageModeFlow.first()
                val storedText = if (storageMode == "full") {
                    HashUtil.preview(text, 500)
                } else {
                    HashUtil.preview(text, 50)
                }
                val storedTitle = HashUtil.preview(title, 100)

                // 查询所有活跃的敏感事件关键词进行匹配
                val sensitiveEvents = sensitiveEventRepository.getAll()
                val matchedIds = mutableListOf<String>()

                val combinedText = "${storedTitle.lowercase()} ${storedText.lowercase()}"
                for (event in sensitiveEvents) {
                    val keywords = event.keywords.split(",")
                        .map { it.trim().lowercase() }
                        .filter { it.isNotEmpty() }
                    val matched = keywords.any { combinedText.contains(it) }
                    if (matched) {
                        matchedIds.add(event.id.toString())
                    }
                }

                val entity = NotificationEventEntity(
                    timestamp = System.currentTimeMillis(),
                    packageName = packageName,
                    title = storedTitle,
                    text = storedText,
                    category = notification.category?.toString(),
                    matchedTopicIds = if (matchedIds.isEmpty()) null else matchedIds.joinToString(",")
                )
                notificationRepository.insert(entity)
                if (matchedIds.isNotEmpty()) {
                    Log.d(TAG, "Notification matched sensitive events: $matchedIds from $packageName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification from $packageName", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // 不处理通知移除
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
