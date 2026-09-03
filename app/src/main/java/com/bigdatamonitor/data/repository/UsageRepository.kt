package com.bigdatamonitor.data.repository

import com.bigdatamonitor.data.db.dao.AppUsageEventDao
import com.bigdatamonitor.data.db.entity.AppUsageEventEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    private val usageDao: AppUsageEventDao
) {
    suspend fun insert(event: AppUsageEventEntity): Long =
        usageDao.insert(event)

    suspend fun insertAll(events: List<AppUsageEventEntity>) =
        usageDao.insertAll(events)

    suspend fun getByTimeRange(start: Long, end: Long): List<AppUsageEventEntity> =
        usageDao.getByTimeRange(start, end)

    fun getByPackage(pkg: String): Flow<List<AppUsageEventEntity>> =
        usageDao.getByPackage(pkg)

    fun getPage(limit: Int, offset: Int): Flow<List<AppUsageEventEntity>> =
        usageDao.getPage(limit, offset)

    suspend fun countBackgroundByPackageSince(pkg: String, since: Long): Int =
        usageDao.countBackgroundByPackageSince(pkg, since)

    fun getAllFlow(): Flow<List<AppUsageEventEntity>> =
        usageDao.getAllFlow()

    suspend fun deleteOlderThan(before: Long) =
        usageDao.deleteOlderThan(before)

    suspend fun deleteAll() = usageDao.deleteAll()
}
