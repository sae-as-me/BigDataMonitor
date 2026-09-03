package com.bigdatamonitor.data.repository

import com.bigdatamonitor.data.db.dao.NotificationEventDao
import com.bigdatamonitor.data.db.entity.NotificationEventEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationEventDao
) {
    suspend fun insert(event: NotificationEventEntity): Long =
        notificationDao.insert(event)

    suspend fun getByTimeRange(start: Long, end: Long): List<NotificationEventEntity> =
        notificationDao.getByTimeRange(start, end)

    fun getByPackage(pkg: String): Flow<List<NotificationEventEntity>> =
        notificationDao.getByPackage(pkg)

    fun getMatchedFlow(): Flow<List<NotificationEventEntity>> =
        notificationDao.getMatchedFlow()

    fun countMatchedSince(since: Long): Flow<Int> =
        notificationDao.countMatchedSince(since)

    suspend fun countMatchedByPackageSince(pkg: String, since: Long): Int =
        notificationDao.countMatchedByPackageSince(pkg, since)

    fun getPage(limit: Int, offset: Int): Flow<List<NotificationEventEntity>> =
        notificationDao.getPage(limit, offset)

    fun countSince(since: Long): Flow<Int> =
        notificationDao.countSince(since)

    suspend fun deleteOlderThan(before: Long) =
        notificationDao.deleteOlderThan(before)

    suspend fun deleteAll() = notificationDao.deleteAll()
}
