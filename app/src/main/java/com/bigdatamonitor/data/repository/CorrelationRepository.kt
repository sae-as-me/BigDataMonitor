package com.bigdatamonitor.data.repository

import com.bigdatamonitor.data.db.dao.CorrelationResultDao
import com.bigdatamonitor.data.db.entity.CorrelationResultEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CorrelationRepository @Inject constructor(
    private val correlationDao: CorrelationResultDao
) {
    suspend fun insert(result: CorrelationResultEntity): Long =
        correlationDao.insert(result)

    fun getBySensitiveEvent(eventId: Long): Flow<List<CorrelationResultEntity>> =
        correlationDao.getBySensitiveEvent(eventId)

    suspend fun getLatestBySensitiveEvent(eventId: Long): CorrelationResultEntity? =
        correlationDao.getLatestBySensitiveEvent(eventId)

    suspend fun deleteBySensitiveEvent(eventId: Long) =
        correlationDao.deleteBySensitiveEvent(eventId)

    suspend fun deleteAll() = correlationDao.deleteAll()
}
