package com.bigdatamonitor.data.repository

import com.bigdatamonitor.data.db.dao.AppDao
import com.bigdatamonitor.data.db.dao.PermissionSnapshotDao
import com.bigdatamonitor.data.db.entity.AppEntity
import com.bigdatamonitor.data.db.entity.PermissionSnapshotEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepository @Inject constructor(
    private val appDao: AppDao,
    private val permissionDao: PermissionSnapshotDao
) {
    suspend fun upsertApp(app: AppEntity) = appDao.upsert(app)

    suspend fun insertApps(apps: List<AppEntity>) = appDao.insertAll(apps)

    fun getAllApps(): Flow<List<AppEntity>> = appDao.getAllFlow()

    suspend fun getApp(pkg: String): AppEntity? = appDao.getByPackage(pkg)

    fun getTopRiskApps(limit: Int): Flow<List<AppEntity>> = appDao.getTopRiskApps(limit)

    fun countByRiskLevel(level: String): Flow<Int> = appDao.countByRiskLevel(level)

    suspend fun updateRiskScore(pkg: String, score: Int, level: String, timestamp: Long) =
        appDao.updateRiskScore(pkg, score, level, timestamp)

    suspend fun insertPermissionSnapshot(snapshot: PermissionSnapshotEntity): Long =
        permissionDao.insert(snapshot)

    suspend fun getLatestPermissionSnapshot(pkg: String): PermissionSnapshotEntity? =
        permissionDao.getLatestByPackage(pkg)

    suspend fun deleteApp(pkg: String) = appDao.delete(pkg)

    suspend fun deleteOlderThan(before: Long) = permissionDao.deleteOlderThan(before)

    suspend fun deleteAll() {
        appDao.deleteAll()
        permissionDao.deleteAll()
    }
}
