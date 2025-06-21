package com.wfit.heart.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wfit.heart.data.HeartRateService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HeartRateViewModel(application: Application) : AndroidViewModel(application) {
    
    private val heartRateService = HeartRateService(application)
    
    private val _uiState = MutableStateFlow(HeartRateUiState())
    val uiState: StateFlow<HeartRateUiState> = _uiState.asStateFlow()
    
    init {
        observeHeartRate()
    }
    
    private fun observeHeartRate() {
        viewModelScope.launch {
            heartRateService.heartRate.collect { heartRate ->
                _uiState.value = _uiState.value.copy(
                    heartRate = heartRate,
                    isMonitoring = heartRateService.isMonitoring.value ?: false
                )
            }
        }
        
        viewModelScope.launch {
            heartRateService.isMonitoring.collect { isMonitoring ->
                _uiState.value = _uiState.value.copy(isMonitoring = isMonitoring)
            }
        }
        
        viewModelScope.launch {
            heartRateService.sensorAvailable.collect { sensorAvailable ->
                _uiState.value = _uiState.value.copy(sensorAvailable = sensorAvailable)
            }
        }
    }
    
    fun startMonitoring() {
        if (_uiState.value.sensorAvailable) {
            heartRateService.startMonitoring()
        }
    }
    
    fun stopMonitoring() {
        heartRateService.stopMonitoring()
    }
    
    fun toggleMonitoring() {
        if (_uiState.value.isMonitoring) {
            stopMonitoring()
        } else {
            startMonitoring()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        heartRateService.stopMonitoring()
    }
}

data class HeartRateUiState(
    val heartRate: Int? = null,
    val isMonitoring: Boolean = false,
    val sensorAvailable: Boolean = false
) 