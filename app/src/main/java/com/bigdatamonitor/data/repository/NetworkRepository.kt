package com.bigdatamonitor.data.repository

import com.bigdatamonitor.data.db.dao.NetworkEventDao
import com.bigdatamonitor.data.db.entity.NetworkEventEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepository @Inject constructor(
    private val networkDao: NetworkEventDao
) {
    suspend fun insert(event: NetworkEventEntity): Long =
        networkDao.insert(event)

    suspend fun insertAll(events: List<NetworkEventEntity>) =
        networkDao.insertAll(events)

    suspend fun getByTimeRange(start: Long, end: Long): List<NetworkEventEntity> =
        networkDao.getByTimeRange(start, end)

    fun getByPackage(pkg: String): Flow<List<NetworkEventEntity>> =
        networkDao.getByPackage(pkg)

    suspend fun countTrackerDomainsByPackageSince(pkg: String, since: Long): Int =
        networkDao.countTrackerDomainsByPackageSince(pkg, since)

    fun countTrackerDomainsSince(since: Long): Flow<Int> =
        networkDao.countTrackerDomainsSince(since)

    fun getPage(limit: Int, offset: Int): Flow<List<NetworkEventEntity>> =
        networkDao.getPage(limit, offset)

    fun countSince(since: Long): Flow<Int> =
        networkDao.countSince(since)

    suspend fun deleteOlderThan(before: Long) =
        networkDao.deleteOlderThan(before)

    suspend fun deleteAll() = networkDao.deleteAll()
}
