package com.wfit.heart.data

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.wfit.heart.R
import com.wfit.heart.presentation.MainActivity
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class HeartRateBackgroundService : Service() {
    private lateinit var heartRateService: HeartRateService
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var measurementBuffer = mutableListOf<Int>()
    private var lastSaveTime = LocalDateTime.now()
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "heart_rate_channel"
        private const val BUFFER_TIME_MINUTES = 5 // Tiempo para acumular mediciones antes de guardar
        private const val MIN_MEASUREMENTS_TO_SAVE = 3 // Mínimo de mediciones para calcular la media
    }

    override fun onCreate() {
        super.onCreate()
        heartRateService = HeartRateService(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        heartRateService.startMonitoring()
        
        serviceScope.launch {
            heartRateService.heartRate.collect { heartRate ->
                heartRate?.let { processHeartRate(it) }
            }
        }
    }

    private fun processHeartRate(value: Int) {
        measurementBuffer.add(value)
        val now = LocalDateTime.now()
        
        // Guardar si han pasado X minutos o tenemos suficientes mediciones
        if (ChronoUnit.MINUTES.between(lastSaveTime, now) >= BUFFER_TIME_MINUTES || 
            measurementBuffer.size >= MIN_MEASUREMENTS_TO_SAVE) {
            saveAggregatedMeasurement()
        }
    }

    private fun saveAggregatedMeasurement() {
        if (measurementBuffer.size >= MIN_MEASUREMENTS_TO_SAVE) {
            val average = measurementBuffer.average().toInt()
            heartRateService.history.addMeasurement(average)
            lastSaveTime = LocalDateTime.now()
            measurementBuffer.clear()
            
            // Actualizar notificación
            updateNotification(average)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Heart Rate Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoring heart rate in background"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitorizando ritmo cardíaco")
            .setContentText("Servicio activo")
            .setSmallIcon(R.drawable.ic_heart)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(heartRate: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitorizando ritmo cardíaco")
            .setContentText("Último valor: $heartRate bpm")
            .setSmallIcon(R.drawable.ic_heart)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        heartRateService.stopMonitoring()
        saveAggregatedMeasurement() // Guardar últimas mediciones pendientes
    }
} 