package com.wfit.heart.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.LocalTime

data class HeartRateMeasurement(
    val value: Int,
    val timestamp: LocalDateTime
) {
    val time: LocalTime
        get() = timestamp.toLocalTime()
}

class HeartRateHistory(context: Context) {
    private val storage = HeartRateStorage(context)
    
    private val _measurements = MutableStateFlow<List<HeartRateMeasurement>>(emptyList())
    val measurements: StateFlow<List<HeartRateMeasurement>> = _measurements.asStateFlow()

    private val _minValue = MutableStateFlow(60)
    val minValue: StateFlow<Int> = _minValue.asStateFlow()

    private val _maxValue = MutableStateFlow(143)
    val maxValue: StateFlow<Int> = _maxValue.asStateFlow()

    init {
        // Cargar mediciones guardadas al iniciar
        loadSavedMeasurements()
    }

    private fun loadSavedMeasurements() {
        val savedMeasurements = storage.loadMeasurements()
        if (savedMeasurements.isNotEmpty()) {
            _measurements.value = savedMeasurements
            updateMinMax(savedMeasurements)
        }
    }

    fun addMeasurement(value: Int) {
        val measurement = HeartRateMeasurement(value, LocalDateTime.now())
        val currentList = _measurements.value.toMutableList()
        currentList.add(measurement)
        
        _measurements.value = currentList
        storage.saveMeasurements(currentList)
        
        // Actualizar min/max
        updateMinMax(currentList)
    }

    fun getTodayMeasurements(): List<HeartRateMeasurement> {
        val now = LocalDateTime.now()
        return _measurements.value.filter {
            it.timestamp.toLocalDate() == now.toLocalDate()
        }
    }

    private fun updateMinMax(measurements: List<HeartRateMeasurement>) {
        if (measurements.isNotEmpty()) {
            val values = measurements.map { it.value }
            _minValue.value = values.minOrNull() ?: 60
            _maxValue.value = values.maxOrNull() ?: 143
        }
    }
} 