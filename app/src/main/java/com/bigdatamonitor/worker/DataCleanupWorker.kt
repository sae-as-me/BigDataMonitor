package com.bigdatamonitor.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bigdatamonitor.data.datastore.SettingsDataStore
import com.bigdatamonitor.data.repository.ClipboardRepository
import com.bigdatamonitor.data.repository.NetworkRepository
import com.bigdatamonitor.data.repository.NotificationRepository
import com.bigdatamonitor.data.repository.PermissionRepository
import com.bigdatamonitor.data.repository.UsageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * 数据清理 Worker。
 * 根据用户设定的保留天数，定期清理过期数据。
 * 每日执行一次。
 */
@HiltWorker
class DataCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsDataStore: SettingsDataStore,
    private val clipboardRepository: ClipboardRepository,
    private val notificationRepository: NotificationRepository,
    private val usageRepository: UsageRepository,
    private val networkRepository: NetworkRepository,
    private val permissionRepository: PermissionRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DataCleanup"
    }

    override suspend fun doWork(): Result {
        return try {
            val retentionDays = settingsDataStore.dataRetentionDaysFlow.first()
            if (retentionDays <= 0) {
                Log.d(TAG, "Data retention set to permanent, skipping cleanup")
                return Result.success()
            }

            val cutoff = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)

            clipboardRepository.deleteOlderThan(cutoff)
            notificationRepository.deleteOlderThan(cutoff)
            usageRepository.deleteOlderThan(cutoff)
            networkRepository.deleteOlderThan(cutoff)
            permissionRepository.deleteOlderThan(cutoff)

            Log.i(TAG, "Data cleanup completed: removed entries older than $retentionDays days")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during data cleanup", e)
            Result.success()
        }
    }
}
