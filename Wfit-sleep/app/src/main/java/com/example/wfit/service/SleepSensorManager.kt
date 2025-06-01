package com.example.wfit.service

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.HeartRateAccuracy
import androidx.health.services.client.data.HeartRateRecord
import androidx.health.services.client.data.SleepSessionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration
import java.time.Instant

class SleepSensorManager(context: Context) {
    private val healthClient = HealthServices.getClient(context)
    private val measureClient = healthClient.measureClient
    
    private val _movementDetected = MutableStateFlow(false)
    val movementDetected: StateFlow<Boolean> = _movementDetected
    
    private val _heartRate = MutableStateFlow(0f)
    val heartRate: StateFlow<Float> = _heartRate
    
    private var lastMovementTime = Instant.now()
    private val movementThreshold = 0.8f
    
    suspend fun startMonitoring() {
        try {
            // Registrar para datos de ritmo cardíaco
            measureClient.registerCallback(DataType.HEART_RATE_BPM) { data ->
                processHeartRateData(data)
            }
            
            // Registrar para datos de sueño
            measureClient.registerCallback(DataType.SLEEP_SESSION_RECORD) { data ->
                processSleepData(data)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting sleep monitoring", e)
        }
    }
    
    private fun processHeartRateData(data: DataPointContainer) {
        data.getData(DataType.HEART_RATE_BPM).firstOrNull()?.let { record ->
            if (record is HeartRateRecord && record.accuracy != HeartRateAccuracy.NO_CONTACT) {
                _heartRate.value = record.bpm.toFloat()
            }
        }
    }
    
    private fun processSleepData(data: DataPointContainer) {
        data.getData(DataType.SLEEP_SESSION_RECORD).firstOrNull()?.let { record ->
            if (record is SleepSessionRecord) {
                when (record.stage) {
                    SleepSessionRecord.Stage.SLEEPING -> {
                        _movementDetected.value = false
                        lastMovementTime = Instant.now()
                    }
                    SleepSessionRecord.Stage.AWAKE -> {
                        _movementDetected.value = true
                        lastMovementTime = Instant.now()
                    }
                    else -> {
                        // Mantener el estado actual
                    }
                }
            }
        }
    }
    
    suspend fun stopMonitoring() {
        try {
            measureClient.unregisterAllCallbacks()
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