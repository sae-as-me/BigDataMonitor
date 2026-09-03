package com.bigdatamonitor.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bigdatamonitor.R
import com.bigdatamonitor.ui.applist.AppDetailScreen
import com.bigdatamonitor.ui.applist.AppListScreen
import com.bigdatamonitor.ui.correlation.CorrelationScreen
import com.bigdatamonitor.ui.dashboard.DashboardScreen
import com.bigdatamonitor.ui.settings.SettingsScreen
import com.bigdatamonitor.ui.timeline.TimelineScreen

/**
 * 主导航容器，包含底部 5 Tab 导航和页面路由。
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // 仅在主 Tab 页面显示底部导航栏
            val showBottomBar = bottomNavScreens.any { it.route == currentRoute }
            if (showBottomBar) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == screen.route
                        } == true
                        NavigationBarItem(
                            icon = { Icon(imageVector = screen.iconVector, contentDescription = null) },
                            label = { Text(text = screen.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }
            composable(Screen.Timeline.route) {
                TimelineScreen()
            }
            composable(Screen.AppList.route) {
                AppListScreen(
                    onAppClick = { pkg -> navController.navigate(appDetailRoute(pkg)) }
                )
            }
            composable(Screen.Correlation.route) {
                CorrelationScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = ROUTE_APP_DETAIL,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType })
            ) { backStackEntry ->
                val pkg = backStackEntry.arguments?.getString("packageName") ?: ""
                AppDetailScreen(
                    packageName = pkg,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/** 主入口 Composable */
@Composable
fun MainScreen() {
    AppNavHost()
}
