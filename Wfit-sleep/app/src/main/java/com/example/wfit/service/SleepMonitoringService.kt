package com.wfit.sleep.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.wfit.sleep.R
import com.wfit.sleep.data.db.SleepDatabase
import com.wfit.sleep.data.db.entity.SleepCycleEntity
import com.wfit.sleep.presentation.model.SleepPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalTime

class SleepMonitoringService : LifecycleService() {
    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + job)
    private var monitoringJob: Job? = null
    
    private lateinit var database: SleepDatabase
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var sensorManager: SleepSensorManager
    
    private var isMonitoring = false
    private var currentPhase: SleepPhase = SleepPhase.AWAKE
    private var lastPhaseChange: LocalDateTime = LocalDateTime.now()
    private var isTrackingEnabled = true

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
        startForeground(NOTIFICATION_ID, createNotification())
        sensorManager.startMonitoring()
        
        monitoringJob = serviceScope.launch {
            while (isMonitoring && isTrackingEnabled) {
                val now = LocalDateTime.now()
                val currentTime = now.toLocalTime()
                
                if (shouldStopMonitoring(currentTime)) {
                    withContext(Dispatchers.Main) {
                        stopMonitoring()
                    }
                    break
                }
                
                if (shouldMonitorSleep(currentTime)) {
                    checkAndUpdateSleepPhase()
                }
                
                delay(MONITORING_INTERVAL)
            }
        }
    }

    private fun shouldStopMonitoring(currentTime: LocalTime): Boolean {
        return currentTime.isBefore(LocalTime.of(12, 0)) && 
               currentTime.isAfter(LocalTime.of(7, 0)) && 
               currentPhase == SleepPhase.AWAKE
    }

    private fun shouldMonitorSleep(currentTime: LocalTime): Boolean {
        return currentTime.isAfter(LocalTime.of(23, 0)) || 
               currentTime.isBefore(LocalTime.of(12, 0))
    }

    private suspend fun checkAndUpdateSleepPhase() {
        val now = LocalDateTime.now()
        val newPhase = determineSleepPhase()
        
        if (newPhase != currentPhase) {
            withContext(Dispatchers.IO) {
                saveSleepCycle(currentPhase, lastPhaseChange, now)
            }
            currentPhase = newPhase
            lastPhaseChange = now
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

    private fun determineSleepPhase(): SleepPhase {
        val minutesSinceLastMovement = sensorManager.getTimeSinceLastMovement().toMinutes()
        val currentHeartRate = sensorManager.heartRate.value.toInt()
        
        return when {
            minutesSinceLastMovement < 5 && currentHeartRate > 70 -> SleepPhase.AWAKE
            minutesSinceLastMovement < 20 && currentHeartRate in 50..70 -> SleepPhase.LIGHT_SLEEP
            minutesSinceLastMovement < 40 && currentHeartRate < 50 -> SleepPhase.DEEP_SLEEP
            else -> SleepPhase.REM
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
        sensorManager.stopMonitoring()
        
        serviceScope.launch(Dispatchers.Main) {
            safeStopForeground()
            stopSelf()
        }
    }

    private fun safeStopForeground() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }.onFailure {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
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
        monitoringJob?.cancel()
        job.cancel()
        serviceScope.cancel()
        sensorManager.stopMonitoring()
    }

    companion object {
        private const val CHANNEL_ID = "sleep_monitoring_channel"
        private const val NOTIFICATION_ID = 1
        private const val MONITORING_INTERVAL = 5 * 60 * 1000L // 5 minutes
        private const val STOP_FOREGROUND_DETACH = 1
        
        const val ACTION_START_MONITORING = "com.wfit.sleep.ACTION_START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.wfit.sleep.ACTION_STOP_MONITORING"
        const val ACTION_SET_TRACKING_STATE = "com.wfit.sleep.ACTION_SET_TRACKING_STATE"
        const val EXTRA_TRACKING_STATE = "tracking_state"
    }
} 