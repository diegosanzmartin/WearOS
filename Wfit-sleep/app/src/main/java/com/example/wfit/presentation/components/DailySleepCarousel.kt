package com.example.wfit.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wfit.presentation.model.DailySleepData
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailySleepCarousel(
    sleepDataList: List<DailySleepData>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { sleepDataList.size })
    
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                
                // Información adicional que se puede scrollear
                Spacer(modifier = Modifier.height(16.dp))
                
                // Detalles de las fases
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Detalles del sueño",
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    // Tiempo en cada fase
                    PhaseTimeDetail("Sueño profundo", 
                        dailyData.cycles
                            .filter { it.phase == com.example.wfit.presentation.model.SleepPhase.DEEP_SLEEP }
                            .sumOf { it.durationMinutes }
                    )
                    PhaseTimeDetail("Sueño ligero", 
                        dailyData.cycles
                            .filter { it.phase == com.example.wfit.presentation.model.SleepPhase.LIGHT_SLEEP }
                            .sumOf { it.durationMinutes }
                    )
                    PhaseTimeDetail("REM", 
                        dailyData.cycles
                            .filter { it.phase == com.example.wfit.presentation.model.SleepPhase.REM }
                            .sumOf { it.durationMinutes }
                    )
                    PhaseTimeDetail("Despierto", 
                        dailyData.cycles
                            .filter { it.phase == com.example.wfit.presentation.model.SleepPhase.AWAKE }
                            .sumOf { it.durationMinutes }
                    )
                }
                
                // Espacio adicional al final para asegurar que todo es scrolleable
                Spacer(modifier = Modifier.height(32.dp))
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = phase,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = "${minutes}min",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
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