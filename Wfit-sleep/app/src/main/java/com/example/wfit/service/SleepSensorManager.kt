package com.example.wfit.service

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.SleepStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant

class SleepSensorManager(context: Context) {
    private val healthClient = HealthServices.getClient(context)
    private val dataClient = healthClient.dataClient
    
    private val _movementDetected = MutableStateFlow(false)
    val movementDetected: StateFlow<Boolean> = _movementDetected
    
    private val _heartRate = MutableStateFlow(0f)
    val heartRate: StateFlow<Float> = _heartRate
    
    private var lastMovementTime = Instant.now()
    private val movementThreshold = 0.8f
    
    suspend fun startMonitoring() {
        try {
            // Registrar para datos de ritmo cardíaco
            val heartRateFlow = dataClient.register(DataType.HEART_RATE_BPM)
            
            // Registrar para datos de movimiento
            val motionFlow = dataClient.register(DataType.SLEEP_SEGMENT)
            
            // Procesar datos de ritmo cardíaco
            heartRateFlow.collect { record ->
                _heartRate.value = record.value
            }
            
            // Procesar datos de movimiento/sueño
            motionFlow.collect { record ->
                when (record.value) {
                    SleepStage.SLEEPING -> {
                        _movementDetected.value = false
                        lastMovementTime = Instant.now()
                    }
                    SleepStage.AWAKE -> {
                        _movementDetected.value = true
                        lastMovementTime = Instant.now()
                    }
                    else -> {
                        // Mantener el estado actual
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting sleep monitoring", e)
        }
    }
    
    suspend fun stopMonitoring() {
        try {
            dataClient.clearAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sleep monitoring", e)
        }
    }
    
    fun getTimeSinceLastMovement(): Duration {
        return Duration.between(lastMovementTime, Instant.now())
    }
    
    companion object {
        private const val TAG = "SleepSensorManager"
    }
} 