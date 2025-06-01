package com.example.wfit.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.wear.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wfit.R
import com.example.wfit.presentation.model.DailySleepData
import com.example.wfit.presentation.model.SleepPhase
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailySleepCarousel(
    sleepDataList: List<DailySleepData>,
    isTrackingEnabled: Boolean,
    onTrackingToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsMenu(
            isTrackingEnabled = isTrackingEnabled,
            onTrackingToggle = onTrackingToggle,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (sleepDataList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmptySleepData()
                }
                
                // Settings button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.DarkGray, shape = MaterialTheme.shapes.small)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showSettings = true }
                        )
                    }
                }
            }
            return
        }

        val pagerState = rememberPagerState(pageCount = { sleepDataList.size })
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val dailyData = sleepDataList[page]
            val scrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DailySleepCard(dailyData)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Sleep stage",
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val totalTime = dailyData.totalSleepTime
                    
                    // Deep Sleep
                    val deepSleepTime = dailyData.cycles
                        .filter { it.phase == SleepPhase.DEEP_SLEEP }
                        .sumOf { it.durationMinutes }
                    PhaseTimeDetail(
                        phase = "Profundo",
                        minutes = deepSleepTime,
                        totalMinutes = totalTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Light Sleep
                    val lightSleepTime = dailyData.cycles
                        .filter { it.phase == SleepPhase.LIGHT_SLEEP }
                        .sumOf { it.durationMinutes }
                    PhaseTimeDetail(
                        phase = "Ligero",
                        minutes = lightSleepTime,
                        totalMinutes = totalTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // REM
                    val remTime = dailyData.cycles
                        .filter { it.phase == SleepPhase.REM }
                        .sumOf { it.durationMinutes }
                    PhaseTimeDetail(
                        phase = "REM",
                        minutes = remTime,
                        totalMinutes = totalTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Awake
                    val awakeTime = dailyData.cycles
                        .filter { it.phase == SleepPhase.AWAKE }
                        .sumOf { it.durationMinutes }
                    PhaseTimeDetail(
                        phase = "Despierto",
                        minutes = awakeTime,
                        totalMinutes = totalTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Settings button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.DarkGray, shape = MaterialTheme.shapes.small)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { showSettings = true }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DailySleepCard(
    dailyData: DailySleepData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fecha
        Text(
            text = dailyData.date.format(DateTimeFormatter.ofPattern("dd MMM")),
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Gráfica
        SleepCycleGraph(
            sleepData = dailyData,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )
        
        // Estadísticas principales
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatisticItem("Total", "${dailyData.totalSleepTime}min")
            StatisticItem("Score", "${dailyData.sleepScore}%")
        }
    }
}

@Composable
private fun PhaseTimeDetail(
    phase: String,
    minutes: Int,
    totalMinutes: Int,
    modifier: Modifier = Modifier
) {
    val phaseColors = mapOf(
        "Profundo" to Color(0xFF3F51B5), // Azul oscuro
        "Ligero" to Color(0xFF2196F3),   // Azul claro
        "REM" to Color(0xFF4CAF50),            // Verde
        "Despierto" to Color(0xFFFF9800)       // Naranja
    )

    val percentage = if (totalMinutes > 0) {
        (minutes.toFloat() / totalMinutes.toFloat() * 100).toInt()
    } else {
        0
    }
    
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    val timeText = if (hours > 0) {
        "${hours}h ${remainingMinutes}m"
    } else {
        "${remainingMinutes}m"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = phase,
                    fontSize = 14.sp,
                    color = phaseColors[phase] ?: Color.White
                )
                Text(
                    text = " ${percentage}%",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            Text(
                text = timeText,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFF424242))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .height(8.dp)
                    .background(phaseColors[phase] ?: Color.White)
            )
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.White
        )
    }
} 