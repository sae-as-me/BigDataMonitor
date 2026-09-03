package com.bigdatamonitor.worker

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bigdatamonitor.data.db.entity.AppEntity
import com.bigdatamonitor.data.db.entity.PermissionSnapshotEntity
import com.bigdatamonitor.data.repository.PermissionRepository
import com.bigdatamonitor.domain.RiskScorer
import com.bigdatamonitor.domain.model.SensitivePermission
import com.bigdatamonitor.util.AppInfoUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 权限审计 Worker。
 * 扫描所有已安装 App 的权限声明，标记敏感权限持有者。
 * 每日执行一次。
 */
@HiltWorker
class PermissionAuditWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val permissionRepository: PermissionRepository,
    private val riskScorer: RiskScorer
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "PermissionAudit"
    }

    @Serializable
    data class PermissionRecord(val name: String, val isGranted: String, val sensitivity: Int, val category: String)

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        return try {
            val pm = applicationContext.packageManager
            val apps = AppInfoUtil.getAllInstalledApps(applicationContext)
            val now = System.currentTimeMillis()

            for (appInfo in apps) {
                val packageName = appInfo.packageName
                if (packageName == applicationContext.packageName) continue

                val appName = try {
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    packageName
                }

                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val installerPkg = AppInfoUtil.getInstallerPackage(applicationContext, packageName)

                // 获取权限列表
                val perms = try {
                    val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                    pkgInfo.requestedPermissions ?: emptyArray()
                } catch (e: Exception) {
                    emptyArray()
                }

                // 过滤敏感权限
                val sensitivePerms = mutableListOf<PermissionRecord>()
                for (perm in perms) {
                    val sp = SensitivePermission.fromPermission(perm)
                    if (sp != null) {
                        sensitivePerms.add(PermissionRecord(
                            name = perm,
                            isGranted = "true",
                            sensitivity = sp.sensitivityLevel,
                            category = sp.category
                        ))
                    }
                }

                val permissionsJson = json.encodeToString(sensitivePerms)

                // 存储 App 信息
                permissionRepository.upsertApp(AppEntity(
                    packageName = packageName,
                    appName = appName,
                    isSystemApp = isSystemApp,
                    installerPackage = installerPkg,
                    riskScore = 0,
                    riskLevel = "unknown",
                    lastAudited = now
                ))

                // 存储权限快照
                permissionRepository.insertPermissionSnapshot(PermissionSnapshotEntity(
                    timestamp = now,
                    packageName = packageName,
                    permissionsJson = permissionsJson
                ))

                // 计算风险评分
                riskScorer.scoreApp(packageName, appName)
            }

            Log.i(TAG, "Permission audit completed: ${apps.size} apps scanned")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during permission audit", e)
            Result.success()
        }
    }
}
