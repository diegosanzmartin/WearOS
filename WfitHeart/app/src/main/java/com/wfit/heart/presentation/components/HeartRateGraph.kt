package com.wfit.heart.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.wear.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wfit.heart.data.HeartRateMeasurement
import java.time.LocalTime

@Composable
fun HeartRateGraph(
    measurements: List<HeartRateMeasurement>,
    minValue: Int,
    maxValue: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Líneas horizontales y valores
            val gridLines = 4
            val step = height / gridLines
            for (i in 0..gridLines) {
                val y = i * step
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }
            
            // Líneas verticales (horas)
            val hoursStep = width / 24
            for (i in 0..24) {
                val x = i * hoursStep
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
            }
            
            // Dibujar la línea de mediciones
            if (measurements.isNotEmpty()) {
                val points = measurements.map { measurement ->
                    val hourOfDay = measurement.time.hour + (measurement.time.minute / 60f)
                    val x = (hourOfDay / 24f) * width
                    val normalizedValue = (measurement.value - minValue).toFloat() / (maxValue - minValue)
                    val y = height - (normalizedValue * height)
                    Offset(x, y)
                }.sortedBy { it.x }
                
                // Dibujar líneas entre puntos
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color.White,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
                
                // Dibujar puntos
                points.forEach { point ->
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = point
                    )
                }
            }
        }
        
        // Valores min/max
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(start = 4.dp)
        ) {
            Text(
                text = maxValue.toString(),
                color = Color.Red,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = minValue.toString(),
                color = Color.Blue,
                fontSize = 10.sp
            )
        }
        
        // Horas
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0",
                color = Color.Gray,
                fontSize = 10.sp
            )
            Text(
                text = "12",
                color = Color.Gray,
                fontSize = 10.sp
            )
            Text(
                text = "24",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
} 