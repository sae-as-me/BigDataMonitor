package com.bigdatamonitor.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bigdatamonitor.util.PermissionUtil
import kotlinx.coroutines.launch

/**
 * 首次启动引导页面。
 * 3 页：欢迎、权限授权、技术限制说明。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var understandChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> PermissionsPage(context)
                2 -> LimitsPage(
                    checked = understandChecked,
                    onCheckedChange = { understandChecked = it }
                )
            }
        }

        // 底部按钮区
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Text("上一步")
                }
            } else {
                Spacer(modifier = Modifier.width(80.dp))
            }

            // 页面指示器
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    val color = if (index == pagerState.currentPage)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                            .clip(RoundedCornerShape(50))
                    ) {
                        Surface(color = color, modifier = Modifier.fillMaxSize()) {}
                    }
                }
            }

            if (pagerState.currentPage < 2) {
                Button(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) {
                    Text("下一步")
                }
            } else {
                Button(
                    onClick = onFinish,
                    enabled = understandChecked
                ) {
                    Text("开始使用")
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "欢迎使用 BigDataMonitor",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "一款隐私行为感知工具，帮助您了解哪些个人信息被哪些 App 获取并用于个性化推荐。",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "不联网 · 不上传 · 无广告",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun PermissionsPage(context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(
            text = "开启监控权限",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "请依次开启以下权限以启用监控功能。您也可以稍后在设置中开启。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        PermissionItem(
            icon = Icons.Filled.Accessibility,
            title = "无障碍服务",
            description = "检测剪贴板访问行为",
            granted = PermissionUtil.isAccessibilityEnabled(context),
            onAction = { PermissionUtil.openAccessibilitySettings(context) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionItem(
            icon = Icons.Filled.Notifications,
            title = "通知监听",
            description = "捕获推送通知内容",
            granted = PermissionUtil.isNotificationListenerEnabled(context),
            onAction = { PermissionUtil.openNotificationListenerSettings(context) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionItem(
            icon = Icons.Filled.Analytics,
            title = "使用统计",
            description = "追踪应用前后台切换",
            granted = PermissionUtil.isUsageStatsEnabled(context),
            onAction = { PermissionUtil.openUsageStatsSettings(context) }
        )
    }
}

@Composable
private fun LimitsPage(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(
            text = "技术限制说明",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        val limits = listOf(
            "无法直接监听其他 App 的麦克风采集行为",
            "无法截获 App 之间的数据共享（如 SDK 跨 App 追踪）",
            "关联分析是统计推断，非因果证明",
            "剪贴板读取检测在 Android 12+ 效果较好",
            "网络监控为可选项，耗电较高"
        )
        limits.forEach { limit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(text = "•", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = limit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
            Text(
                text = "我已了解上述限制",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) MaterialTheme.colorScheme.primaryContainer
                           else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (granted) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (granted) "已开启" else description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (granted) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.outline
                )
            }
            if (!granted) {
                TextButton(onClick = onAction) {
                    Text("去开启")
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
