package com.bigdatamonitor.ui.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bigdatamonitor.ui.components.EmptyState
import com.bigdatamonitor.ui.components.EventItem

private val filters = listOf("all" to "全部", "clipboard" to "剪贴板", "notification" to "通知", "app" to "使用", "network" to "网络")

@Composable
fun TimelineScreen(viewModel: TimelineViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // 筛选 Chip 行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { (key, label) ->
                FilterChip(
                    selected = uiState.filter == key,
                    onClick = { viewModel.setFilter(key) },
                    label = { Text(label) }
                )
            }
        }

        if (uiState.items.isEmpty() && !uiState.isLoading) {
            EmptyState(
                icon = Icons.Filled.Inbox,
                title = "暂无监控事件",
                subtitle = "请确保已开启监控服务"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(uiState.items) { item ->
                    EventItem(
                        type = item.type,
                        timestamp = item.timestamp,
                        packageName = item.packageName,
                        content = item.content
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
