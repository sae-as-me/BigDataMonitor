package com.bigdatamonitor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 监控服务状态卡片组件 */
@Composable
fun ServiceStatusCard(
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    statusText: String? = null,
    onAction: (() -> Unit)? = null,
    actionText: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (statusText != null) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (onAction != null && actionText != null && !enabled) {
                TextButton(onClick = onAction) {
                    Text(text = actionText)
                }
            } else {
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}
