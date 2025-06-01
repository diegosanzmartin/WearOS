/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.wfit.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import com.example.wfit.presentation.components.DailySleepCarousel
import com.example.wfit.presentation.model.DailySleepData
import com.example.wfit.presentation.model.SleepCycle
import com.example.wfit.presentation.model.SleepPhase
import com.example.wfit.service.SleepMonitoringService
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.BODY_SENSORS_BACKGROUND,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.BODY_SENSORS_BACKGROUND
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startSleepMonitoring()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        
        // TODO: Esto será reemplazado por datos reales de los sensores
        val mockSleepData = createMockSleepData()
        
        setContent {
            WearApp {
                DailySleepCarousel(
                    sleepDataList = mockSleepData,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            startSleepMonitoring()
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    private fun startSleepMonitoring() {
        val intent = Intent(this, SleepMonitoringService::class.java).apply {
            action = SleepMonitoringService.ACTION_START_MONITORING
        }
        startForegroundService(intent)
    }
    
    private fun createMockSleepData(): List<DailySleepData> {
        val now = LocalDateTime.now()
        return listOf(
            createDailySleepData(now.minusDays(2)),
            createDailySleepData(now.minusDays(1)),
            createDailySleepData(now)
        )
    }
    
    private fun createDailySleepData(date: LocalDateTime): DailySleepData {
        val startTime = date.withHour(23).withMinute(0)
        return DailySleepData(
            date = date,
            cycles = listOf(
                SleepCycle(
                    phase = SleepPhase.AWAKE,
                    startTime = startTime,
                    endTime = startTime.plusMinutes(20)
                ),
                SleepCycle(
                    phase = SleepPhase.LIGHT_SLEEP,
                    startTime = startTime.plusMinutes(20),
                    endTime = startTime.plusMinutes(90)
                ),
                SleepCycle(
                    phase = SleepPhase.DEEP_SLEEP,
                    startTime = startTime.plusMinutes(90),
                    endTime = startTime.plusMinutes(150)
                ),
                SleepCycle(
                    phase = SleepPhase.REM,
                    startTime = startTime.plusMinutes(150),
                    endTime = startTime.plusMinutes(210)
                ),
                // Repetir patrón...
                SleepCycle(
                    phase = SleepPhase.LIGHT_SLEEP,
                    startTime = startTime.plusMinutes(210),
                    endTime = startTime.plusMinutes(270)
                )
            )
        )
    }
}

@Composable
fun WearApp(content: @Composable () -> Unit) {
    MaterialTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            content()
        }
    }
}