package com.example.wfit.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val movementThreshold = 0.8f // Ajustable según sensibilidad deseada
    
    fun startMonitoring() {
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            this,
            heartRateSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }
    
    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
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
        
        // Detecta movimiento significativo
        val movement = (deltaX + deltaY + deltaZ) > movementThreshold
        _movementDetected.value = movement
    }
    
    private fun processHeartRateData(heartRate: Float) {
        _heartRate.value = heartRate
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No necesitamos implementar esto por ahora
    }
} 