package com.wfit.heart.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HeartRateService(private val context: Context) : SensorEventListener, LocationListener {
    
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private val _sensorAvailable = MutableStateFlow(false)
    val sensorAvailable: StateFlow<Boolean> = _sensorAvailable.asStateFlow()
    
    private var heartRateSensor: Sensor? = null
    private var lastHeartRateUpdate = 0L
    
    val history = HeartRateHistory(context)
    
    init {
        setupSensors()
    }
    
    private fun setupSensors() {
        // Buscar sensor de ritmo cardíaco real
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        
        // Verificar si el sensor está disponible
        _sensorAvailable.value = heartRateSensor != null
        
        if (heartRateSensor == null) {
            // Si no hay sensor de ritmo cardíaco, buscar otros sensores relacionados
            val availableSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
            for (sensor in availableSensors) {
                if (sensor.name.contains("heart", ignoreCase = true) || 
                    sensor.name.contains("cardiac", ignoreCase = true) ||
                    sensor.vendor.contains("heart", ignoreCase = true)) {
                    heartRateSensor = sensor
                    _sensorAvailable.value = true
                    break
                }
            }
        }
    }
    
    fun startMonitoring() {
        if (_isMonitoring.value) return
        
        if (!_sensorAvailable.value) {
            // No hay sensor disponible, no podemos monitorear
            return
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            heartRateSensor?.let { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                _isMonitoring.value = true
            }
        }
        
        // También registrar listener de ubicación si está disponible (necesario para algunos sensores)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                this
            )
        }
    }
    
    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
        _isMonitoring.value = false
        history.clear()
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_HEART_RATE -> {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastHeartRateUpdate > 1000) { // Actualizar máximo cada segundo
                        val heartRateValue = it.values[0].toInt()
                        if (heartRateValue > 0 && heartRateValue < 300) { // Valores válidos
                            _heartRate.value = heartRateValue
                            history.addMeasurement(heartRateValue)
                            lastHeartRateUpdate = currentTime
                        }
                    }
                }
                else -> {
                    // Para otros sensores que puedan ser de ritmo cardíaco
                    if (it.sensor.name.contains("heart", ignoreCase = true) || 
                        it.sensor.name.contains("cardiac", ignoreCase = true)) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastHeartRateUpdate > 1000) {
                            val heartRateValue = it.values[0].toInt()
                            if (heartRateValue > 0 && heartRateValue < 300) {
                                _heartRate.value = heartRateValue
                                history.addMeasurement(heartRateValue)
                                lastHeartRateUpdate = currentTime
                            }
                        }
                    }
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No necesitamos manejar cambios de precisión para este caso
    }
    
    override fun onLocationChanged(location: Location) {
        // Necesario para algunos sensores de ritmo cardíaco, pero no usamos la ubicación
    }
    
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
} 