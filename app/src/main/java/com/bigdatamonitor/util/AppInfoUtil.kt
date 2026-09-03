package com.bigdatamonitor.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

/** 应用信息工具类 */
object AppInfoUtil {


    /** 获取应用名 */
    fun getAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /** 获取应用图标 */
    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationIcon(info)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /** 是否系统应用 */
    fun isSystemApp(context: Context, packageName: String): Boolean {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** 获取安装来源 */
    fun getInstallerPackage(context: Context, packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 获取所有已安装的非系统应用包名 */
    fun getInstalledPackages(context: Context): List<String> {
        val pm = context.packageManager
        return pm.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { it.packageName }
    }

    /** 获取所有已安装应用（含系统应用） */
    fun getAllInstalledApps(context: Context): List<ApplicationInfo> {
        return context.packageManager.getInstalledApplications(PackageManager.GET_PERMISSIONS)
    }
}
