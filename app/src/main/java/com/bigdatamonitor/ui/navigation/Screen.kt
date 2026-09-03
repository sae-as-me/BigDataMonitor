package com.bigdatamonitor.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector

/** 底部导航路由定义 */
sealed class Screen(
    val route: String,
    val label: String,
    val iconVector: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        label = "仪表盘",
        iconVector = Icons.Filled.Shield
    )

    data object Timeline : Screen(
        route = "timeline",
        label = "时间线",
        iconVector = Icons.Filled.Assessment
    )

    data object AppList : Screen(
        route = "applist",
        label = "应用",
        iconVector = Icons.Filled.Apps
    )

    data object Correlation : Screen(
        route = "correlation",
        label = "关联分析",
        iconVector = Icons.AutoMirrored.Filled.List
    )

    data object Settings : Screen(
        route = "settings",
        label = "设置",
        iconVector = Icons.Filled.Settings
    )
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Timeline,
    Screen.AppList,
    Screen.Correlation,
    Screen.Settings
)

/** App 详情页路由 */
const val ROUTE_APP_DETAIL = "app_detail/{packageName}"
fun appDetailRoute(packageName: String) = "app_detail/$packageName"
