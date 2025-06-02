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
    val isTrackingEnabled: StateFlow<Boolean> = _isTrackingEnabled

    init {
        loadSleepData()
        cleanOldData()
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
        // El rango de monitorización es de 23:00 a 06:00 del día siguiente
        val startDate = today.atTime(23, 0)
        val endDate = today.plusDays(1).atTime(6, 0)

        database.sleepCycleDao()
            .getSleepCyclesBetween(startDate, endDate)
            .map { entities ->
                // Ordenar los ciclos por tiempo de inicio
                val sortedEntities = entities.sortedBy { it.startTime }
                // Log para verificar los ciclos obtenidos
                android.util.Log.d("SleepViewModel", "Ciclos obtenidos de la base de datos:")
                sortedEntities.forEach { entity ->
                    android.util.Log.d("SleepViewModel", "Ciclo: ${entity.phase} de ${entity.startTime} a ${entity.endTime} (${entity.durationMinutes} minutos)")
                }
                groupSleepCyclesByDate(sortedEntities)
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
        if (entities.isEmpty()) return emptyList()

        // Agrupar los ciclos por día, considerando que el ciclo de sueño empieza a las 23:00
        val groupedCycles = mutableMapOf<LocalDate, MutableList<SleepCycleEntity>>()
        
        entities.forEach { entity ->
            // Si el ciclo empieza antes de las 6:00, pertenece al día anterior
            val cycleDate = if (entity.startTime.hour < 6) {
                entity.startTime.toLocalDate().minusDays(1)
            } else {
                entity.startTime.toLocalDate()
            }
            groupedCycles.getOrPut(cycleDate) { mutableListOf() }.add(entity)
        }

        return groupedCycles.map { (date, cycles) ->
            // Log para verificar los ciclos agrupados
            android.util.Log.d("SleepViewModel", "Procesando ciclos para el día $date:")
            cycles.forEach { cycle ->
                android.util.Log.d("SleepViewModel", "Ciclo: ${cycle.phase} de ${cycle.startTime} a ${cycle.endTime} (${cycle.durationMinutes} minutos)")
            }

            DailySleepData(
                date = date.atTime(23, 0), // El ciclo de sueño empieza a las 23:00
                cycles = cycles.map { entity ->
                    SleepCycle(
                        phase = entity.phase,
                        startTime = entity.startTime,
                        endTime = entity.endTime
                    )
                }
            )
        }.sortedByDescending { it.date }
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