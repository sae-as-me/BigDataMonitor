package com.bigdatamonitor.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bigdatamonitor.MainActivity
import com.bigdatamonitor.R

/** 通知工具类，管理前台服务通知 */
object NotificationHelper {

    const val CHANNEL_MONITOR = "monitor_channel"
    const val CHANNEL_VPN = "vpn_channel"
    const val NOTIF_ID_MONITOR = 1
    const val NOTIF_ID_VPN = 2

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR,
                context.getString(R.string.foreground_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "BigDataMonitor 监控服务运行状态"
                setShowBadge(false)
            }

            val vpnChannel = NotificationChannel(
                CHANNEL_VPN,
                context.getString(R.string.vpn_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "网络监控 VPN 服务运行状态"
                setShowBadge(false)
            }

            manager.createNotificationChannel(monitorChannel)
            manager.createNotificationChannel(vpnChannel)
        }
    }

    /** 创建监控前台服务通知 */
    fun createMonitorNotification(context: Context): Notification {
        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent("com.bigdatamonitor.ACTION_STOP_MONITOR")
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setContentTitle(context.getString(R.string.foreground_notification_title))
            .setContentText(context.getString(R.string.foreground_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "暂停", stopPendingIntent)
            .build()
    }

    /** 创建 VPN 前台服务通知 */
    fun createVpnNotification(context: Context): Notification {
        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_VPN)
            .setContentTitle(context.getString(R.string.vpn_notification_title))
            .setContentText(context.getString(R.string.vpn_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
