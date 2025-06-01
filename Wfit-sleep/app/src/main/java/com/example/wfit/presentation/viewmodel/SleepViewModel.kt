package com.example.wfit.presentation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wfit.data.db.SleepDatabase
import com.example.wfit.data.db.entity.SleepCycleEntity
import com.example.wfit.presentation.model.DailySleepData
import com.example.wfit.presentation.model.SleepCycle
import com.example.wfit.presentation.model.SleepPhase
import com.example.wfit.service.SleepMonitoringService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SleepViewModel(
    private val database: SleepDatabase,
    private val context: Context
) : ViewModel() {
    private val _sleepData = MutableStateFlow<List<DailySleepData>>(emptyList())
    val sleepData: StateFlow<List<DailySleepData>> = _sleepData.asStateFlow()

    private val _isTrackingEnabled = MutableStateFlow(true)
    val isTrackingEnabled: StateFlow<Boolean> = _isTrackingEnabled.asStateFlow()

    init {
        loadSleepData()
        cleanOldData()
        
        // Add test data for today
        viewModelScope.launch {
            val today = LocalDate.now()
            val startTime = today.atTime(23, 0) // 11:00 PM
            val cycles = listOf(
                SleepCycleEntity(
                    phase = SleepPhase.AWAKE,
                    startTime = startTime,
                    endTime = startTime.plusMinutes(30)
                ),
                SleepCycleEntity(
                    phase = SleepPhase.LIGHT_SLEEP,
                    startTime = startTime.plusMinutes(30),
                    endTime = startTime.plusMinutes(90)
                ),
                SleepCycleEntity(
                    phase = SleepPhase.DEEP_SLEEP,
                    startTime = startTime.plusMinutes(90),
                    endTime = startTime.plusMinutes(150)
                ),
                SleepCycleEntity(
                    phase = SleepPhase.REM,
                    startTime = startTime.plusMinutes(150),
                    endTime = startTime.plusMinutes(180)
                ),
                SleepCycleEntity(
                    phase = SleepPhase.LIGHT_SLEEP,
                    startTime = startTime.plusMinutes(180),
                    endTime = startTime.plusMinutes(240)
                ),
                SleepCycleEntity(
                    phase = SleepPhase.DEEP_SLEEP,
                    startTime = startTime.plusMinutes(240),
                    endTime = startTime.plusMinutes(300)
                ),
                SleepCycleEntity(
                    phase = SleepPhase.REM,
                    startTime = startTime.plusMinutes(300),
                    endTime = startTime.plusMinutes(330)
                ),
                SleepCycleEntity(
                    phase = SleepPhase.LIGHT_SLEEP,
                    startTime = startTime.plusMinutes(330),
                    endTime = startTime.plusMinutes(390)
                ),
                SleepCycleEntity(
                    phase = SleepPhase.AWAKE,
                    startTime = startTime.plusMinutes(390),
                    endTime = startTime.plusMinutes(420)
                )
            )
            database.sleepCycleDao().insertAll(cycles)
        }
    }

    fun toggleTracking() {
        _isTrackingEnabled.value = !_isTrackingEnabled.value
        val intent = Intent(context, SleepMonitoringService::class.java).apply {
            action = SleepMonitoringService.ACTION_SET_TRACKING_STATE
            putExtra(SleepMonitoringService.EXTRA_TRACKING_STATE, _isTrackingEnabled.value)
        }
        context.startForegroundService(intent)
    }

    private fun loadSleepData() {
        val today = LocalDate.now()
        val startDate = today.minusDays(2).atStartOfDay()
        val endDate = today.plusDays(1).atStartOfDay()

        database.sleepCycleDao()
            .getSleepCyclesBetween(startDate, endDate)
            .map { entities ->
                groupSleepCyclesByDate(entities)
            }
            .onEach { dailyData ->
                _sleepData.value = dailyData
            }
            .launchIn(viewModelScope)
    }

    private fun cleanOldData() {
        viewModelScope.launch {
            // Mantener solo los datos de los últimos 7 días
            val cutoffDate = LocalDate.now().minusDays(7).atStartOfDay()
            database.sleepCycleDao().deleteSleepCyclesBefore(cutoffDate)
        }
    }

    private fun groupSleepCyclesByDate(entities: List<SleepCycleEntity>): List<DailySleepData> {
        return entities
            .groupBy { it.startTime.toLocalDate() }
            .map { (date, cycles) ->
                DailySleepData(
                    date = date.atStartOfDay(),
                    cycles = cycles.map { entity ->
                        SleepCycle(
                            phase = entity.phase,
                            startTime = entity.startTime,
                            endTime = entity.endTime
                        )
                    }
                )
            }
            .sortedByDescending { it.date }
    }

    class Factory(
        private val database: SleepDatabase,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SleepViewModel::class.java)) {
                return SleepViewModel(database, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 