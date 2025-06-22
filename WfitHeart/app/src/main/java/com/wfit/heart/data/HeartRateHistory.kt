package com.wfit.heart.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class HeartRateMeasurement(
    val value: Int,
    val timestamp: LocalDateTime
) {
    val time: LocalTime
        get() = timestamp.toLocalTime()
        
    val hour: Int
        get() = timestamp.hour
}

data class HourlyAverage(
    val hour: Int,
    val averageValue: Int
)

class HeartRateHistory(context: Context) {
    private val storage = HeartRateStorage(context)
    
    private val _measurements = MutableStateFlow<List<HeartRateMeasurement>>(emptyList())
    val measurements: StateFlow<List<HeartRateMeasurement>> = _measurements.asStateFlow()
    
    private val _hourlyAverages = MutableStateFlow<List<HourlyAverage>>(emptyList())
    val hourlyAverages: StateFlow<List<HourlyAverage>> = _hourlyAverages.asStateFlow()

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
            updateHourlyAverages()
        }
    }

    fun addMeasurement(value: Int) {
        val measurement = HeartRateMeasurement(value, LocalDateTime.now())
        val currentList = _measurements.value.toMutableList()
        
        // Mantener solo las mediciones de las últimas 24 horas
        val twentyFourHoursAgo = LocalDateTime.now().minus(24, ChronoUnit.HOURS)
        currentList.removeAll { it.timestamp.isBefore(twentyFourHoursAgo) }
        
        currentList.add(measurement)
        _measurements.value = currentList
        storage.saveMeasurements(currentList)
        
        updateMinMax(currentList)
        updateHourlyAverages()
    }

    private fun updateHourlyAverages() {
        val todayMeasurements = getTodayMeasurements()
        val hourlyAverages = todayMeasurements
            .groupBy { it.hour }
            .map { (hour, measurements) ->
                HourlyAverage(
                    hour = hour,
                    averageValue = measurements.map { it.value }.average().toInt()
                )
            }
            .sortedBy { it.hour }
        
        _hourlyAverages.value = hourlyAverages
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