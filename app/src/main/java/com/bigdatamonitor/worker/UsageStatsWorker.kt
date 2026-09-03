package com.bigdatamonitor.worker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bigdatamonitor.data.db.entity.AppUsageEventEntity
import com.bigdatamonitor.data.repository.UsageRepository
import com.bigdatamonitor.util.PermissionUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 使用统计追踪 Worker。
 * 每 15 分钟查询应用使用统计，记录前后台切换事件。
 */
@HiltWorker
class UsageStatsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageRepository: UsageRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "UsageStatsWorker"
        private const val QUERY_WINDOW_MS = 15 * 60 * 1000L
    }

    override suspend fun doWork(): Result {
        if (!PermissionUtil.isUsageStatsEnabled(applicationContext)) {
            Log.d(TAG, "Usage stats permission not granted, skipping")
            return Result.success()
        }

        return try {
            val usageStatsManager = applicationContext
                .getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val now = System.currentTimeMillis()
            val events = usageStatsManager.queryEvents(now - QUERY_WINDOW_MS, now)

            val usageEvents = mutableListOf<AppUsageEventEntity>()
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        usageEvents.add(AppUsageEventEntity(
                            timestamp = event.timeStamp,
                            packageName = event.packageName,
                            eventType = "foreground",
                            durationMs = null
                        ))
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        usageEvents.add(AppUsageEventEntity(
                            timestamp = event.timeStamp,
                            packageName = event.packageName,
                            eventType = "background",
                            durationMs = null
                        ))
                    }
                }
            }

            if (usageEvents.isNotEmpty()) {
                usageRepository.insertAll(usageEvents)
                Log.d(TAG, "Recorded ${usageEvents.size} usage events")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error querying usage stats", e)
            Result.success()
        }
    }
}
