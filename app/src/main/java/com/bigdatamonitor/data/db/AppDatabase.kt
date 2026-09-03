package com.bigdatamonitor.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bigdatamonitor.data.db.dao.*
import com.bigdatamonitor.data.db.entity.*

/**
 * BigDataMonitor Room 数据库。
 * 包含所有实体表和 DAO 接口。
 */
@Database(
    entities = [
        AppEntity::class,
        ClipboardEventEntity::class,
        ClipboardAccessEventEntity::class,
        NotificationEventEntity::class,
        AppUsageEventEntity::class,
        PermissionSnapshotEntity::class,
        NetworkEventEntity::class,
        SensitiveEventEntity::class,
        CorrelationResultEntity::class,
        AppConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun clipboardEventDao(): ClipboardEventDao
    abstract fun clipboardAccessEventDao(): ClipboardAccessEventDao
    abstract fun notificationEventDao(): NotificationEventDao
    abstract fun appUsageEventDao(): AppUsageEventDao
    abstract fun permissionSnapshotDao(): PermissionSnapshotDao
    abstract fun networkEventDao(): NetworkEventDao
    abstract fun sensitiveEventDao(): SensitiveEventDao
    abstract fun correlationResultDao(): CorrelationResultDao
    abstract fun appConfigDao(): AppConfigDao
}
