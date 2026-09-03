package com.bigdatamonitor.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bigdatamonitor.util.PermissionUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exportUri by viewModel.exportUri.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.updatePermissionStatus(context)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // 导出后弹出分享
    LaunchedEffect(exportUri) {
        exportUri?.let { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "导出数据"))
            viewModel.clearExportUri()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 外观设置
            SettingsSection("外观") {
                ListItem(
                    headlineContent = { Text("主题色跟随系统") },
                    supportingContent = { Text("Android 12+ 动态取色") },
                    trailingContent = {
                        Switch(
                            checked = uiState.dynamicColor,
                            onCheckedChange = viewModel::setDynamicColor
                        )
                    }
                )
                ListItem(
                    headlineContent = { Text("深色模式") },
                    trailingContent = {
                        val options = listOf("system" to "跟随系统", "on" to "开启", "off" to "关闭")
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(options.find { it.first == uiState.darkMode }?.second ?: "跟随系统")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                options.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setDarkMode(value)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // 监控服务
            SettingsSection("监控服务") {
                ServiceToggle(
                    title = "剪贴板监控",
                    enabled = uiState.clipboardMonitor,
                    statusText = if (uiState.clipboardMonitor) "运行中" else "未开启",
                    onToggle = { viewModel.setClipboardMonitor(it, context) }
                )
                ServiceToggle(
                    title = "通知监控",
                    enabled = uiState.notificationMonitor,
                    statusText = if (uiState.notificationListenerEnabled) "已授权" else "需要通知监听权限",
                    onToggle = { viewModel.setNotificationMonitor(it) },
                    onAction = if (!uiState.notificationListenerEnabled) {
                        { PermissionUtil.openNotificationListenerSettings(context) }
                    } else null,
                    actionText = "去开启"
                )
                ServiceToggle(
                    title = "使用统计追踪",
                    enabled = uiState.usageStats,
                    statusText = if (uiState.usageStatsEnabled) "已授权" else "需要使用统计权限",
                    onToggle = { viewModel.setUsageStats(it) },
                    onAction = if (!uiState.usageStatsEnabled) {
                        { PermissionUtil.openUsageStatsSettings(context) }
                    } else null,
                    actionText = "去开启"
                )
                ServiceToggle(
                    title = "权限审计",
                    enabled = uiState.permissionAudit,
                    statusText = "每日自动扫描",
                    onToggle = { viewModel.setPermissionAudit(it) }
                )
                ServiceToggle(
                    title = "网络监控 (VPN)",
                    enabled = uiState.networkMonitor,
                    statusText = "可选模块，耗电较高",
                    onToggle = { viewModel.setNetworkMonitor(it) }
                )
            }

            // 数据存储
            SettingsSection("数据存储") {
                var retentionExpanded by remember { mutableStateOf(false) }
                val retentionOptions = listOf(7 to "7天", 14 to "14天", 30 to "30天", 90 to "90天", 0 to "永久")
                ListItem(
                    headlineContent = { Text("数据保留天数") },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { retentionExpanded = true }) {
                                Text(retentionOptions.find { it.first == uiState.dataRetentionDays }?.second ?: "30天")
                            }
                            DropdownMenu(expanded = retentionExpanded, onDismissRequest = { retentionExpanded = false }) {
                                retentionOptions.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setDataRetentionDays(value)
                                            retentionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text("通知内容存储") },
                    trailingContent = {
                        SegmentedButtonRow(
                            value = uiState.notificationStorageMode,
                            onValueChange = viewModel::setNotificationStorageMode
                        )
                    }
                )
                ListItem(
                    headlineContent = { Text("剪贴板内容存储") },
                    trailingContent = {
                        SegmentedButtonRow(
                            value = uiState.clipboardStorageMode,
                            onValueChange = viewModel::setClipboardStorageMode
                        )
                    }
                )
            }

            // 数据导出
            SettingsSection("数据导出") {
                ListItem(
                    headlineContent = { Text("导出为 JSON") },
                    trailingContent = {
                        TextButton(onClick = viewModel::exportJson) { Text("导出") }
                    }
                )
                ListItem(
                    headlineContent = { Text("导出为 CSV") },
                    trailingContent = {
                        TextButton(onClick = viewModel::exportCsv) { Text("导出") }
                    }
                )
            }

            // 关于
            SettingsSection("关于") {
                ListItem(
                    headlineContent = { Text("版本") },
                    trailingContent = { Text("1.0.0") }
                )
                ListItem(
                    headlineContent = { Text("隐私声明") },
                    supportingContent = { Text("本应用不联网、不上传任何数据。所有监控数据仅存储在本地。") }
                )
                ListItem(
                    headlineContent = { Text("技术限制说明") },
                    supportingContent = { Text("关联分析为推断非因果证明。无法直接监听其他 App 的麦克风行为。") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ServiceToggle(
    title: String,
    enabled: Boolean,
    statusText: String? = null,
    onToggle: (Boolean) -> Unit,
    onAction: (() -> Unit)? = null,
    actionText: String? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = statusText?.let { { Text(it) } },
        trailingContent = {
            if (onAction != null && actionText != null && !enabled) {
                TextButton(onClick = onAction) { Text(actionText) }
            } else {
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedButtonRow(
    value: String,
    onValueChange: (String) -> Unit
) {
    val options = listOf("summary" to "摘要", "full" to "完整")
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, (val_, label) ->
            SegmentedButton(
                selected = value == val_,
                onClick = { onValueChange(val_) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) {
                Text(label)
            }
        }
    }
}
