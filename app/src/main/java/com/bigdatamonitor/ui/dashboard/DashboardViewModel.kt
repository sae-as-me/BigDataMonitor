package com.bigdatamonitor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigdatamonitor.data.db.entity.AppEntity
import com.bigdatamonitor.data.db.entity.ClipboardEventEntity
import com.bigdatamonitor.data.db.entity.ClipboardAccessEventEntity
import com.bigdatamonitor.data.db.entity.NotificationEventEntity
import com.bigdatamonitor.data.db.entity.AppUsageEventEntity
import com.bigdatamonitor.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val todayEventCount: Int = 0,
    val clipboardReadCount: Int = 0,
    val relatedNotificationCount: Int = 0,
    val highRiskAppCount: Int = 0,
    val clipboardCount: Int = 0,
    val notificationCount: Int = 0,
    val networkCount: Int = 0,
    val topRiskApps: List<AppEntity> = emptyList(),
    val recentEvents: List<RecentEventItem> = emptyList()
)

data class RecentEventItem(
    val timestamp: Long,
    val type: String,
    val packageName: String,
    val content: String
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val clipboardRepository: ClipboardRepository,
    private val notificationRepository: NotificationRepository,
    private val usageRepository: UsageRepository,
    private val networkRepository: NetworkRepository,
    private val permissionRepository: PermissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            // 获取今日统计数据
            val clipboardReadCount = clipboardRepository.countClipboardAccessSince(todayStart).first()
            val relatedNotifCount = notificationRepository.countMatchedSince(todayStart).first()
            val highRiskCount = permissionRepository.countByRiskLevel("high").first() +
                permissionRepository.countByRiskLevel("critical").first()
            val clipboardCount = clipboardRepository.countClipboardAccessSince(todayStart).first()
            val notificationCount = notificationRepository.countSince(todayStart).first()
            val networkCount = networkRepository.countSince(todayStart).first()
            val topRiskApps = permissionRepository.getTopRiskApps(5).first()

            // 获取最近事件
            val recentEvents = mutableListOf<RecentEventItem>()
            clipboardRepository.getClipboardEvents(todayStart, now).first().take(3).forEach {
                recentEvents.add(RecentEventItem(it.timestamp, "clipboard", it.sourceApp ?: "未知", it.contentPreview))
            }
            notificationRepository.getByTimeRange(todayStart, now).forEach { notif ->
                if (notif.matchedTopicIds?.isNotBlank() == true) {
                    recentEvents.add(RecentEventItem(notif.timestamp, "notification", notif.packageName, "${notif.title} ${notif.text}"))
                }
            }
            recentEvents.sortByDescending { it.timestamp }

            val todayEventCount = clipboardCount + notificationCount + networkCount

            _uiState.value = DashboardUiState(
                isLoading = false,
                todayEventCount = todayEventCount,
                clipboardReadCount = clipboardReadCount,
                relatedNotificationCount = relatedNotifCount,
                highRiskAppCount = highRiskCount,
                clipboardCount = clipboardCount,
                notificationCount = notificationCount,
                networkCount = networkCount,
                topRiskApps = topRiskApps,
                recentEvents = recentEvents.take(5)
            )
        }
    }
}
