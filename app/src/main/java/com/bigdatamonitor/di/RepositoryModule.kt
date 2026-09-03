package com.bigdatamonitor.di

import android.content.Context
import com.bigdatamonitor.data.datastore.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore =
        SettingsDataStore(context)

    // 其余 Repository（ClipboardRepository、NotificationRepository、UsageRepository、
    // PermissionRepository、NetworkRepository、SensitiveEventRepository、CorrelationRepository）
    // 均已标注 @Singleton + @Inject constructor，Hilt 可自动绑定，无需在此重复 @Provides。
}
