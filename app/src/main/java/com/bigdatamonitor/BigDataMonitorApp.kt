package com.bigdatamonitor

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bigdatamonitor.worker.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * BigDataMonitor 应用入口。
 * 初始化 Hilt 依赖注入和 WorkManager。
 */
@HiltAndroidApp
class BigDataMonitorApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        WorkScheduler.schedulePeriodicTasks(this)
    }
}
