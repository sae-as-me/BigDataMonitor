package com.bigdatamonitor.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 应用设置 DataStore，管理所有用户偏好。
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val CLIPBOARD_MONITOR_ENABLED = booleanPreferencesKey("clipboard_monitor_enabled")
        val NOTIFICATION_MONITOR_ENABLED = booleanPreferencesKey("notification_monitor_enabled")
        val USAGE_STATS_ENABLED = booleanPreferencesKey("usage_stats_enabled")
        val PERMISSION_AUDIT_ENABLED = booleanPreferencesKey("permission_audit_enabled")
        val NETWORK_MONITOR_ENABLED = booleanPreferencesKey("network_monitor_enabled")
        val DATA_RETENTION_DAYS = intPreferencesKey("data_retention_days")
        val NOTIFICATION_STORAGE_MODE = stringPreferencesKey("notification_storage_mode")
        val CLIPBOARD_STORAGE_MODE = stringPreferencesKey("clipboard_storage_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val dynamicColorFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[DYNAMIC_COLOR] ?: true }

    val darkModeFlow: Flow<String> = context.settingsDataStore.data.map { it[DARK_MODE] ?: "system" }

    val clipboardMonitorEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[CLIPBOARD_MONITOR_ENABLED] ?: false }

    val notificationMonitorEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[NOTIFICATION_MONITOR_ENABLED] ?: false }

    val usageStatsEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[USAGE_STATS_ENABLED] ?: false }

    val permissionAuditEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[PERMISSION_AUDIT_ENABLED] ?: false }

    val networkMonitorEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[NETWORK_MONITOR_ENABLED] ?: false }

    val dataRetentionDaysFlow: Flow<Int> = context.settingsDataStore.data.map { it[DATA_RETENTION_DAYS] ?: 30 }

    val notificationStorageModeFlow: Flow<String> = context.settingsDataStore.data.map { it[NOTIFICATION_STORAGE_MODE] ?: "summary" }

    val clipboardStorageModeFlow: Flow<String> = context.settingsDataStore.data.map { it[CLIPBOARD_STORAGE_MODE] ?: "summary" }

    val onboardingCompletedFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    suspend fun setDynamicColor(value: Boolean) {
        context.settingsDataStore.edit { it[DYNAMIC_COLOR] = value }
    }

    suspend fun setDarkMode(value: String) {
        context.settingsDataStore.edit { it[DARK_MODE] = value }
    }

    suspend fun setClipboardMonitorEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[CLIPBOARD_MONITOR_ENABLED] = value }
    }

    suspend fun setNotificationMonitorEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[NOTIFICATION_MONITOR_ENABLED] = value }
    }

    suspend fun setUsageStatsEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[USAGE_STATS_ENABLED] = value }
    }

    suspend fun setPermissionAuditEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[PERMISSION_AUDIT_ENABLED] = value }
    }

    suspend fun setNetworkMonitorEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[NETWORK_MONITOR_ENABLED] = value }
    }

    suspend fun setDataRetentionDays(value: Int) {
        context.settingsDataStore.edit { it[DATA_RETENTION_DAYS] = value }
    }

    suspend fun setNotificationStorageMode(value: String) {
        context.settingsDataStore.edit { it[NOTIFICATION_STORAGE_MODE] = value }
    }

    suspend fun setClipboardStorageMode(value: String) {
        context.settingsDataStore.edit { it[CLIPBOARD_STORAGE_MODE] = value }
    }

    suspend fun setOnboardingCompleted(value: Boolean) {
        context.settingsDataStore.edit { it[ONBOARDING_COMPLETED] = value }
    }
}
