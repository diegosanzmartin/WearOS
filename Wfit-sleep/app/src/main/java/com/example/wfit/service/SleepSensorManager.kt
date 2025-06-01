package com.example.wfit.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

class SleepSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private val _movementDetected = MutableStateFlow(false)
    val movementDetected: StateFlow<Boolean> = _movementDetected
    
    private val _heartRate = MutableStateFlow(0f)
    val heartRate: StateFlow<Float> = _heartRate
    
    private var lastAcceleration = floatArrayOf(0f, 0f, 0f)
    private var lastMovementTime = Instant.now()
    private val movementThreshold = 0.8f
    
    fun startMonitoring() {
        try {
            heartRateSensor?.let { sensor ->
                sensorManager.registerListener(
                    this,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
            
            accelerometer?.let { sensor ->
                sensorManager.registerListener(
                    this,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting sensors", e)
        }
    }
    
    fun stopMonitoring() {
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sensors", e)
        }
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> processAccelerometerData(event.values)
            Sensor.TYPE_HEART_RATE -> processHeartRateData(event.values[0])
        }
    }
    
    private fun processAccelerometerData(values: FloatArray) {
        val deltaX = abs(values[0] - lastAcceleration[0])
        val deltaY = abs(values[1] - lastAcceleration[1])
        val deltaZ = abs(values[2] - lastAcceleration[2])
        
        lastAcceleration = values.clone()
        
        val movement = (deltaX + deltaY + deltaZ) > movementThreshold
        if (movement) {
            _movementDetected.value = true
            lastMovementTime = Instant.now()
        } else {
            _movementDetected.value = false
        }
    }
    
    private fun processHeartRateData(heartRate: Float) {
        if (heartRate > 0) {
            _heartRate.value = heartRate
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No necesitamos implementar esto por ahora
    }
    
    fun getTimeSinceLastMovement(): Duration {
        return Duration.between(lastMovementTime, Instant.now())
    }
    
    companion object {
        private const val TAG = "SleepSensorManager"
    }
} 