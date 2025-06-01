package com.example.wfit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wfit.data.db.SleepDatabase
import com.example.wfit.data.db.entity.SleepCycleEntity
import com.example.wfit.presentation.model.DailySleepData
import com.example.wfit.presentation.model.SleepCycle
import com.example.wfit.presentation.model.SleepPhase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SleepViewModel(private val database: SleepDatabase) : ViewModel() {
    private val _sleepData = MutableStateFlow<List<DailySleepData>>(emptyList())
    val sleepData: StateFlow<List<DailySleepData>> = _sleepData.asStateFlow()

    init {
        loadSleepData()
        cleanOldData()
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

    class Factory(private val database: SleepDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SleepViewModel::class.java)) {
                return SleepViewModel(database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 