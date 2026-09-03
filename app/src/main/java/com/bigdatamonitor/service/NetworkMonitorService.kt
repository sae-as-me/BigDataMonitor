package com.bigdatamonitor.service

import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.bigdatamonitor.data.db.entity.NetworkEventEntity
import com.bigdatamonitor.data.repository.NetworkRepository
import com.bigdatamonitor.domain.model.TrackerDomains
import com.bigdatamonitor.util.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * 网络监控 VPN 服务。
 * 通过 VpnService 建立本地 VPN Tunnel，捕获各 App 的网络连接目标（域名/IP）。
 * 可选模块，默认关闭。
 */
class NetworkMonitorService : VpnService() {

    companion object {
        private const val TAG = "NetworkMonitor"
        private const val VPN_MTU = 1500
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ROUTE = "0.0.0.0"

        fun start(context: android.content.Context, vpnService: VpnService) {
            val intent = Intent(context, NetworkMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NetworkEntryPoint {
        fun networkRepository(): NetworkRepository
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var workerThread: Thread? = null
    private var isRunning = false

    private val networkRepository: NetworkRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            NetworkEntryPoint::class.java
        ).networkRepository()
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        startForeground(
            NotificationHelper.NOTIF_ID_VPN,
            NotificationHelper.createVpnNotification(this)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) return START_STICKY
        setupVpn()
        return START_STICKY
    }

    private fun setupVpn() {
        try {
            val builder = Builder()
                .setSession("BigDataMonitor VPN")
                .setMtu(VPN_MTU)
                .addAddress(VPN_ADDRESS, 24)
                .addRoute(VPN_ROUTE, 0)
                .addDnsServer("8.8.8.8")

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                isRunning = true
                startPacketLoop()
                Log.i(TAG, "VPN established")
            } else {
                Log.e(TAG, "Failed to establish VPN")
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up VPN", e)
            stopSelf()
        }
    }

    /**
     * 网络数据包处理循环。
     * 简化实现：解析 DNS 请求提取域名。
     * 完整实现需要解析 TCP/IP 数据包头并关联 UID。
     */
    private fun startPacketLoop() {
        workerThread = Thread {
            val fd = vpnInterface?.fileDescriptor ?: return@Thread
            val input = FileInputStream(fd)
            val buffer = ByteBuffer.allocate(VPN_MTU)

            while (isRunning && !Thread.interrupted()) {
                try {
                    val length = input.read(buffer.array())
                    if (length > 0) {
                        buffer.limit(length)
                        processPacket(buffer)
                        buffer.clear()
                    }
                } catch (e: Exception) {
                    if (isRunning) {
                        Log.e(TAG, "Error reading packet", e)
                    }
                    break
                }
            }
        }.also { it.start() }
    }

    /**
     * 处理单个网络数据包。
     * 解析 IP 头和 UDP/TCP 头，提取目标地址和端口。
     * DNS 请求（UDP 53）尝试提取域名。
     */
    private fun processPacket(buffer: ByteBuffer) {
        try {
            if (buffer.remaining() < 20) return

            val version = (buffer.get(0).toInt() ushr 4) and 0x0F
            if (version != 4) return // 仅处理 IPv4

            val protocol = buffer.get(9).toInt() and 0xFF
            val dstAddr = byteToInt(buffer.get(12), buffer.get(13), buffer.get(14), buffer.get(15))
            val dstIp = InetAddress.getByAddress(buffer.array().copyOfRange(12, 16)).hostAddress ?: return

            // 提取目标端口
            val dstPort = if (buffer.remaining() >= 20) {
                ((buffer.get(16 + 2).toInt() and 0xFF) shl 8) or (buffer.get(16 + 3).toInt() and 0xFF)
            } else {
                0
            }

            val protocolName = when (protocol) {
                6 -> "TCP"
                17 -> "UDP"
                else -> "IP$protocol"
            }

            val isTracker = TrackerDomains.isTrackerDomain(dstIp)

            // 记录到数据库（简化版，无法准确获取 UID）
            scope.launch {
                try {
                    val event = NetworkEventEntity(
                        timestamp = System.currentTimeMillis(),
                        uid = -1,
                        packageName = "unknown",
                        protocol = protocolName,
                        targetHost = dstIp,
                        targetPort = dstPort,
                        bytesOut = buffer.limit().toLong(),
                        bytesIn = 0,
                        isTrackerDomain = isTracker
                    )
                    networkRepository.insert(event)
                } catch (e: Exception) {
                    Log.e(TAG, "Error storing network event", e)
                }
            }
        } catch (e: Exception) {
            // 忽略解析错误
        }
    }

    private fun byteToInt(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Int {
        return ((b1.toInt() and 0xFF) shl 24) or
            ((b2.toInt() and 0xFF) shl 16) or
            ((b3.toInt() and 0xFF) shl 8) or
            (b4.toInt() and 0xFF)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        workerThread?.interrupt()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
        vpnInterface = null
        scope.cancel()
        Log.i(TAG, "NetworkMonitorService stopped")
    }

    override fun onRevoke() {
        super.onRevoke()
        isRunning = false
        stopSelf()
    }
}
