package com.bigdatamonitor.ui.applist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bigdatamonitor.ui.components.LoadingState
import com.bigdatamonitor.ui.components.RiskBadge
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    onBack: () -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(packageName) {
        viewModel.load(packageName)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.app?.appName ?: packageName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 基本信息
            item {
                val app = uiState.app
                if (app != null) {
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("隐私风险评分", style = MaterialTheme.typography.titleMedium)
                                RiskBadge(score = app.riskScore, level = app.riskLevel)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("包名: ${app.packageName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            if (app.installerPackage != null) {
                                Text("安装来源: ${app.installerPackage}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }

            // 敏感权限
            item {
                if (uiState.permissions.isNotEmpty()) {
                    Text("敏感权限", style = MaterialTheme.typography.titleMedium)
                    uiState.permissions.forEach { (perm, sensitivity) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(perm, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text("Lv$sensitivity", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 事件记录
            item {
                if (uiState.recentEvents.isNotEmpty()) {
                    Text("事件记录", style = MaterialTheme.typography.titleMedium)
                }
            }
            items(uiState.recentEvents.size) { index ->
                val event = uiState.recentEvents[index]
                val time = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(event.timestamp))
                Text(
                    text = "$time  ${event.type}  ${event.content}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // 网络连接
            item {
                if (uiState.networkEvents.isNotEmpty()) {
                    Text("网络连接", style = MaterialTheme.typography.titleMedium)
                    uiState.networkEvents.forEach { event ->
                        val trackerMark = if (event.isTrackerDomain) " [追踪]" else ""
                        Text(
                            text = "${event.protocol} ${event.targetHost}$trackerMark",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (event.isTrackerDomain) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
