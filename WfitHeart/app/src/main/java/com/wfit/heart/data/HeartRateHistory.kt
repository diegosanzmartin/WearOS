package com.wfit.heart.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

data class HeartRateMeasurement(
    val value: Int,
    val time: LocalTime
)

class HeartRateHistory {
    private val _measurements = MutableStateFlow<List<HeartRateMeasurement>>(emptyList())
    val measurements: StateFlow<List<HeartRateMeasurement>> = _measurements.asStateFlow()

    private val _minValue = MutableStateFlow(60)
    val minValue: StateFlow<Int> = _minValue.asStateFlow()

    private val _maxValue = MutableStateFlow(143)
    val maxValue: StateFlow<Int> = _maxValue.asStateFlow()

    fun addMeasurement(value: Int) {
        val measurement = HeartRateMeasurement(value, LocalTime.now())
        val currentList = _measurements.value.toMutableList()
        
        // Mantener solo las últimas 24 mediciones
        if (currentList.size >= 24) {
            currentList.removeAt(0)
        }
        currentList.add(measurement)
        
        _measurements.value = currentList
        
        // Actualizar min/max
        updateMinMax(currentList)
    }

    private fun updateMinMax(measurements: List<HeartRateMeasurement>) {
        if (measurements.isNotEmpty()) {
            val values = measurements.map { it.value }
            _minValue.value = values.minOrNull() ?: 60
            _maxValue.value = values.maxOrNull() ?: 143
        }
    }

    fun clear() {
        _measurements.value = emptyList()
        _minValue.value = 60
        _maxValue.value = 143
    }
} 