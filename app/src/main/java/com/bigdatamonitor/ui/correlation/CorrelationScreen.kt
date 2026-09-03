package com.bigdatamonitor.ui.correlation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bigdatamonitor.data.db.entity.SensitiveEventEntity
import com.bigdatamonitor.domain.model.RelatedEvent
import com.bigdatamonitor.ui.components.ConfidenceBar
import com.bigdatamonitor.ui.components.EmptyState
import com.bigdatamonitor.ui.components.LoadingState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CorrelationScreen(viewModel: CorrelationViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedEventId by remember { mutableStateOf<Long?>(null) }

    if (uiState.isLoading) {
        LoadingState()
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "标注敏感事件")
            }
        }
    ) { padding ->
        if (uiState.sensitiveEvents.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Search,
                title = "尚未标注任何敏感事件",
                subtitle = "点击右下角按钮标注您的隐私事件（如线下对话主题），系统将自动关联分析相关 App 行为",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("已标注事件", style = MaterialTheme.typography.titleMedium)
                }

                items(uiState.sensitiveEvents) { event ->
                    val result = uiState.correlationResults[event.id]
                    val isExpanded = expandedEventId == event.id
                    SensitiveEventCard(
                        event = event,
                        result = result,
                        isExpanded = isExpanded,
                        onToggle = { expandedEventId = if (isExpanded) null else event.id },
                        onReanalyze = { viewModel.reanalyze(event) },
                        onDelete = { viewModel.deleteSensitiveEvent(event) },
                        parseResult = viewModel::parseCorrelationResult
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⚠ 关联为推断分析，非因果证明。系统仅基于时间和关键词匹配推断关联，不能确认 App 确实获取了您的隐私数据。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSensitiveEventDialog(
            onConfirm = { title, desc, keywords, timestamp, scope ->
                viewModel.addSensitiveEvent(title, desc, keywords, timestamp, scope)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun SensitiveEventCard(
    event: SensitiveEventEntity,
    result: com.bigdatamonitor.data.db.entity.CorrelationResultEntity?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onReanalyze: () -> Unit,
    onDelete: () -> Unit,
    parseResult: (String) -> com.bigdatamonitor.domain.model.CorrelationResult?
) {
    val timeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val parsed = if (result != null) parseResult(result.resultJson) else null
    val relatedCount = parsed?.relatedEvents?.size ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onToggle
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = timeFormat.format(Date(event.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "关键词: ${event.keywords}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (relatedCount > 0) {
                    Text(
                        text = "$relatedCount 条关联",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (result != null && parsed != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ConfidenceBar(score = result.confidenceScore)
            }

            if (isExpanded && parsed != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                parsed.relatedEvents.forEach { related ->
                    RelatedEventItem(related)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (parsed.summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = parsed.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    TextButton(onClick = onReanalyze) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重新分析")
                    }
                    TextButton(onClick = onDelete) {
                        Text("删除")
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedEventItem(event: RelatedEvent) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = timeFormat.format(Date(event.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(48.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${event.packageName} - ${event.eventType}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = event.matchReason,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun AddSensitiveEventDialog(
    onConfirm: (title: String, desc: String, keywords: String, timestamp: Long, scope: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var selectedScope by remember { mutableStateOf("offline_conversation") }

    val scopes = listOf(
        "offline_conversation" to "线下对话",
        "online_search" to "线上搜索",
        "clipboard_copy" to "剪贴板复制",
        "custom" to "自定义"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("标注敏感事件") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    placeholder = { Text("如：与朋友谈论爬山") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("关键词（逗号分隔）") },
                    placeholder = { Text("爬山,登山,徒步") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("事件类型", style = MaterialTheme.typography.labelMedium)
                Row {
                    scopes.forEach { (value, label) ->
                        FilterChip(
                            selected = selectedScope == value,
                            onClick = { selectedScope = value },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && keywords.isNotBlank()) {
                        onConfirm(title, description, keywords, System.currentTimeMillis(), selectedScope)
                    }
                },
                enabled = title.isNotBlank() && keywords.isNotBlank()
            ) {
                Text("保存并分析")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
