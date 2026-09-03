package com.bigdatamonitor.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.bigdatamonitor.data.db.entity.AppUsageEventEntity
import com.bigdatamonitor.data.db.entity.ClipboardAccessEventEntity
import com.bigdatamonitor.data.repository.ClipboardRepository
import com.bigdatamonitor.data.repository.UsageRepository
import com.bigdatamonitor.util.PermissionUtil
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 无障碍服务，捕获屏幕上的隐私相关事件：
 * 1. Android 12+ "XXX 已从剪贴板粘贴" Toast → 识别读取剪贴板的 App
 * 2. App 前后台切换 → 记录哪个 App 在何时处于前台
 */
class PrivacyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PrivacyA11y"
        private const val DEBOUNCE_MS = 500L
        private val CLIPBOARD_KEYWORDS = listOf("粘贴", "剪贴板", "pasted", "clipboard", "复制")
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AccessibilityEntryPoint {
        fun clipboardRepository(): ClipboardRepository
        fun usageRepository(): UsageRepository
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastWindowChangeTime = 0L
    private var lastWindowPackage: String? = null

    private val clipboardRepository: ClipboardRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            AccessibilityEntryPoint::class.java
        ).clipboardRepository()
    }

    private val usageRepository: UsageRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            AccessibilityEntryPoint::class.java
        ).usageRepository()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return

        // 过滤系统 UI（但 Toast 需要处理）
        val isSystemUi = packageName == "com.android.systemui" ||
            packageName == "android" ||
            packageName.startsWith("com.android.")

        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                handleNotificationStateChanged(event, packageName, isSystemUi)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(packageName, isSystemUi)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Debounce 高频内容变化事件
                val now = System.currentTimeMillis()
                if (now - lastWindowChangeTime < DEBOUNCE_MS) return
                lastWindowChangeTime = now
            }
        }
    }

    /** 处理通知状态变化（包括 Toast） */
    private fun handleNotificationStateChanged(
        event: AccessibilityEvent,
        packageName: String,
        isSystemUi: Boolean
    ) {
        val text = event.text?.joinToString(" ")?.lowercase() ?: ""
        if (text.isEmpty()) return

        // 检查是否是剪贴板访问 Toast
        val isClipboardAccess = CLIPBOARD_KEYWORDS.any { text.contains(it.lowercase()) }
        if (isClipboardAccess) {
            // Android 12+ 的剪贴板 Toast 来自系统 UI
            // 当前前台 App 即为读取方
            val readerPackage = if (isSystemUi) {
                lastWindowPackage ?: packageName
            } else {
                packageName
            }

            scope.launch {
                try {
                    val entity = ClipboardAccessEventEntity(
                        timestamp = System.currentTimeMillis(),
                        packageName = readerPackage ?: "unknown",
                        clipboardContentHash = null
                    )
                    clipboardRepository.insertClipboardAccessEvent(entity)
                    Log.d(TAG, "Clipboard access detected by: $readerPackage")
                } catch (e: Exception) {
                    Log.e(TAG, "Error recording clipboard access", e)
                }
            }
        }
    }

    /** 处理窗口状态变化（前后台切换） */
    private fun handleWindowStateChanged(packageName: String, isSystemUi: Boolean) {
        if (isSystemUi) return

        val now = System.currentTimeMillis()
        val eventType = if (packageName != lastWindowPackage) {
            // 新 App 进入前台，旧 App 进入后台
            if (lastWindowPackage != null) {
                // 先记录旧 App 进入后台
                scope.launch {
                    try {
                        val bgEvent = AppUsageEventEntity(
                            timestamp = now,
                            packageName = lastWindowPackage!!,
                            eventType = "background",
                            durationMs = null
                        )
                        usageRepository.insert(bgEvent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error recording background event", e)
                    }
                }
            }
            // 记录新 App 进入前台
            scope.launch {
                try {
                    val fgEvent = AppUsageEventEntity(
                        timestamp = now,
                        packageName = packageName,
                        eventType = "foreground",
                        durationMs = null
                    )
                    usageRepository.insert(fgEvent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error recording foreground event", e)
                }
            }
            "foreground"
        } else {
            return
        }

        lastWindowPackage = packageName
        lastWindowChangeTime = now
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        scope.cancel()
        return super.onUnbind(intent)
    }
}
