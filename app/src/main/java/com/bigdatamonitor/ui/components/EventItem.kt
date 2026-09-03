package com.bigdatamonitor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bigdatamonitor.domain.model.EventType
import com.bigdatamonitor.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/** 事件列表项组件 */
@Composable
fun EventItem(
    type: EventType,
    timestamp: Long,
    packageName: String,
    content: String,
    modifier: Modifier = Modifier
) {
    val color = when (type) {
        EventType.CLIPBOARD_CHANGE, EventType.CLIPBOARD_ACCESS -> EventClipboardColor
        EventType.NOTIFICATION -> EventNotificationColor
        EventType.APP_FOREGROUND, EventType.APP_BACKGROUND -> EventUsageColor
        EventType.NETWORK -> EventNetworkColor
        EventType.CORRELATION -> EventCorrelationColor
    }

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 色条指示器
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
        ) {
            Surface(color = color, modifier = Modifier.fillMaxSize()) {}
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
                Text(
                    text = timeFormat.format(Date(timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (content.isNotBlank()) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2
                )
            }
        }
    }
}
