package com.bigdatamonitor.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigdatamonitor.data.db.entity.*
import com.bigdatamonitor.data.repository.*
import com.bigdatamonitor.domain.model.EventType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimelineItem(
    val id: Long,
    val type: EventType,
    val timestamp: Long,
    val packageName: String,
    val content: String
)

data class TimelineUiState(
    val isLoading: Boolean = true,
    val items: List<TimelineItem> = emptyList(),
    val filter: String = "all"
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val clipboardRepository: ClipboardRepository,
    private val notificationRepository: NotificationRepository,
    private val usageRepository: UsageRepository,
    private val networkRepository: NetworkRepository
) : ViewModel() {

    private val _filter = MutableStateFlow("all")
    val uiState: StateFlow<TimelineUiState> = combine(
        _filter,
        clipboardRepository.getAllClipboardEvents(),
        notificationRepository.getPage(100, 0),
        usageRepository.getPage(100, 0),
        networkRepository.getPage(100, 0)
    ) { filter, clipEvents, notifEvents, usageEvents, netEvents ->
        val allItems = mutableListOf<TimelineItem>()

        clipEvents.forEach {
            allItems.add(TimelineItem(it.id, EventType.CLIPBOARD_CHANGE, it.timestamp, it.sourceApp ?: "未知", it.contentPreview))
        }
        notifEvents.forEach {
            allItems.add(TimelineItem(it.id, EventType.NOTIFICATION, it.timestamp, it.packageName, "${it.title} ${it.text}"))
        }
        usageEvents.forEach {
            val type = if (it.eventType == "foreground") EventType.APP_FOREGROUND else EventType.APP_BACKGROUND
            allItems.add(TimelineItem(it.id, type, it.timestamp, it.packageName, ""))
        }
        netEvents.forEach {
            allItems.add(TimelineItem(it.id, EventType.NETWORK, it.timestamp, it.packageName, "${it.protocol} ${it.targetHost}"))
        }

        val filtered = if (filter == "all") allItems else allItems.filter { it.type.key.startsWith(filter) }
        TimelineUiState(
            isLoading = false,
            items = filtered.sortedByDescending { it.timestamp },
            filter = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimelineUiState())

    fun setFilter(filter: String) {
        _filter.value = filter
    }
}
