package com.bigdatamonitor.di

import android.content.Context
import androidx.room.Room
import com.bigdatamonitor.data.db.AppDatabase
import com.bigdatamonitor.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "bigdatamonitor.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideAppDao(db: AppDatabase): AppDao = db.appDao()

    @Provides
    fun provideClipboardEventDao(db: AppDatabase): ClipboardEventDao = db.clipboardEventDao()

    @Provides
    fun provideClipboardAccessEventDao(db: AppDatabase): ClipboardAccessEventDao = db.clipboardAccessEventDao()

    @Provides
    fun provideNotificationEventDao(db: AppDatabase): NotificationEventDao = db.notificationEventDao()

    @Provides
    fun provideAppUsageEventDao(db: AppDatabase): AppUsageEventDao = db.appUsageEventDao()

    @Provides
    fun providePermissionSnapshotDao(db: AppDatabase): PermissionSnapshotDao = db.permissionSnapshotDao()

    @Provides
    fun provideNetworkEventDao(db: AppDatabase): NetworkEventDao = db.networkEventDao()

    @Provides
    fun provideSensitiveEventDao(db: AppDatabase): SensitiveEventDao = db.sensitiveEventDao()

    @Provides
    fun provideCorrelationResultDao(db: AppDatabase): CorrelationResultDao = db.correlationResultDao()

    @Provides
    fun provideAppConfigDao(db: AppDatabase): AppConfigDao = db.appConfigDao()
}
