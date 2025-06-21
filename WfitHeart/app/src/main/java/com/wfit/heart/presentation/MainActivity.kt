/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.wfit.heart.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.*
import androidx.wear.tooling.preview.devices.WearDevices
import com.wfit.heart.R
import com.wfit.heart.presentation.theme.WfitHeartTheme
import androidx.compose.ui.res.stringResource

class MainActivity : ComponentActivity() {
    
    private val viewModel: HeartRateViewModel by viewModels()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startMonitoring()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp()
        }
        
        checkAndRequestPermissions()
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            viewModel.startMonitoring()
        }
    }
}

@Composable
fun WearApp() {
    WfitHeartTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            HeartRateScreen(viewModel)
        }
    }
}

@Composable
fun HeartRateScreen(viewModel: HeartRateViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título
        Text(
            text = stringResource(R.string.heart_rate_title),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Indicador de ritmo cardíaco
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !uiState.sensorAvailable -> Color(0xFFF44336) // Rojo si no hay sensor
                        uiState.isMonitoring -> Color(0xFF4CAF50) // Verde si está monitoreando
                        else -> Color(0xFF9E9E9E) // Gris si está detenido
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    !uiState.sensorAvailable -> stringResource(R.string.sensor_na)
                    uiState.heartRate != null -> uiState.heartRate.toString()
                    else -> stringResource(R.string.no_heart_rate)
                },
                style = MaterialTheme.typography.h3.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Unidad
        Text(
            text = stringResource(R.string.heart_rate_unit),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Estado del sensor
        if (!uiState.sensorAvailable) {
            Text(
                text = stringResource(R.string.sensor_not_available),
                style = MaterialTheme.typography.caption,
                color = Color(0xFFF44336),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Botón de control
        Button(
            onClick = { viewModel.toggleMonitoring() },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.sensorAvailable
        ) {
            Text(
                text = if (uiState.isMonitoring) stringResource(R.string.stop_monitoring) else stringResource(R.string.start_monitoring),
                style = MaterialTheme.typography.button
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Estado del monitoreo
        Text(
            text = when {
                !uiState.sensorAvailable -> stringResource(R.string.sensor_not_available)
                uiState.isMonitoring -> stringResource(R.string.monitoring_status)
                else -> stringResource(R.string.stopped_status)
            },
            style = MaterialTheme.typography.caption,
            color = when {
                !uiState.sensorAvailable -> Color(0xFFF44336)
                uiState.isMonitoring -> Color(0xFF4CAF50)
                else -> Color(0xFF9E9E9E)
            }
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}