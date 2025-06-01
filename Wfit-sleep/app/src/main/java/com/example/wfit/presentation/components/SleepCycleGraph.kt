package com.example.wfit.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.wfit.presentation.model.DailySleepData
import com.example.wfit.presentation.model.SleepPhase
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun SleepCycleGraph(
    sleepData: DailySleepData,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(8.dp)
    ) {
        val width = size.width
        val height = size.height
        val path = Path()

        // Colores para cada fase del sueño
        val phaseColors = mapOf(
            SleepPhase.AWAKE to Color(0xFFE57373),      // Rojo claro
            SleepPhase.LIGHT_SLEEP to Color(0xFF81C784), // Verde claro
            SleepPhase.DEEP_SLEEP to Color(0xFF5C6BC0),  // Azul medio
            SleepPhase.REM to Color(0xFF9575CD)          // Morado
        )

        // Calcular el tiempo total en minutos
        val totalMinutes = ChronoUnit.MINUTES.between(
            sleepData.cycles.first().startTime,
            sleepData.cycles.last().endTime
        )

        var currentX = 0f
        sleepData.cycles.forEach { cycle ->
            val cycleWidth = (width * cycle.durationMinutes / totalMinutes)
            val y = when (cycle.phase) {
                SleepPhase.AWAKE -> height * 0.2f
                SleepPhase.LIGHT_SLEEP -> height * 0.4f
                SleepPhase.DEEP_SLEEP -> height * 0.8f
                SleepPhase.REM -> height * 0.6f
            }

            if (currentX == 0f) {
                path.moveTo(currentX, y)
            } else {
                path.lineTo(currentX, y)
            }

            currentX += cycleWidth
            path.lineTo(currentX, y)

            // Dibujar la línea con el color correspondiente a la fase
            drawLine(
                color = phaseColors[cycle.phase] ?: Color.Gray,
                start = Offset(currentX - cycleWidth, y),
                end = Offset(currentX, y),
                strokeWidth = 3f
            )
        }

        // Dibujar el path completo
        drawPath(
            path = path,
            color = Color.Gray,
            style = Stroke(width = 2f)
        )
    }
} 