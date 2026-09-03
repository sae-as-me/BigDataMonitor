package com.bigdatamonitor.ui.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigdatamonitor.data.db.entity.AppEntity
import com.bigdatamonitor.data.db.entity.NotificationEventEntity
import com.bigdatamonitor.data.db.entity.AppUsageEventEntity
import com.bigdatamonitor.data.db.entity.NetworkEventEntity
import com.bigdatamonitor.data.db.entity.PermissionSnapshotEntity
import com.bigdatamonitor.data.repository.PermissionRepository
import com.bigdatamonitor.data.repository.NotificationRepository
import com.bigdatamonitor.data.repository.UsageRepository
import com.bigdatamonitor.data.repository.NetworkRepository
import com.bigdatamonitor.domain.model.RiskLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class AppListUiState(
    val isLoading: Boolean = true,
    val apps: List<AppEntity> = emptyList()
)

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val app: AppEntity? = null,
    val permissions: List<Pair<String, Int>> = emptyList(),
    val recentEvents: List<RecentAppEvent> = emptyList(),
    val networkEvents: List<NetworkEventEntity> = emptyList(),
    val riskBreakdown: Map<String, Int> = emptyMap()
)

data class RecentAppEvent(
    val timestamp: Long,
    val type: String,
    val content: String
)

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val permissionRepository: PermissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            permissionRepository.getAllApps().collect { apps ->
                _uiState.value = AppListUiState(isLoading = false, apps = apps.filter { !it.isSystemApp })
            }
        }
    }
}

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val permissionRepository: PermissionRepository,
    private val notificationRepository: NotificationRepository,
    private val usageRepository: UsageRepository,
    private val networkRepository: NetworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    fun load(packageName: String) {
        viewModelScope.launch {
            val app = permissionRepository.getApp(packageName)
            val snapshot = permissionRepository.getLatestPermissionSnapshot(packageName)

            // 解析权限
            val permissions = parsePermissions(snapshot?.permissionsJson ?: "[]")

            // 获取事件
            val notifEvents = notificationRepository.getByTimeRange(0, System.currentTimeMillis())
                .filter { it.packageName == packageName }
            val usageEvents = usageRepository.getByTimeRange(0, System.currentTimeMillis())
                .filter { it.packageName == packageName }
            val networkEvents = networkRepository.getByTimeRange(0, System.currentTimeMillis())
                .filter { it.packageName == packageName }

            val recentEvents = mutableListOf<RecentAppEvent>()
            notifEvents.forEach { recentEvents.add(RecentAppEvent(it.timestamp, "通知", "${it.title} ${it.text}")) }
            usageEvents.take(20).forEach { recentEvents.add(RecentAppEvent(it.timestamp, it.eventType, "")) }

            _uiState.value = AppDetailUiState(
                isLoading = false,
                app = app,
                permissions = permissions,
                recentEvents = recentEvents.sortedByDescending { it.timestamp }.take(30),
                networkEvents = networkEvents.take(20),
                riskBreakdown = if (app != null) mapOf(
                    "评分" to app.riskScore,
                    "等级" to when (RiskLevel.fromKey(app.riskLevel)) {
                        RiskLevel.LOW -> 1
                        RiskLevel.MEDIUM -> 2
                        RiskLevel.HIGH -> 3
                        RiskLevel.CRITICAL -> 4
                        RiskLevel.UNKNOWN -> 0
                    }
                ) else emptyMap()
            )
        }
    }

    private fun parsePermissions(jsonStr: String): List<Pair<String, Int>> {
        return try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val arr = json.parseToJsonElement(jsonStr).jsonArray
            arr.mapNotNull { el ->
                val obj = el.jsonObject
                val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val sensitivity = obj["sensitivity"]?.jsonPrimitive?.intOrNull ?: 1
                val category = obj["category"]?.jsonPrimitive?.content ?: "other"
                "$category: $name" to sensitivity
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
