package com.bigdatamonitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bigdatamonitor.worker.PermissionAuditWorker

/**
 * 应用安装/卸载监听 Receiver。
 * 当有应用安装或卸载时，触发一次权限审计。
 */
class PackageChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PackageChangeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                Log.d(TAG, "Package added: ${intent.data?.schemeSpecificPart}")
                triggerPermissionAudit(context)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                Log.d(TAG, "Package removed: ${intent.data?.schemeSpecificPart}")
                // 卸载后也可触发审计以更新应用列表
                triggerPermissionAudit(context)
            }
        }
    }

    private fun triggerPermissionAudit(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<PermissionAuditWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
