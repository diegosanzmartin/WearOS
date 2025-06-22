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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import androidx.wear.tooling.preview.devices.WearDevices
import com.wfit.heart.R
import com.wfit.heart.presentation.theme.WfitHeartTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import com.wfit.heart.presentation.components.HeartRateGraph

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
    
    override fun onResume() {
        super.onResume()
        viewModel.setAppVisible(true)
    }
    
    override fun onPause() {
        super.onPause()
        viewModel.setAppVisible(false)
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
            HeartRateScreen()
        }
    }
}

@Composable
fun HeartRateScreen() {
    val viewModel: HeartRateViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.isMonitoring) {
            Chip(
                onClick = { /* No-op */ },
                label = { 
                    Text(
                        text = stringResource(id = R.string.monitoring_status),
                        fontSize = 7.sp
                    ) 
                },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF4CAF50)),
                modifier = Modifier.height(24.dp)
            )
        } else if (!uiState.sensorAvailable) {
            Chip(
                onClick = { /* No-op */ },
                label = { 
                    Text(
                        text = stringResource(id = R.string.sensor_not_available),
                        fontSize = 7.sp
                    ) 
                },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFFF44336)),
                modifier = Modifier.height(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    !uiState.sensorAvailable -> stringResource(id = R.string.sensor_na)
                    uiState.heartRate != null -> uiState.heartRate.toString()
                    else -> stringResource(id = R.string.no_heart_rate)
                },
                style = MaterialTheme.typography.display1,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_heart),
                    contentDescription = "Heart Icon",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Red
                )
                Text(
                    text = stringResource(id = R.string.heart_rate_unit),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Gráfica de ritmo cardíaco con promedios por hora
        HeartRateGraph(
            hourlyAverages = uiState.hourlyAverages,
            minValue = uiState.minValue,
            maxValue = uiState.maxValue,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { viewModel.toggleMonitoring() },
            enabled = uiState.sensorAvailable,
            modifier = Modifier.size(ButtonDefaults.LargeButtonSize)
        ) {
            Text(
                text = if (uiState.isMonitoring) 
                    stringResource(id = R.string.stop_monitoring)
                else 
                    stringResource(id = R.string.start_monitoring)
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}