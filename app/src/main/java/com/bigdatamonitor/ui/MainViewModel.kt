package com.bigdatamonitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigdatamonitor.data.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainActivity 级别 ViewModel，管理启动引导状态和主题配置加载。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState(isLoading = true))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val dynamicColor = settingsDataStore.dynamicColorFlow.first()
            val onboardingCompleted = settingsDataStore.onboardingCompletedFlow.first()
            _uiState.value = MainUiState(
                isLoading = false,
                dynamicColor = dynamicColor,
                onboardingCompleted = onboardingCompleted
            )
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingCompleted(true)
            _uiState.value = _uiState.value.copy(onboardingCompleted = true)
        }
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val dynamicColor: Boolean = true,
    val onboardingCompleted: Boolean = false
)
