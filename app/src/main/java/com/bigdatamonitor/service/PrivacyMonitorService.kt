package com.bigdatamonitor.service

import android.app.Service
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.bigdatamonitor.data.db.entity.ClipboardEventEntity
import com.bigdatamonitor.data.repository.ClipboardRepository
import com.bigdatamonitor.data.datastore.SettingsDataStore
import com.bigdatamonitor.util.HashUtil
import com.bigdatamonitor.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 隐私监控前台服务。
 * 持有剪贴板监听器，监控剪贴板内容变化。
 */
@AndroidEntryPoint
class PrivacyMonitorService : Service() {

    companion object {
        private const val TAG = "PrivacyMonitorService"
        const val ACTION_START = "com.bigdatamonitor.START_MONITOR"
        const val ACTION_STOP = "com.bigdatamonitor.STOP_MONITOR"

        fun start(context: android.content.Context) {
            val intent = Intent(context, PrivacyMonitorService::class.java)
            intent.action = ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: android.content.Context) {
            val intent = Intent(context, PrivacyMonitorService::class.java)
            context.stopService(intent)
        }
    }

    @Inject
    lateinit var clipboardRepository: ClipboardRepository

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var isMonitoring = false

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        startForegroundCompat()
        registerClipboardListener()
        isMonitoring = true
        Log.i(TAG, "PrivacyMonitorService started")
    }

    private fun startForegroundCompat() {
        val notification = NotificationHelper.createMonitorNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.NOTIF_ID_MONITOR,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.NOTIF_ID_MONITOR, notification)
        }
    }

    private fun registerClipboardListener() {
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            scope.launch {
                try {
                    val clip = clipboardManager.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val item = clip.getItemAt(0)
                        val text = item.text?.toString() ?: ""
                        val contentType = if (text.isNotEmpty()) "text" else if (item.uri != null) "uri" else "intent"
                        val storageMode = settingsDataStore.clipboardStorageModeFlow.first()

                        val contentPreview = if (storageMode == "full") {
                            HashUtil.preview(text, 200)
                        } else {
                            HashUtil.preview(text, 20)
                        }

                        val contentHash = HashUtil.sha256(text)
                        val currentPackage = try {
                            // 尝试获取来源 App（无法可靠获取时为 null）
                            null
                        } catch (e: Exception) {
                            null
                        }

                        val entity = ClipboardEventEntity(
                            timestamp = System.currentTimeMillis(),
                            contentHash = contentHash,
                            contentPreview = contentPreview,
                            contentType = contentType,
                            sourceApp = currentPackage
                        )
                        clipboardRepository.insertClipboardEvent(entity)
                        Log.d(TAG, "Clipboard event recorded: $contentPreview")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing clipboard event", e)
                }
            }
        }
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        clipboardListener?.let { listener ->
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.removePrimaryClipChangedListener(listener)
        }
        scope.cancel()
        Log.i(TAG, "PrivacyMonitorService stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
