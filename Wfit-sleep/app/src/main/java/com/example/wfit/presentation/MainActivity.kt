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
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import com.example.wfit.data.db.SleepDatabase
import com.example.wfit.presentation.components.DailySleepCarousel
import com.example.wfit.presentation.viewmodel.SleepViewModel
import com.example.wfit.service.SleepMonitoringService

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

    private val viewModel: SleepViewModel by viewModels {
        SleepViewModel.Factory(SleepDatabase.getDatabase(this), this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        
        setContent {
            WearApp {
                val sleepData by viewModel.sleepData.collectAsState()
                val isTrackingEnabled by viewModel.isTrackingEnabled.collectAsState()
                
                DailySleepCarousel(
                    sleepDataList = sleepData,
                    isTrackingEnabled = isTrackingEnabled,
                    onTrackingToggle = { viewModel.toggleTracking() },
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