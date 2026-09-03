package com.bigdatamonitor.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * WorkManager 定时任务调度器。
 */
object WorkScheduler {

    private const val WORK_USAGE_STATS = "work_usage_stats"
    private const val WORK_PERMISSION_AUDIT = "work_permission_audit"
    private const val WORK_DATA_CLEANUP = "work_data_cleanup"

    fun schedulePeriodicTasks(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 使用统计追踪：每 15 分钟
        val usageStatsWork = PeriodicWorkRequestBuilder<UsageStatsWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()
        ).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_USAGE_STATS,
            ExistingPeriodicWorkPolicy.KEEP,
            usageStatsWork
        )

        // 权限审计：每 24 小时
        val permissionAuditWork = PeriodicWorkRequestBuilder<PermissionAuditWorker>(
            1, TimeUnit.DAYS
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()
        ).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_PERMISSION_AUDIT,
            ExistingPeriodicWorkPolicy.KEEP,
            permissionAuditWork
        )

        // 数据清理：每 24 小时
        val dataCleanupWork = PeriodicWorkRequestBuilder<DataCleanupWorker>(
            1, TimeUnit.DAYS
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(true)
                .build()
        ).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_DATA_CLEANUP,
            ExistingPeriodicWorkPolicy.KEEP,
            dataCleanupWork
        )
    }
}
