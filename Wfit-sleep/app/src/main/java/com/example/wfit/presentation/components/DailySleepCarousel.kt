package com.example.wfit.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val dailyData = sleepDataList[page]
            DailySleepCard(dailyData)
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
                .height(120.dp)
        )
        
        // Estadísticas
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