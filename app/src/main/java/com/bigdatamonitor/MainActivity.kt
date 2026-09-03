package com.bigdatamonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bigdatamonitor.ui.navigation.MainScreen
import com.bigdatamonitor.ui.MainViewModel
import com.bigdatamonitor.ui.onboarding.OnboardingScreen
import com.bigdatamonitor.ui.theme.BigDataMonitorTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用唯一 Activity，承载所有 Compose 页面。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            BigDataMonitorTheme(
                dynamicColor = uiState.dynamicColor
            ) {
                when {
                    uiState.isLoading -> { /* 空白等待加载完成 */ }
                    !uiState.onboardingCompleted -> {
                        OnboardingScreen(onFinish = viewModel::completeOnboarding)
                    }
                    else -> {
                        MainScreen()
                    }
                }
            }
        }
    }
}
