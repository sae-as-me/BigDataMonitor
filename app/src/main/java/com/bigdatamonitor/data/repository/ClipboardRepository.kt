package com.bigdatamonitor.data.repository

import com.bigdatamonitor.data.db.dao.ClipboardEventDao
import com.bigdatamonitor.data.db.dao.ClipboardAccessEventDao
import com.bigdatamonitor.data.db.entity.ClipboardAccessEventEntity
import com.bigdatamonitor.data.db.entity.ClipboardEventEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardRepository @Inject constructor(
    private val clipboardEventDao: ClipboardEventDao,
    private val clipboardAccessDao: ClipboardAccessEventDao
) {
    suspend fun insertClipboardEvent(event: ClipboardEventEntity): Long =
        clipboardEventDao.insert(event)

    suspend fun insertClipboardAccessEvent(event: ClipboardAccessEventEntity): Long =
        clipboardAccessDao.insert(event)

    fun getClipboardEvents(start: Long, end: Long): Flow<List<ClipboardEventEntity>> =
        clipboardEventDao.getByTimeRange(start, end)

    suspend fun getClipboardEventsList(start: Long, end: Long): List<ClipboardEventEntity> =
        clipboardEventDao.getByTimeRangeList(start, end)

    suspend fun getClipboardAccessEvents(start: Long, end: Long): List<ClipboardAccessEventEntity> =
        clipboardAccessDao.getByTimeRange(start, end)

    suspend fun countClipboardAccessByPackage(pkg: String, since: Long): Int =
        clipboardAccessDao.countByPackageSince(pkg, since)

    fun countClipboardAccessSince(since: Long): Flow<Int> =
        clipboardAccessDao.countSince(since)

    fun getClipboardEventPage(limit: Int, offset: Int): Flow<List<ClipboardEventEntity>> =
        clipboardEventDao.getPage(limit, offset)

    fun getClipboardAccessPage(limit: Int, offset: Int): Flow<List<ClipboardAccessEventEntity>> =
        clipboardAccessDao.getPage(limit, offset)

    fun getAllClipboardEvents(): Flow<List<ClipboardEventEntity>> =
        clipboardEventDao.getAllFlow()

    suspend fun deleteOlderThan(before: Long) {
        clipboardEventDao.deleteOlderThan(before)
        clipboardAccessDao.deleteOlderThan(before)
    }

    suspend fun deleteAll() {
        clipboardEventDao.deleteAll()
        clipboardAccessDao.deleteAll()
    }
}
