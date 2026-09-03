package com.bigdatamonitor.data.repository

import com.bigdatamonitor.data.db.dao.SensitiveEventDao
import com.bigdatamonitor.data.db.entity.SensitiveEventEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensitiveEventRepository @Inject constructor(
    private val sensitiveEventDao: SensitiveEventDao
) {
    suspend fun insert(event: SensitiveEventEntity): Long =
        sensitiveEventDao.insert(event)

    suspend fun delete(event: SensitiveEventEntity) =
        sensitiveEventDao.delete(event)

    fun getAllFlow(): Flow<List<SensitiveEventEntity>> =
        sensitiveEventDao.getAllFlow()

    suspend fun getById(id: Long): SensitiveEventEntity? =
        sensitiveEventDao.getById(id)

    suspend fun getAll(): List<SensitiveEventEntity> =
        sensitiveEventDao.getAll()

    suspend fun deleteById(id: Long) = sensitiveEventDao.deleteById(id)

    suspend fun deleteAll() = sensitiveEventDao.deleteAll()
}
