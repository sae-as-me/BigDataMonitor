package com.bigdatamonitor.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigdatamonitor.data.datastore.SettingsDataStore
import com.bigdatamonitor.service.PrivacyMonitorService
import com.bigdatamonitor.util.ExportManager
import com.bigdatamonitor.util.PermissionUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val dynamicColor: Boolean = true,
    val darkMode: String = "system",
    val clipboardMonitor: Boolean = false,
    val notificationMonitor: Boolean = false,
    val usageStats: Boolean = false,
    val permissionAudit: Boolean = false,
    val networkMonitor: Boolean = false,
    val dataRetentionDays: Int = 30,
    val notificationStorageMode: String = "summary",
    val clipboardStorageMode: String = "summary",
    val accessibilityEnabled: Boolean = false,
    val notificationListenerEnabled: Boolean = false,
    val usageStatsEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val exportManager: ExportManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _exportUri = MutableStateFlow<Uri?>(null)
    val exportUri: StateFlow<Uri?> = _exportUri.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.dynamicColorFlow,
                settingsDataStore.darkModeFlow,
                settingsDataStore.clipboardMonitorEnabledFlow,
                settingsDataStore.notificationMonitorEnabledFlow,
                settingsDataStore.usageStatsEnabledFlow,
                settingsDataStore.permissionAuditEnabledFlow,
                settingsDataStore.networkMonitorEnabledFlow
            ) { values ->
                SettingsUiState(
                    dynamicColor = values[0] as Boolean,
                    darkMode = values[1] as String,
                    clipboardMonitor = values[2] as Boolean,
                    notificationMonitor = values[3] as Boolean,
                    usageStats = values[4] as Boolean,
                    permissionAudit = values[5] as Boolean,
                    networkMonitor = values[6] as Boolean
                )
            }.collect { partial ->
                _uiState.value = _uiState.value.copy(
                    dynamicColor = partial.dynamicColor,
                    darkMode = partial.darkMode,
                    clipboardMonitor = partial.clipboardMonitor,
                    notificationMonitor = partial.notificationMonitor,
                    usageStats = partial.usageStats,
                    permissionAudit = partial.permissionAudit,
                    networkMonitor = partial.networkMonitor
                )
            }
        }
    }

    fun updatePermissionStatus(context: android.content.Context) {
        _uiState.value = _uiState.value.copy(
            accessibilityEnabled = PermissionUtil.isAccessibilityEnabled(context),
            notificationListenerEnabled = PermissionUtil.isNotificationListenerEnabled(context),
            usageStatsEnabled = PermissionUtil.isUsageStatsEnabled(context)
        )
    }

    fun setDynamicColor(value: Boolean) {
        viewModelScope.launch { settingsDataStore.setDynamicColor(value) }
    }

    fun setDarkMode(value: String) {
        viewModelScope.launch { settingsDataStore.setDarkMode(value) }
    }

    fun setClipboardMonitor(enabled: Boolean, context: android.content.Context) {
        viewModelScope.launch {
            settingsDataStore.setClipboardMonitorEnabled(enabled)
            if (enabled) {
                PrivacyMonitorService.start(context)
            } else {
                PrivacyMonitorService.stop(context)
            }
        }
    }

    fun setNotificationMonitor(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setNotificationMonitorEnabled(enabled) }
    }

    fun setUsageStats(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setUsageStatsEnabled(enabled) }
    }

    fun setPermissionAudit(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setPermissionAuditEnabled(enabled) }
    }

    fun setNetworkMonitor(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setNetworkMonitorEnabled(enabled) }
    }

    fun setDataRetentionDays(value: Int) {
        viewModelScope.launch { settingsDataStore.setDataRetentionDays(value) }
    }

    fun setNotificationStorageMode(value: String) {
        viewModelScope.launch { settingsDataStore.setNotificationStorageMode(value) }
    }

    fun setClipboardStorageMode(value: String) {
        viewModelScope.launch { settingsDataStore.setClipboardStorageMode(value) }
    }

    fun exportJson() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val start = now - 30L * 24 * 60 * 60 * 1000
            _exportUri.value = exportManager.exportJson(start, now)
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val start = now - 30L * 24 * 60 * 60 * 1000
            _exportUri.value = exportManager.exportCsv(start, now)
        }
    }

    fun clearExportUri() {
        _exportUri.value = null
    }
}
