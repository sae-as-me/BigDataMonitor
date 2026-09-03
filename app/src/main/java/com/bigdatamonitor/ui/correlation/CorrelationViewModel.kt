package com.bigdatamonitor.ui.correlation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigdatamonitor.data.db.entity.CorrelationResultEntity
import com.bigdatamonitor.data.db.entity.SensitiveEventEntity
import com.bigdatamonitor.data.repository.CorrelationRepository
import com.bigdatamonitor.data.repository.SensitiveEventRepository
import com.bigdatamonitor.domain.CorrelationEngine
import com.bigdatamonitor.domain.model.CorrelationResult
import com.bigdatamonitor.domain.model.RelatedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class CorrelationUiState(
    val isLoading: Boolean = true,
    val sensitiveEvents: List<SensitiveEventEntity> = emptyList(),
    val correlationResults: Map<Long, CorrelationResultEntity> = emptyMap()
)

@HiltViewModel
class CorrelationViewModel @Inject constructor(
    private val sensitiveEventRepository: SensitiveEventRepository,
    private val correlationRepository: CorrelationRepository,
    private val correlationEngine: CorrelationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(CorrelationUiState())
    val uiState: StateFlow<CorrelationUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            sensitiveEventRepository.getAllFlow().collect { events ->
                val resultsMap = mutableMapOf<Long, CorrelationResultEntity>()
                for (event in events) {
                    val result = correlationRepository.getLatestBySensitiveEvent(event.id)
                    if (result != null) {
                        resultsMap[event.id] = result
                    }
                }
                _uiState.value = CorrelationUiState(
                    isLoading = false,
                    sensitiveEvents = events,
                    correlationResults = resultsMap
                )
            }
        }
    }

    fun addSensitiveEvent(title: String, description: String, keywords: String, timestamp: Long, scope: String) {
        viewModelScope.launch {
            val entity = SensitiveEventEntity(
                timestamp = timestamp,
                title = title,
                description = description,
                keywords = keywords,
                scope = scope
            )
            val id = sensitiveEventRepository.insert(entity)
            // 自动执行关联分析
            val saved = sensitiveEventRepository.getById(id)
            if (saved != null) {
                correlationEngine.analyze(saved)
            }
            loadData()
        }
    }

    fun deleteSensitiveEvent(event: SensitiveEventEntity) {
        viewModelScope.launch {
            sensitiveEventRepository.delete(event)
        }
    }

    fun reanalyze(event: SensitiveEventEntity) {
        viewModelScope.launch {
            correlationEngine.analyze(event)
            loadData()
        }
    }

    fun parseCorrelationResult(jsonStr: String): CorrelationResult? {
        return try {
            json.decodeFromString<CorrelationResult>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }
}
