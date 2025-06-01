package com.example.wfit.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.wfit.R
import com.example.wfit.data.db.SleepDatabase
import com.example.wfit.data.db.entity.SleepCycleEntity
import com.example.wfit.presentation.model.SleepPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs

class SleepMonitoringService : LifecycleService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: SleepDatabase
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var sensorManager: SleepSensorManager
    
    private var isMonitoring = false
    private var lastMovementTime: LocalDateTime = LocalDateTime.now()
    private var currentPhase: SleepPhase = SleepPhase.AWAKE
    private var lastPhaseChange: LocalDateTime = LocalDateTime.now()
    private var isTrackingEnabled = true
    
    private var movementCounter = 0
    private var heartRateSum = 0f
    private var heartRateReadings = 0
    
    override fun onCreate() {
        super.onCreate()
        database = SleepDatabase.getDatabase(this)
        sensorManager = SleepSensorManager(this)
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
            ACTION_SET_TRACKING_STATE -> {
                isTrackingEnabled = intent.getBooleanExtra(EXTRA_TRACKING_STATE, true)
                if (!isTrackingEnabled) {
                    stopMonitoring()
                } else if (!isMonitoring) {
                    startMonitoring()
                }
            }
        }
        
        return START_STICKY
    }

    private fun startMonitoring() {
        if (isMonitoring || !isTrackingEnabled) return
        
        isMonitoring = true
        sensorManager.startMonitoring()
        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            while (isMonitoring && isTrackingEnabled) {
                val now = LocalDateTime.now()
                val currentTime = now.toLocalTime()
                
                // Check if we should stop monitoring (early wake-up)
                if (currentTime.isBefore(LocalTime.of(12, 0)) && 
                    currentTime.isAfter(LocalTime.of(7, 0)) && 
                    currentPhase == SleepPhase.AWAKE) {
                    stopMonitoring()
                    break
                }
                
                // Check if we should start monitoring (sleep time)
                if (currentTime.isAfter(LocalTime.of(23, 0)) || 
                    currentTime.isBefore(LocalTime.of(12, 0))) {
                    monitorSleepCycle()
                }
                
                delay(MONITORING_INTERVAL)
            }
        }
        
        // Monitorear los sensores
        serviceScope.launch {
            sensorManager.movementDetected.collect { movement ->
                if (movement) {
                    movementCounter++
                    lastMovementTime = LocalDateTime.now()
                }
            }
        }
        
        serviceScope.launch {
            sensorManager.heartRate.collect { rate ->
                if (rate > 0) {
                    heartRateSum += rate
                    heartRateReadings++
                }
            }
        }
    }

    private suspend fun monitorSleepCycle() {
        val now = LocalDateTime.now()
        
        // Determinar fase del sueño basado en sensores
        val newPhase = determineSleepPhase(now)
        
        if (newPhase != currentPhase) {
            // Guardar la fase anterior
            saveSleepCycle(currentPhase, lastPhaseChange, now)
            
            // Actualizar fase actual
            currentPhase = newPhase
            lastPhaseChange = now
            
            // Resetear contadores
            movementCounter = 0
            heartRateSum = 0f
            heartRateReadings = 0
        }
    }

    private suspend fun saveSleepCycle(phase: SleepPhase, startTime: LocalDateTime, endTime: LocalDateTime) {
        val sleepCycle = SleepCycleEntity(
            phase = phase,
            startTime = startTime,
            endTime = endTime
        )
        database.sleepCycleDao().insert(sleepCycle)
    }

    private fun determineSleepPhase(now: LocalDateTime): SleepPhase {
        val minutesSinceLastMovement = java.time.Duration.between(lastMovementTime, now).toMinutes()
        val averageHeartRate = if (heartRateReadings > 0) heartRateSum / heartRateReadings else 0f
        
        return when {
            // Alta actividad y ritmo cardíaco elevado = Despierto
            minutesSinceLastMovement < 5 && movementCounter > 10 -> SleepPhase.AWAKE
            
            // Poco movimiento y ritmo cardíaco estable = Sueño ligero
            minutesSinceLastMovement < 20 && averageHeartRate > 50 -> SleepPhase.LIGHT_SLEEP
            
            // Muy poco movimiento y ritmo cardíaco bajo = Sueño profundo
            minutesSinceLastMovement < 40 && averageHeartRate < 50 -> SleepPhase.DEEP_SLEEP
            
            // Algo de movimiento y variación en ritmo cardíaco = REM
            else -> SleepPhase.REM
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        sensorManager.stopMonitoring()
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sleep Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors sleep cycles"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Sleep Monitoring Active")
        .setContentText("Monitoring your sleep cycles")
        .setSmallIcon(R.drawable.ic_moon)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WfitSleep::SleepMonitoringWakeLock"
        ).apply {
            acquire(10*60*1000L /*10 minutes*/)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        sensorManager.stopMonitoring()
    }

    companion object {
        private const val CHANNEL_ID = "sleep_monitoring_channel"
        private const val NOTIFICATION_ID = 1
        private const val MONITORING_INTERVAL = 5 * 60 * 1000L // 5 minutes
        
        const val ACTION_START_MONITORING = "com.example.wfit.ACTION_START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.wfit.ACTION_STOP_MONITORING"
        const val ACTION_SET_TRACKING_STATE = "com.example.wfit.ACTION_SET_TRACKING_STATE"
        const val EXTRA_TRACKING_STATE = "tracking_state"
    }
} 