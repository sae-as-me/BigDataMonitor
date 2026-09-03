package com.bigdatamonitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bigdatamonitor.domain.model.RiskLevel
import com.bigdatamonitor.ui.theme.*

/** 风险等级徽章组件 */
@Composable
fun RiskBadge(
    score: Int,
    level: String,
    modifier: Modifier = Modifier
) {
    val color = when (RiskLevel.fromKey(level)) {
        RiskLevel.LOW -> RiskLowColor
        RiskLevel.MEDIUM -> RiskMediumColor
        RiskLevel.HIGH -> RiskHighColor
        RiskLevel.CRITICAL -> RiskCriticalColor
        RiskLevel.UNKNOWN -> MaterialTheme.colorScheme.outline
    }

    val label = when (RiskLevel.fromKey(level)) {
        RiskLevel.LOW -> "低风险"
        RiskLevel.MEDIUM -> "中风险"
        RiskLevel.HIGH -> "高风险"
        RiskLevel.CRITICAL -> "极高风险"
        RiskLevel.UNKNOWN -> "未知"
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$score · $label",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 12.sp
        )
    }
}
